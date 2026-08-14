package com.kivo.backend.catalog.application.port;

import java.util.UUID;

public record RouteFloorReadModel(
    UUID id,
    String code,
    int displayOrder,
    String mapImageUrl,
    int coordinateWidth,
    int coordinateHeight) {}
