package com.kivo.backend.navigation.configuration;

import com.kivo.backend.navigation.domain.DefaultGraphValidator;
import com.kivo.backend.navigation.domain.GraphValidator;
import com.kivo.backend.navigation.domain.routing.AStarShortestPath;
import com.kivo.backend.navigation.domain.routing.RoutingCostPolicy;
import com.kivo.backend.navigation.domain.routing.ShortestPathAlgorithm;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({RoutingProperties.class, RouteDeliveryProperties.class})
public class NavigationConfiguration {

  @Bean
  GraphValidator graphValidator() {
    return new DefaultGraphValidator();
  }

  @Bean
  ShortestPathAlgorithm shortestPathAlgorithm() {
    return new AStarShortestPath();
  }

  @Bean
  RoutingCostPolicy routingCostPolicy(RoutingProperties properties) {
    return new RoutingCostPolicy(
        properties.getStandardSpeedMetersPerSecond(),
        properties.getAccessibleSpeedMetersPerSecond(),
        properties.getStairsPenaltySeconds(),
        properties.getEscalatorPenaltySeconds(),
        properties.getElevatorPenaltySeconds());
  }

  @Bean
  @ConditionalOnMissingBean(Clock.class)
  Clock navigationClock() {
    return Clock.systemUTC();
  }
}
