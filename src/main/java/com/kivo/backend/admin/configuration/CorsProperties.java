package com.kivo.backend.admin.configuration;

import jakarta.validation.constraints.AssertTrue;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("kivo.http.cors")
public record CorsProperties(
    List<String> allowedOrigins,
    List<String> allowedMethods,
    List<String> allowedHeaders,
    List<String> exposedHeaders,
    boolean allowCredentials) {

  public CorsProperties {
    allowedOrigins = clean(allowedOrigins);
    allowedMethods = clean(allowedMethods);
    allowedHeaders = clean(allowedHeaders);
    exposedHeaders = clean(exposedHeaders);
  }

  @AssertTrue(message = "CORS origins must be an explicit allowlist")
  public boolean isExplicitAllowlist() {
    return allowedOrigins.stream().noneMatch("*"::equals);
  }

  private static List<String> clean(List<String> values) {
    return values == null
        ? List.of()
        : values.stream().map(String::trim).filter(value -> !value.isEmpty()).toList();
  }
}
