package com.kivo.backend.navigation.application;

import com.kivo.backend.navigation.domain.routing.MovementProfile;
import com.kivo.backend.navigation.domain.routing.RouteRequest;
import com.kivo.backend.navigation.domain.routing.RouteResult;
import com.kivo.backend.navigation.domain.routing.RoutingCostPolicy;
import com.kivo.backend.navigation.domain.routing.ShortestPathAlgorithm;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultRoutePlanner implements RoutePlanner {

  private final ActiveGraphProvider activeGraphProvider;
  private final ShortestPathAlgorithm shortestPathAlgorithm;
  private final RoutingCostPolicy routingCostPolicy;

  @Override
  public PlannedRoute planWithSnapshot(
      UUID mallId, String startNodeCode, String destinationNodeCode, MovementProfile profile) {
    var snapshot = activeGraphProvider.getRequired(mallId);
    var request = new RouteRequest(startNodeCode, destinationNodeCode, profile);
    RouteResult route =
        shortestPathAlgorithm.findRoute(snapshot, request, routingCostPolicy).route();
    return new PlannedRoute(route, snapshot);
  }
}
