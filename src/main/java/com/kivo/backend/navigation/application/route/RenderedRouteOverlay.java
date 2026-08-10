package com.kivo.backend.navigation.application.route;

public record RenderedRouteOverlay(byte[] content) {

  public RenderedRouteOverlay {
    content = content.clone();
  }

  @Override
  public byte[] content() {
    return content.clone();
  }
}
