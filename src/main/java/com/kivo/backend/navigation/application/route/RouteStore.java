package com.kivo.backend.navigation.application.route;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RouteStore {

  void save(RouteDocument route);

  Optional<RouteDocument> find(UUID routeId, Instant now);
}
