package com.kivo.backend.navigation.application.route;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.kivo.backend.navigation.configuration.RouteDeliveryProperties;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CaffeineRouteStore implements RouteStore {

  private final Cache<UUID, RouteDocument> routes;

  @Autowired
  public CaffeineRouteStore(RouteDeliveryProperties properties) {
    this(properties, Ticker.systemTicker());
  }

  CaffeineRouteStore(RouteDeliveryProperties properties, Ticker ticker) {
    routes =
        Caffeine.newBuilder()
            .maximumSize(properties.getMaximumSize())
            .expireAfterWrite(properties.getTtl())
            .ticker(ticker)
            .build();
  }

  @Override
  public void save(RouteDocument route) {
    routes.put(route.routeId(), route);
  }

  @Override
  public Optional<RouteDocument> find(UUID routeId, Instant now) {
    RouteDocument route = routes.getIfPresent(routeId);
    if (route == null) {
      return Optional.empty();
    }
    if (!now.isBefore(route.expiresAt())) {
      routes.invalidate(routeId);
      return Optional.empty();
    }
    return Optional.of(route);
  }

  void cleanUp() {
    routes.cleanUp();
  }
}
