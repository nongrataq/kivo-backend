package com.kivo.backend.navigation.domain.routing;

public final class RouteNotFoundException extends RuntimeException {

  public RouteNotFoundException(RouteRequest request) {
    super(
        "Route was not found from %s to %s for profile %s"
            .formatted(request.startNodeCode(), request.destinationNodeCode(), request.profile()));
  }
}
