package com.kivo.backend.catalog.api;

import java.util.UUID;

public record PlaceResponse(
    UUID id,
    String code,
    String name,
    String description,
    UUID categoryId,
    String floorCode,
    String navigationNodeCode,
    int displayOrder) {}
