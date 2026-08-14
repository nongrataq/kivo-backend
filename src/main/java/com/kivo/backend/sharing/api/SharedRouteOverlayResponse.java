package com.kivo.backend.sharing.api;

import java.net.URI;

public record SharedRouteOverlayResponse(
    String floorCode,
    int displayOrder,
    String mapImageUrl,
    URI imageUrl,
    int pixelWidth,
    int pixelHeight) {}
