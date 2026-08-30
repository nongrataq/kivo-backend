package com.kivo.backend.admin.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("kivo.security")
public record SecurityProperties(@Valid Admin admin, boolean requireHttps, @Valid Hsts hsts) {

  public record Admin(
      @NotBlank String username,
      @NotBlank
          @Pattern(
              regexp = "^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$",
              message = "must be a BCrypt hash")
          String passwordHash) {}

  public record Hsts(Duration maxAge, boolean includeSubdomains, boolean preload) {

    @AssertTrue(message = "HSTS max-age must be positive")
    public boolean isMaxAgePositive() {
      return maxAge != null && !maxAge.isZero() && !maxAge.isNegative();
    }
  }
}
