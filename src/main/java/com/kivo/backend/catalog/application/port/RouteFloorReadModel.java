package com.kivo.backend.catalog.application.port;

import java.util.UUID;

public record RouteFloorReadModel(
    UUID id, String code, int displayOrder, int coordinateWidth, int coordinateHeight) {}
