package com.kivo.backend.sharing.persistence.entity;

import static com.kivo.backend.sharing.SharingTestFixtures.CREATED_AT;
import static com.kivo.backend.sharing.SharingTestFixtures.GRAPH_VERSION_ID;
import static com.kivo.backend.sharing.SharingTestFixtures.MALL_ID;
import static com.kivo.backend.sharing.SharingTestFixtures.ROUTE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class SharedRouteTest {

  @Test
  void requiresFutureExpiryAndMakesRevocationIdempotent() {
    assertThatThrownBy(
            () ->
                SharedRoute.create(
                    ROUTE_ID,
                    MALL_ID,
                    GRAPH_VERSION_ID,
                    "a".repeat(64),
                    "{}",
                    CREATED_AT,
                    CREATED_AT))
        .isInstanceOf(IllegalArgumentException.class);

    SharedRoute route =
        SharedRoute.create(
            ROUTE_ID,
            MALL_ID,
            GRAPH_VERSION_ID,
            "a".repeat(64),
            "{}",
            CREATED_AT,
            CREATED_AT.plus(Duration.ofHours(2)));
    route.revoke(CREATED_AT.plusSeconds(10));
    route.revoke(CREATED_AT.plusSeconds(20));

    assertThat(route.getRevokedAt()).isEqualTo(CREATED_AT.plusSeconds(10));
    assertThatThrownBy(() -> route.revoke(CREATED_AT.minusSeconds(1)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
