package com.kivo.backend.navigation.domain.model;

import java.util.Objects;
import java.util.UUID;

public record GraphNode(
    String code,
    UUID floorId,
    UUID floorMallId,
    int coordinateWidth,
    int coordinateHeight,
    int x,
    int y,
    NodeType type) {

  public GraphNode {
    Objects.requireNonNull(code, "code");
    Objects.requireNonNull(floorId, "floorId");
    Objects.requireNonNull(floorMallId, "floorMallId");
    Objects.requireNonNull(type, "type");
  }

  public GraphPoint point() {
    return new GraphPoint(x, y);
  }
}
