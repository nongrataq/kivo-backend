package com.kivo.backend.maps.application;

import java.util.List;

public record OverlayRenderRequest(
    int coordinateWidth,
    int coordinateHeight,
    int pixelWidth,
    int pixelHeight,
    List<OverlayPolyline> polylines,
    List<OverlayMarker> markers) {

  public OverlayRenderRequest {
    if (coordinateWidth <= 0 || coordinateHeight <= 0 || pixelWidth <= 0 || pixelHeight <= 0) {
      throw new IllegalArgumentException("Overlay dimensions must be positive");
    }
    polylines = List.copyOf(polylines);
    markers = List.copyOf(markers);
  }
}
