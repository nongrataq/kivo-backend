package com.kivo.backend.maps.application;

import java.util.List;

public record OverlayPolyline(List<OverlayPoint> points) {

  public OverlayPolyline {
    points = List.copyOf(points);
  }
}
