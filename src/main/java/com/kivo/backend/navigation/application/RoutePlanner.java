package com.kivo.backend.navigation.application;

import com.kivo.backend.navigation.domain.routing.MovementProfile;
import com.kivo.backend.navigation.domain.routing.RouteResult;
import java.util.UUID;

public interface RoutePlanner {

  PlannedRoute planWithSnapshot(
      UUID mallId, String startNodeCode, String destinationNodeCode, MovementProfile profile);

  default RouteResult plan(
      UUID mallId, String startNodeCode, String destinationNodeCode, MovementProfile profile) {
    return planWithSnapshot(mallId, startNodeCode, destinationNodeCode, profile).route();
  }
}
