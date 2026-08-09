package com.kivo.backend.navigation.domain.routing;

import com.kivo.backend.navigation.domain.model.GraphEdge;
import com.kivo.backend.navigation.domain.model.GraphNode;
import com.kivo.backend.navigation.domain.model.GraphSnapshot;
import com.kivo.backend.navigation.domain.model.MovementType;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

final class RouteAssembler {

  private RouteAssembler() {}

  static RouteResult assemble(
      GraphSnapshot snapshot,
      RouteRequest request,
      List<GraphEdge> path,
      RoutingCostPolicy costPolicy) {
    List<RouteStep> steps =
        path.stream().map(edge -> toStep(snapshot, edge, request.profile(), costPolicy)).toList();
    BigDecimal distance =
        path.stream().map(GraphEdge::lengthMeters).reduce(BigDecimal.ZERO, BigDecimal::add);
    double seconds =
        path.stream().mapToDouble(edge -> costPolicy.travelSeconds(edge, request.profile())).sum();
    return new RouteResult(
        snapshot.mallId(),
        snapshot.versionId(),
        request.profile(),
        request.startNodeCode(),
        request.destinationNodeCode(),
        distance,
        duration(seconds),
        steps,
        instructions(snapshot, request, steps));
  }

  private static RouteStep toStep(
      GraphSnapshot snapshot,
      GraphEdge edge,
      MovementProfile profile,
      RoutingCostPolicy costPolicy) {
    GraphNode from = snapshot.nodesByCode().get(edge.fromNodeCode());
    GraphNode to = snapshot.nodesByCode().get(edge.toNodeCode());
    return new RouteStep(
        edge.code(),
        edge.fromNodeCode(),
        edge.toNodeCode(),
        from.floorId(),
        to.floorId(),
        edge.movementType(),
        edge.lengthMeters(),
        duration(costPolicy.travelSeconds(edge, profile)),
        edge.geometry());
  }

  private static List<RouteInstruction> instructions(
      GraphSnapshot snapshot, RouteRequest request, List<RouteStep> steps) {
    GraphNode start = snapshot.nodesByCode().get(request.startNodeCode());
    GraphNode destination = snapshot.nodesByCode().get(request.destinationNodeCode());
    List<RouteInstruction> instructions = new ArrayList<>();
    instructions.add(new RouteInstruction.Start(start.floorId(), start.code()));
    int index = 0;
    while (index < steps.size()) {
      RouteStep step = steps.get(index);
      if (!step.fromFloorId().equals(step.toFloorId())) {
        instructions.add(
            new RouteInstruction.Transition(
                step.fromFloorId(),
                step.toFloorId(),
                step.fromNodeCode(),
                step.toNodeCode(),
                step.movementType()));
        index++;
        continue;
      }
      int last = index;
      BigDecimal distance = step.distanceMeters();
      if (step.movementType() == MovementType.CORRIDOR) {
        while (last + 1 < steps.size()) {
          RouteStep next = steps.get(last + 1);
          if (next.movementType() != MovementType.CORRIDOR
              || !next.fromFloorId().equals(step.fromFloorId())
              || !next.toFloorId().equals(step.fromFloorId())) {
            break;
          }
          distance = distance.add(next.distanceMeters());
          last++;
        }
      }
      instructions.add(
          new RouteInstruction.Continue(
              step.fromFloorId(),
              step.fromNodeCode(),
              steps.get(last).toNodeCode(),
              step.movementType(),
              distance));
      index = last + 1;
    }
    instructions.add(new RouteInstruction.Arrive(destination.floorId(), destination.code()));
    return List.copyOf(instructions);
  }

  private static Duration duration(double seconds) {
    return Duration.ofMillis(Math.round(seconds * 1000.0));
  }
}
