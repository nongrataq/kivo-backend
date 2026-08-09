package com.kivo.backend.navigation.domain.routing;

import java.util.Objects;

public record RouteRequest(
    String startNodeCode, String destinationNodeCode, MovementProfile profile) {

  public RouteRequest {
    Objects.requireNonNull(startNodeCode, "startNodeCode");
    Objects.requireNonNull(destinationNodeCode, "destinationNodeCode");
    Objects.requireNonNull(profile, "profile");
  }
}
