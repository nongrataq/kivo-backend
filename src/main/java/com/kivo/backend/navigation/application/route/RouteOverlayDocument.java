package com.kivo.backend.navigation.application.route;

import com.kivo.backend.maps.application.OverlayMarker;
import com.kivo.backend.maps.application.OverlayPolyline;
import com.kivo.backend.maps.application.OverlayRenderRequest;
import java.util.List;
import java.util.UUID;

public record RouteOverlayDocument(
    UUID floorId,
    String floorCode,
    int displayOrder,
    String imageUrl,
    int coordinateWidth,
    int coordinateHeight,
    int pixelWidth,
    int pixelHeight,
    List<OverlayPolyline> polylines,
    List<OverlayMarker> markers) {

  public RouteOverlayDocument {
    polylines = List.copyOf(polylines);
    markers = List.copyOf(markers);
  }

  public OverlayRenderRequest toRenderRequest() {
    return new OverlayRenderRequest(
        coordinateWidth, coordinateHeight, pixelWidth, pixelHeight, polylines, markers);
  }
}
