package com.kivo.backend.navigation.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record GraphEdge(
    String code,
    String fromNodeCode,
    String toNodeCode,
    BigDecimal lengthMeters,
    MovementType movementType,
    boolean closed,
    boolean wheelchairAccessible,
    List<GraphPoint> geometry) {

  public GraphEdge {
    Objects.requireNonNull(code, "code");
    Objects.requireNonNull(fromNodeCode, "fromNodeCode");
    Objects.requireNonNull(toNodeCode, "toNodeCode");
    Objects.requireNonNull(movementType, "movementType");
    geometry = List.copyOf(geometry);
  }
}
