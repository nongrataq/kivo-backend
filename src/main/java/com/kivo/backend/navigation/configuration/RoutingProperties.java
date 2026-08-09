package com.kivo.backend.navigation.configuration;

import jakarta.validation.constraints.DecimalMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "kivo.navigation.routing")
public class RoutingProperties {

  @DecimalMin(value = "0.000001")
  private double standardSpeedMetersPerSecond = 1.4;

  @DecimalMin(value = "0.000001")
  private double accessibleSpeedMetersPerSecond = 1.0;

  @DecimalMin("0")
  private double stairsPenaltySeconds = 20.0;

  @DecimalMin("0")
  private double escalatorPenaltySeconds = 15.0;

  @DecimalMin("0")
  private double elevatorPenaltySeconds = 45.0;

  public double getStandardSpeedMetersPerSecond() {
    return standardSpeedMetersPerSecond;
  }

  public void setStandardSpeedMetersPerSecond(double standardSpeedMetersPerSecond) {
    this.standardSpeedMetersPerSecond = standardSpeedMetersPerSecond;
  }

  public double getAccessibleSpeedMetersPerSecond() {
    return accessibleSpeedMetersPerSecond;
  }

  public void setAccessibleSpeedMetersPerSecond(double accessibleSpeedMetersPerSecond) {
    this.accessibleSpeedMetersPerSecond = accessibleSpeedMetersPerSecond;
  }

  public double getStairsPenaltySeconds() {
    return stairsPenaltySeconds;
  }

  public void setStairsPenaltySeconds(double stairsPenaltySeconds) {
    this.stairsPenaltySeconds = stairsPenaltySeconds;
  }

  public double getEscalatorPenaltySeconds() {
    return escalatorPenaltySeconds;
  }

  public void setEscalatorPenaltySeconds(double escalatorPenaltySeconds) {
    this.escalatorPenaltySeconds = escalatorPenaltySeconds;
  }

  public double getElevatorPenaltySeconds() {
    return elevatorPenaltySeconds;
  }

  public void setElevatorPenaltySeconds(double elevatorPenaltySeconds) {
    this.elevatorPenaltySeconds = elevatorPenaltySeconds;
  }
}
