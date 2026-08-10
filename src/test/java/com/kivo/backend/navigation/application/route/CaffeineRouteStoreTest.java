package com.kivo.backend.navigation.application.route;

import static org.assertj.core.api.Assertions.assertThat;

import com.kivo.backend.navigation.configuration.RouteDeliveryProperties;
import com.kivo.backend.navigation.domain.routing.MovementProfile;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class CaffeineRouteStoreTest {

  @Test
  void removesRouteAtDocumentExpiry() {
    RouteDeliveryProperties properties = properties(10);
    CaffeineRouteStore store = new CaffeineRouteStore(properties);
    Instant createdAt = Instant.parse("2026-08-10T10:00:00Z");
    RouteDocument route = route(createdAt, createdAt.plusSeconds(60));
    store.save(route);

    assertThat(store.find(route.routeId(), createdAt.plusSeconds(59))).contains(route);
    assertThat(store.find(route.routeId(), createdAt.plusSeconds(60))).isEmpty();
  }

  @Test
  void caffeineExpiryAndMaximumSizeKeepStoreBounded() {
    RouteDeliveryProperties properties = properties(1);
    AtomicLong nanos = new AtomicLong();
    CaffeineRouteStore store = new CaffeineRouteStore(properties, nanos::get);
    Instant now = Instant.parse("2026-08-10T10:00:00Z");
    RouteDocument first = route(now, now.plus(Duration.ofHours(1)));
    RouteDocument second = route(now, now.plus(Duration.ofHours(1)));

    store.save(first);
    store.save(second);
    store.cleanUp();

    long present =
        List.of(first.routeId(), second.routeId()).stream()
            .filter(id -> store.find(id, now).isPresent())
            .count();
    assertThat(present).isLessThanOrEqualTo(1);

    nanos.set(Duration.ofMinutes(31).toNanos());
    store.cleanUp();
    assertThat(store.find(first.routeId(), now)).isEmpty();
    assertThat(store.find(second.routeId(), now)).isEmpty();
  }

  private static RouteDeliveryProperties properties(long maximumSize) {
    RouteDeliveryProperties properties = new RouteDeliveryProperties();
    properties.setTtl(Duration.ofMinutes(30));
    properties.setMaximumSize(maximumSize);
    return properties;
  }

  private static RouteDocument route(Instant createdAt, Instant expiresAt) {
    return new RouteDocument(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "kiosk",
        UUID.randomUUID(),
        MovementProfile.STANDARD,
        BigDecimal.ZERO.setScale(2),
        0,
        createdAt,
        expiresAt,
        List.of(),
        List.of());
  }
}
