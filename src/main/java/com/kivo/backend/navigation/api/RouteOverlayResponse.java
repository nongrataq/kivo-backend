package com.kivo.backend.navigation.api;

public record RouteOverlayResponse(
    String floorCode, int displayOrder, String imageUrl, int pixelWidth, int pixelHeight) {}
