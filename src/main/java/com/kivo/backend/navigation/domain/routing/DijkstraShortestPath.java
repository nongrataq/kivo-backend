package com.kivo.backend.navigation.domain.routing;

import com.kivo.backend.navigation.domain.model.GraphNode;
import com.kivo.backend.navigation.domain.model.GraphSnapshot;

public final class DijkstraShortestPath extends BestFirstShortestPath {

  @Override
  protected Heuristic createHeuristic(
      GraphSnapshot snapshot,
      GraphNode destination,
      MovementProfile profile,
      RoutingCostPolicy costPolicy) {
    return node -> 0.0;
  }
}
