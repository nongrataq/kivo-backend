package com.kivo.backend.navigation.application.route;

import static org.assertj.core.api.Assertions.assertThat;

import com.kivo.backend.catalog.application.port.RouteFloorReadModel;
import com.kivo.backend.maps.application.OverlayMarker;
import com.kivo.backend.maps.application.OverlayMarkerType;
import com.kivo.backend.navigation.application.PlannedRoute;
import com.kivo.backend.navigation.configuration.RouteDeliveryProperties;
import com.kivo.backend.navigation.domain.model.GraphEdge;
import com.kivo.backend.navigation.domain.model.GraphNode;
import com.kivo.backend.navigation.domain.model.GraphPoint;
import com.kivo.backend.navigation.domain.model.GraphSnapshot;
import com.kivo.backend.navigation.domain.model.MovementType;
import com.kivo.backend.navigation.domain.model.NodeType;
import com.kivo.backend.navigation.domain.routing.DijkstraShortestPath;
import com.kivo.backend.navigation.domain.routing.MovementProfile;
import com.kivo.backend.navigation.domain.routing.RouteRequest;
import com.kivo.backend.navigation.domain.routing.RoutingCostPolicy;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RouteDocumentFactoryTest {

  private static final UUID MALL_ID = UUID.fromString("71000000-0000-0000-0000-000000000001");
  private static final UUID VERSION_ID = UUID.fromString("71000000-0000-0000-0000-000000000002");
  private static final UUID FLOOR_ID = UUID.fromString("71000000-0000-0000-0000-000000000003");
  private static final UUID PLACE_ID = UUID.fromString("71000000-0000-0000-0000-000000000004");
  private static final UUID FLOOR_TWO_ID = UUID.fromString("71000000-0000-0000-0000-000000000005");

  private final RouteDocumentFactory factory = new RouteDocumentFactory(properties());

  @Test
  void buildsFlatInstructionsAndOneScaledOverlayPerVisitedFloor() {
    GraphSnapshot snapshot = snapshot(false);
    var result =
        new DijkstraShortestPath()
            .findRoute(
                snapshot,
                new RouteRequest("start", "target", MovementProfile.STANDARD),
                RoutingCostPolicy.defaults())
            .route();

    RouteDocument route =
        factory.create(
            new RouteCreateCommand("kiosk", PLACE_ID, MovementProfile.STANDARD),
            new PlannedRoute(result, snapshot),
            List.of(new RouteFloorReadModel(FLOOR_ID, "L 1", 10, 10000, 5000)),
            Instant.parse("2026-08-10T10:00:00Z"));

    assertThat(route.totalDistanceMeters()).isEqualByComparingTo("10.00");
    assertThat(route.estimatedDurationSeconds()).isEqualTo(8);
    assertThat(route.instructions()).hasSize(3);
    assertThat(route.overlays())
        .singleElement()
        .satisfies(
            overlay -> {
              assertThat(overlay.floorCode()).isEqualTo("L 1");
              assertThat(overlay.imageUrl()).contains("L%201/overlay.png");
              assertThat(overlay.pixelWidth()).isEqualTo(2048);
              assertThat(overlay.pixelHeight()).isEqualTo(1024);
              assertThat(overlay.polylines()).hasSize(1);
              assertThat(overlay.markers())
                  .extracting(OverlayMarker::type)
                  .containsExactly(OverlayMarkerType.START, OverlayMarkerType.FINISH);
            });
  }

  @Test
  void zeroRouteUsesCombinedMarker() {
    GraphSnapshot snapshot = snapshot(true);
    var result =
        new DijkstraShortestPath()
            .findRoute(
                snapshot,
                new RouteRequest("start", "start", MovementProfile.ACCESSIBLE),
                RoutingCostPolicy.defaults())
            .route();

    RouteDocument route =
        factory.create(
            new RouteCreateCommand("kiosk", PLACE_ID, MovementProfile.ACCESSIBLE),
            new PlannedRoute(result, snapshot),
            List.of(new RouteFloorReadModel(FLOOR_ID, "L1", 10, 10000, 10000)),
            Instant.EPOCH);

    assertThat(route.overlays().getFirst().markers())
        .singleElement()
        .extracting(OverlayMarker::type)
        .isEqualTo(OverlayMarkerType.START_FINISH);
  }

  @Test
  void createsOneOverlayPerFloorForTransition() {
    GraphNode start =
        new GraphNode("start", FLOOR_ID, MALL_ID, 10000, 10000, 100, 100, NodeType.KIOSK);
    GraphNode target =
        new GraphNode(
            "target", FLOOR_TWO_ID, MALL_ID, 10000, 10000, 200, 200, NodeType.PLACE_ACCESS);
    GraphEdge elevator =
        new GraphEdge(
            "elevator",
            "start",
            "target",
            BigDecimal.ONE,
            MovementType.ELEVATOR,
            false,
            true,
            List.of());
    GraphSnapshot snapshot =
        new GraphSnapshot(
            MALL_ID,
            VERSION_ID,
            "floors",
            List.of(start, target),
            List.of(elevator),
            List.of(),
            List.of());
    var result =
        new DijkstraShortestPath()
            .findRoute(
                snapshot,
                new RouteRequest("start", "target", MovementProfile.ACCESSIBLE),
                RoutingCostPolicy.defaults())
            .route();

    RouteDocument route =
        factory.create(
            new RouteCreateCommand("kiosk", PLACE_ID, MovementProfile.ACCESSIBLE),
            new PlannedRoute(result, snapshot),
            List.of(
                new RouteFloorReadModel(FLOOR_ID, "L1", 10, 10000, 10000),
                new RouteFloorReadModel(FLOOR_TWO_ID, "L2", 20, 10000, 10000)),
            Instant.EPOCH);

    assertThat(route.overlays())
        .extracting(RouteOverlayDocument::floorCode)
        .containsExactly("L1", "L2");
    assertThat(route.overlays())
        .allSatisfy(
            overlay ->
                assertThat(overlay.markers())
                    .extracting(OverlayMarker::type)
                    .contains(OverlayMarkerType.TRANSITION));
    assertThat(route.instructions())
        .anySatisfy(
            instruction -> {
              assertThat(instruction.floorCode()).isEqualTo("L1");
              assertThat(instruction.targetFloorCode()).isEqualTo("L2");
            });
  }

  private static RouteDeliveryProperties properties() {
    return new RouteDeliveryProperties();
  }

  private static GraphSnapshot snapshot(boolean zeroRoute) {
    GraphNode start =
        new GraphNode("start", FLOOR_ID, MALL_ID, 10000, 5000, 0, 2500, NodeType.KIOSK);
    if (zeroRoute) {
      return new GraphSnapshot(
          MALL_ID, VERSION_ID, "test", List.of(start), List.of(), List.of(), List.of());
    }
    GraphNode target =
        new GraphNode("target", FLOOR_ID, MALL_ID, 10000, 5000, 10000, 2500, NodeType.PLACE_ACCESS);
    GraphEdge edge =
        new GraphEdge(
            "edge",
            "start",
            "target",
            BigDecimal.TEN,
            MovementType.CORRIDOR,
            false,
            true,
            List.of(new GraphPoint(0, 2500), new GraphPoint(10000, 2500)));
    return new GraphSnapshot(
        MALL_ID, VERSION_ID, "test", List.of(start, target), List.of(edge), List.of(), List.of());
  }
}
