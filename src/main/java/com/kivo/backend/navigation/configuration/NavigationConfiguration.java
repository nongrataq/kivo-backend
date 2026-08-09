package com.kivo.backend.navigation.configuration;

import com.kivo.backend.navigation.domain.DefaultGraphValidator;
import com.kivo.backend.navigation.domain.GraphValidator;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class NavigationConfiguration {

  @Bean
  GraphValidator graphValidator() {
    return new DefaultGraphValidator();
  }

  @Bean
  @ConditionalOnMissingBean(Clock.class)
  Clock navigationClock() {
    return Clock.systemUTC();
  }
}
