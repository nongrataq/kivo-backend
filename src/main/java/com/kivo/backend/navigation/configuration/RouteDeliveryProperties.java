package com.kivo.backend.navigation.configuration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Setter
@Getter
@Validated
@ConfigurationProperties("kivo.navigation.routes")
public class RouteDeliveryProperties {

  @NotNull private Duration ttl = Duration.ofMinutes(30);

  @Positive private long maximumSize = 10_000;

  @Min(1)
  @Max(2048)
  private int overlayMaxDimension = 2048;
}
