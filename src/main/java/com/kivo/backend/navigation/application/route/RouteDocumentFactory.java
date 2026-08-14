package com.kivo.backend.navigation.application.route;

import com.kivo.backend.catalog.application.port.RouteFloorReadModel;
import com.kivo.backend.maps.application.OverlayMarker;
import com.kivo.backend.maps.application.OverlayMarkerType;
import com.kivo.backend.maps.application.OverlayPoint;
import com.kivo.backend.maps.application.OverlayPolyline;
import com.kivo.backend.navigation.application.PlannedRoute;
import com.kivo.backend.navigation.application.exception.NavigationGraphUnavailableException;
import com.kivo.backend.navigation.configuration.RouteDeliveryProperties;
import com.kivo.backend.navigation.domain.model.GraphNode;
import com.kivo.backend.navigation.domain.model.GraphPoint;
import com.kivo.backend.navigation.domain.routing.RouteInstruction;
import com.kivo.backend.navigation.domain.routing.RouteResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class RouteDocumentFactory {

  private final RouteDeliveryProperties properties;

  public RouteDocumentFactory(RouteDeliveryProperties properties) {
    this.properties = properties;
  }

  public RouteDocument create(
      RouteCreateCommand command,
      PlannedRoute plannedRoute,
      List<RouteFloorReadModel> floors,
      Instant createdAt) {
    RouteResult route = plannedRoute.route();
    if (!route.mallId().equals(plannedRoute.snapshot().mallId())
        || !route.graphVersionId().equals(plannedRoute.snapshot().versionId())) {
      throw new NavigationGraphUnavailableException();
    }

    Map<UUID, RouteFloorReadModel> floorById =
        floors.stream().collect(Collectors.toMap(RouteFloorReadModel::id, Function.identity()));
    UUID routeId = UUID.randomUUID();
    Map<UUID, FloorAccumulator> accumulators = new LinkedHashMap<>();
    GraphNode start = requiredNode(plannedRoute, route.startNodeCode());
    GraphNode destination = requiredNode(plannedRoute, route.destinationNodeCode());

    if (route.steps().isEmpty()
        && start.floorId().equals(destination.floorId())
        && start.point().equals(destination.point())) {
      accumulator(accumulators, floorById, start.floorId())
          .markers
          .add(new OverlayMarker(toPoint(start.point()), OverlayMarkerType.START_FINISH));
    } else {
      accumulator(accumulators, floorById, start.floorId())
          .markers
          .add(new OverlayMarker(toPoint(start.point()), OverlayMarkerType.START));
      accumulator(accumulators, floorById, destination.floorId())
          .markers
          .add(new OverlayMarker(toPoint(destination.point()), OverlayMarkerType.FINISH));
    }

    route
        .steps()
        .forEach(
            step -> {
              if (step.fromFloorId().equals(step.toFloorId())) {
                if (step.geometry().size() >= 2) {
                  accumulator(accumulators, floorById, step.fromFloorId())
                      .polylines
                      .add(
                          new OverlayPolyline(
                              step.geometry().stream()
                                  .map(RouteDocumentFactory::toPoint)
                                  .toList()));
                }
              } else {
                GraphNode from = requiredNode(plannedRoute, step.fromNodeCode());
                GraphNode to = requiredNode(plannedRoute, step.toNodeCode());
                accumulator(accumulators, floorById, step.fromFloorId())
                    .markers
                    .add(new OverlayMarker(toPoint(from.point()), OverlayMarkerType.TRANSITION));
                accumulator(accumulators, floorById, step.toFloorId())
                    .markers
                    .add(new OverlayMarker(toPoint(to.point()), OverlayMarkerType.TRANSITION));
              }
            });

    List<RouteOverlayDocument> overlays =
        accumulators.values().stream()
            .sorted(
                Comparator.comparingInt((FloorAccumulator value) -> value.floor.displayOrder())
                    .thenComparing(value -> value.floor.code()))
            .map(value -> toOverlay(routeId, value))
            .toList();

    return new RouteDocument(
        routeId,
        route.mallId(),
        route.graphVersionId(),
        command.kioskCode(),
        command.placeId(),
        route.profile(),
        scale(route.totalDistanceMeters()),
        toSeconds(route),
        createdAt,
        createdAt.plus(properties.getTtl()),
        toInstructions(route.instructions(), floorById),
        overlays);
  }

  private static List<RouteInstructionDocument> toInstructions(
      List<RouteInstruction> instructions, Map<UUID, RouteFloorReadModel> floorById) {
    List<RouteInstructionDocument> result = new ArrayList<>();
    for (int sequence = 0; sequence < instructions.size(); sequence++) {
      RouteInstruction instruction = instructions.get(sequence);
      result.add(
          switch (instruction) {
            case RouteInstruction.Start start ->
                new RouteInstructionDocument(
                    sequence,
                    start.type(),
                    floorCode(floorById, start.floorId()),
                    null,
                    null,
                    null);
            case RouteInstruction.Continue next ->
                new RouteInstructionDocument(
                    sequence,
                    next.type(),
                    floorCode(floorById, next.floorId()),
                    null,
                    next.movementType(),
                    scale(next.distanceMeters()));
            case RouteInstruction.Transition transition ->
                new RouteInstructionDocument(
                    sequence,
                    transition.type(),
                    floorCode(floorById, transition.fromFloorId()),
                    floorCode(floorById, transition.toFloorId()),
                    transition.movementType(),
                    null);
            case RouteInstruction.Arrive arrive ->
                new RouteInstructionDocument(
                    sequence,
                    arrive.type(),
                    floorCode(floorById, arrive.floorId()),
                    null,
                    null,
                    null);
          });
    }
    return List.copyOf(result);
  }

  private RouteOverlayDocument toOverlay(UUID routeId, FloorAccumulator value) {
    int[] dimensions = pixelDimensions(value.floor);
    String imageUrl =
        UriComponentsBuilder.fromPath("/api/v1/routes/{routeId}/floors/{floorCode}/overlay.png")
            .buildAndExpand(routeId, value.floor.code())
            .encode()
            .toUriString();
    return new RouteOverlayDocument(
        value.floor.id(),
        value.floor.code(),
        value.floor.displayOrder(),
        value.floor.mapImageUrl(),
        imageUrl,
        value.floor.coordinateWidth(),
        value.floor.coordinateHeight(),
        dimensions[0],
        dimensions[1],
        value.polylines,
        value.markers);
  }

  private int[] pixelDimensions(RouteFloorReadModel floor) {
    if (floor.coordinateWidth() <= 0 || floor.coordinateHeight() <= 0) {
      throw new NavigationGraphUnavailableException();
    }
    int maximum = properties.getOverlayMaxDimension();
    if (floor.coordinateWidth() >= floor.coordinateHeight()) {
      return new int[] {
        maximum,
        Math.max(
            1,
            (int) Math.round(maximum * floor.coordinateHeight() / (double) floor.coordinateWidth()))
      };
    }
    return new int[] {
      Math.max(
          1,
          (int) Math.round(maximum * floor.coordinateWidth() / (double) floor.coordinateHeight())),
      maximum
    };
  }

  private static FloorAccumulator accumulator(
      Map<UUID, FloorAccumulator> accumulators,
      Map<UUID, RouteFloorReadModel> floorById,
      UUID floorId) {
    return accumulators.computeIfAbsent(
        floorId, ignored -> new FloorAccumulator(requiredFloor(floorById, floorId)));
  }

  private static RouteFloorReadModel requiredFloor(
      Map<UUID, RouteFloorReadModel> floorById, UUID floorId) {
    RouteFloorReadModel floor = floorById.get(floorId);
    if (floor == null) {
      throw new NavigationGraphUnavailableException();
    }
    return floor;
  }

  private static String floorCode(Map<UUID, RouteFloorReadModel> floorById, UUID floorId) {
    return requiredFloor(floorById, floorId).code();
  }

  private static GraphNode requiredNode(PlannedRoute plannedRoute, String nodeCode) {
    GraphNode node = plannedRoute.snapshot().nodesByCode().get(nodeCode);
    if (node == null) {
      throw new NavigationGraphUnavailableException();
    }
    return node;
  }

  private static OverlayPoint toPoint(GraphPoint point) {
    return new OverlayPoint(point.x(), point.y());
  }

  private static BigDecimal scale(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_UP);
  }

  private static long toSeconds(RouteResult route) {
    long milliseconds = route.estimatedDuration().toMillis();
    return Math.ceilDiv(milliseconds, 1000L);
  }

  private static final class FloorAccumulator {

    private final RouteFloorReadModel floor;
    private final List<OverlayPolyline> polylines = new ArrayList<>();
    private final List<OverlayMarker> markers = new ArrayList<>();

    private FloorAccumulator(RouteFloorReadModel floor) {
      this.floor = floor;
    }
  }
}
