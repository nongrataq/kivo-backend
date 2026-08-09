package com.kivo.backend.navigation.domain.routing;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

public record RouteResult(
    UUID mallId,
    UUID graphVersionId,
    MovementProfile profile,
    String startNodeCode,
    String destinationNodeCode,
    BigDecimal totalDistanceMeters,
    Duration estimatedDuration,
    List<RouteStep> steps,
    List<RouteInstruction> instructions) {

  public RouteResult {
    steps = List.copyOf(steps);
    instructions = List.copyOf(instructions);
  }
}
