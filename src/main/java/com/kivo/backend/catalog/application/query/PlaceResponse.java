package com.kivo.backend.catalog.application.query;

import java.util.UUID;

public record PlaceResponse(
    UUID id,
    String code,
    String name,
    String description,
    String floorCode,
    UUID categoryId,
    String categoryCode,
    int displayOrder) {}
