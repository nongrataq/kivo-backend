package com.kivo.backend.catalog.application.port;

import java.util.UUID;

public record RouteDestinationReadModel(UUID placeId, UUID mallId, String navigationNodeCode) {}
