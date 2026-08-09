package com.kivo.backend.navigation.domain.routing;

import com.kivo.backend.navigation.domain.model.GraphEdge;
import com.kivo.backend.navigation.domain.model.GraphNode;
import com.kivo.backend.navigation.domain.model.GraphSnapshot;

public final class AStarShortestPath extends BestFirstShortestPath {

  @Override
  protected Heuristic createHeuristic(
      GraphSnapshot snapshot,
      GraphNode destination,
      MovementProfile profile,
      RoutingCostPolicy costPolicy) {
    double metersPerCoordinateUnit = minimumMetersPerCoordinateUnit(snapshot);
    if (!Double.isFinite(metersPerCoordinateUnit)) {
      return node -> 0.0;
    }
    return node -> {
      if (!node.floorId().equals(destination.floorId())) {
        return 0.0;
      }
      double coordinateDistance =
          Math.hypot(node.x() - destination.x(), node.y() - destination.y());
      return coordinateDistance * metersPerCoordinateUnit / costPolicy.speed(profile);
    };
  }

  private static double minimumMetersPerCoordinateUnit(GraphSnapshot snapshot) {
    double minimum = Double.POSITIVE_INFINITY;
    for (GraphEdge edge : snapshot.edges()) {
      GraphNode from = snapshot.nodesByCode().get(edge.fromNodeCode());
      GraphNode to = snapshot.nodesByCode().get(edge.toNodeCode());
      if (from == null || to == null || !from.floorId().equals(to.floorId())) {
        continue;
      }
      double coordinateDistance = Math.hypot(from.x() - to.x(), from.y() - to.y());
      if (coordinateDistance <= 0 || edge.lengthMeters() == null) {
        continue;
      }
      double ratio = edge.lengthMeters().doubleValue() / coordinateDistance;
      if (Double.isFinite(ratio) && ratio > 0) {
        minimum = Math.min(minimum, ratio);
      }
    }
    return minimum;
  }
}
