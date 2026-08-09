package com.kivo.backend.catalog.application.port;

public record CatalogFloorReadModel(
    String code,
    String name,
    int levelNumber,
    int displayOrder,
    String mapImageUrl,
    int coordinateWidth,
    int coordinateHeight) {}
