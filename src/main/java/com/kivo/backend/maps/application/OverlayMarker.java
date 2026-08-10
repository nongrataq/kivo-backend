package com.kivo.backend.maps.application;

import java.util.Objects;

public record OverlayMarker(OverlayPoint point, OverlayMarkerType type) {

  public OverlayMarker {
    Objects.requireNonNull(point, "point");
    Objects.requireNonNull(type, "type");
  }
}
