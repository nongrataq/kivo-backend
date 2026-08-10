package com.kivo.backend.maps.application;

public interface RouteOverlayRenderer {

  byte[] render(OverlayRenderRequest request);
}
