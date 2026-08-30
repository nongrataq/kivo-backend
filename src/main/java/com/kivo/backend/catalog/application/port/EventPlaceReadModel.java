package com.kivo.backend.catalog.application.port;

import java.util.UUID;

public record EventPlaceReadModel(
    UUID placeId, UUID mallId, String placeCode, String placeName, String floorCode) {}
