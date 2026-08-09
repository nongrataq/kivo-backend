package com.kivo.backend.navigation.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kivo.backend.navigation.domain.routing.RoutingCostPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class RoutingConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner().withUserConfiguration(NavigationConfiguration.class);

  @Test
  void bindsRoutingCostsFromProperties() {
    contextRunner
        .withPropertyValues(
            "kivo.navigation.routing.standard-speed-meters-per-second=2.0",
            "kivo.navigation.routing.accessible-speed-meters-per-second=0.8",
            "kivo.navigation.routing.stairs-penalty-seconds=30",
            "kivo.navigation.routing.escalator-penalty-seconds=12",
            "kivo.navigation.routing.elevator-penalty-seconds=50")
        .run(
            context -> {
              RoutingCostPolicy costs = context.getBean(RoutingCostPolicy.class);
              assertThat(costs.standardSpeedMetersPerSecond()).isEqualTo(2.0);
              assertThat(costs.accessibleSpeedMetersPerSecond()).isEqualTo(0.8);
              assertThat(costs.stairsPenaltySeconds()).isEqualTo(30.0);
              assertThat(costs.escalatorPenaltySeconds()).isEqualTo(12.0);
              assertThat(costs.elevatorPenaltySeconds()).isEqualTo(50.0);
            });
  }
}
