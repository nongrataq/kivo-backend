package com.kivo.backend.navigation.domain.routing;

import com.kivo.backend.navigation.domain.model.GraphSnapshot;

public interface ShortestPathAlgorithm {

  RouteComputation findRoute(
      GraphSnapshot snapshot, RouteRequest request, RoutingCostPolicy costPolicy);
}
