package com.kivo.backend.common.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("kivo.http.rate-limit")
public record RateLimitProperties(
    boolean enabled, @Min(1) long maximumClients, @Valid Map<String, Policy> policies) {

  public RateLimitProperties {
    policies =
        policies == null
            ? Map.of()
            : java.util.Collections.unmodifiableMap(new LinkedHashMap<>(policies));
  }

  public record Policy(
      @NotBlank String method, @NotBlank String path, @Min(1) int capacity, Duration window) {

    @AssertTrue(message = "rate-limit window must be positive")
    public boolean isWindowPositive() {
      return window != null && !window.isZero() && !window.isNegative();
    }
  }
}
