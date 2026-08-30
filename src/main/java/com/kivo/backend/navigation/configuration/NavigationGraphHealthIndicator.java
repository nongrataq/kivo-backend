package com.kivo.backend.navigation.configuration;

import com.kivo.backend.navigation.application.InMemoryActiveGraphRegistry;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("navigationGraph")
public class NavigationGraphHealthIndicator implements HealthIndicator {

  private final InMemoryActiveGraphRegistry activeGraphRegistry;
  private final NavigationReadinessProperties properties;

  public NavigationGraphHealthIndicator(
      InMemoryActiveGraphRegistry activeGraphRegistry, NavigationReadinessProperties properties) {
    this.activeGraphRegistry = activeGraphRegistry;
    this.properties = properties;
  }

  @Override
  public Health health() {
    int activeGraphs = activeGraphRegistry.activeSnapshotCount();
    if (activeGraphs < properties.minimumActiveGraphs()) {
      return Health.outOfService()
          .withDetail("activeGraphs", activeGraphs)
          .withDetail("minimumActiveGraphs", properties.minimumActiveGraphs())
          .build();
    }
    return Health.up().withDetail("activeGraphs", activeGraphs).build();
  }
}
