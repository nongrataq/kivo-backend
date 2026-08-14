package com.kivo.backend.sharing.configuration;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties("kivo.sharing")
public class SharingProperties {

  @NotNull private Duration ttl = Duration.ofHours(2);

  @NotNull private URI publicBaseUrl = URI.create("http://localhost:8080");

  @Min(128)
  @Max(1024)
  private int qrSize = 384;

  @AssertTrue(message = "kivo.sharing.ttl must be positive")
  public boolean isTtlPositive() {
    return ttl != null && !ttl.isZero() && !ttl.isNegative();
  }

  @AssertTrue(message = "kivo.sharing.public-base-url must be an absolute HTTP(S) URL")
  public boolean isPublicBaseUrlValid() {
    if (publicBaseUrl == null || !publicBaseUrl.isAbsolute()) {
      return false;
    }
    return "http".equalsIgnoreCase(publicBaseUrl.getScheme())
        || "https".equalsIgnoreCase(publicBaseUrl.getScheme());
  }
}
