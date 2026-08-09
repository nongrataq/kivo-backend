package com.kivo.backend.navigation.application;

import com.kivo.backend.navigation.domain.routing.MovementProfile;
import com.kivo.backend.navigation.domain.routing.RouteResult;
import java.util.UUID;

public interface RoutePlanner {

  RouteResult plan(
      UUID mallId, String startNodeCode, String destinationNodeCode, MovementProfile profile);
}
