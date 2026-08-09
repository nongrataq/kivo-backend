package com.kivo.backend.navigation.domain.routing;

import com.kivo.backend.navigation.domain.model.GraphEdge;
import com.kivo.backend.navigation.domain.model.MovementType;

public record RoutingCostPolicy(
    double standardSpeedMetersPerSecond,
    double accessibleSpeedMetersPerSecond,
    double stairsPenaltySeconds,
    double escalatorPenaltySeconds,
    double elevatorPenaltySeconds) {

  public RoutingCostPolicy {
    requirePositive(standardSpeedMetersPerSecond, "standardSpeedMetersPerSecond");
    requirePositive(accessibleSpeedMetersPerSecond, "accessibleSpeedMetersPerSecond");
    requireNonNegative(stairsPenaltySeconds, "stairsPenaltySeconds");
    requireNonNegative(escalatorPenaltySeconds, "escalatorPenaltySeconds");
    requireNonNegative(elevatorPenaltySeconds, "elevatorPenaltySeconds");
  }

  public static RoutingCostPolicy defaults() {
    return new RoutingCostPolicy(1.4, 1.0, 20.0, 15.0, 45.0);
  }

  public boolean isAllowed(GraphEdge edge, MovementProfile profile) {
    if (edge.closed()) {
      return false;
    }
    return profile != MovementProfile.ACCESSIBLE
        || (edge.wheelchairAccessible() && edge.movementType() != MovementType.STAIRS);
  }

  public double travelSeconds(GraphEdge edge, MovementProfile profile) {
    return edge.lengthMeters().doubleValue() / speed(profile) + penalty(edge.movementType());
  }

  public double speed(MovementProfile profile) {
    return profile == MovementProfile.ACCESSIBLE
        ? accessibleSpeedMetersPerSecond
        : standardSpeedMetersPerSecond;
  }

  private double penalty(MovementType movementType) {
    return switch (movementType) {
      case CORRIDOR -> 0.0;
      case STAIRS -> stairsPenaltySeconds;
      case ESCALATOR -> escalatorPenaltySeconds;
      case ELEVATOR -> elevatorPenaltySeconds;
    };
  }

  private static void requirePositive(double value, String name) {
    if (!Double.isFinite(value) || value <= 0) {
      throw new IllegalArgumentException(name + " must be positive");
    }
  }

  private static void requireNonNegative(double value, String name) {
    if (!Double.isFinite(value) || value < 0) {
      throw new IllegalArgumentException(name + " must be non-negative");
    }
  }
}
