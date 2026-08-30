package com.kivo.backend.common.configuration;

import com.kivo.backend.common.web.ProblemResponseWriter;
import com.kivo.backend.common.web.RateLimitFilter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfiguration {

  @Bean
  RateLimitFilter rateLimitFilter(
      RateLimitProperties properties,
      ProblemResponseWriter problemResponseWriter,
      MeterRegistry meterRegistry) {
    return new RateLimitFilter(properties, problemResponseWriter, meterRegistry, Clock.systemUTC());
  }

  @Bean
  FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(
      RateLimitFilter rateLimitFilter) {
    FilterRegistrationBean<RateLimitFilter> registration =
        new FilterRegistrationBean<>(rateLimitFilter);
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
    return registration;
  }
}
