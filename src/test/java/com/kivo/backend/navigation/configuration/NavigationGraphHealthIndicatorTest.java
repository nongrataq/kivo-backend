package com.kivo.backend.navigation.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kivo.backend.navigation.application.InMemoryActiveGraphRegistry;
import com.kivo.backend.navigation.domain.model.GraphSnapshot;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;

class NavigationGraphHealthIndicatorTest {

  @Test
  void reportsOutOfServiceUntilRequiredGraphSnapshotIsActive() {
    InMemoryActiveGraphRegistry registry = new InMemoryActiveGraphRegistry();
    var indicator =
        new NavigationGraphHealthIndicator(registry, new NavigationReadinessProperties(1));

    assertThat(indicator.health().getStatus()).isEqualTo(Status.OUT_OF_SERVICE);

    registry.replace(
        new GraphSnapshot(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "v1",
            List.of(),
            List.of(),
            List.of(),
            List.of()));

    assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
  }
}
