package com.kivo.backend.navigation.application;

import com.kivo.backend.navigation.domain.model.GraphSnapshot;
import com.kivo.backend.navigation.domain.routing.RouteResult;
import java.util.Objects;

public record PlannedRoute(RouteResult route, GraphSnapshot snapshot) {

  public PlannedRoute {
    Objects.requireNonNull(route, "route");
    Objects.requireNonNull(snapshot, "snapshot");
  }
}
