package com.kivo.backend.navigation.domain.routing;

import com.kivo.backend.navigation.domain.model.GraphPoint;
import com.kivo.backend.navigation.domain.model.MovementType;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

public record RouteStep(
    String edgeCode,
    String fromNodeCode,
    String toNodeCode,
    UUID fromFloorId,
    UUID toFloorId,
    MovementType movementType,
    BigDecimal distanceMeters,
    Duration estimatedDuration,
    List<GraphPoint> geometry) {

  public RouteStep {
    geometry = List.copyOf(geometry);
  }
}
