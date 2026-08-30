package com.kivo.backend.navigation.application.port;

import java.util.UUID;

public record FeedbackRouteReadModel(UUID routeId, UUID mallId, String kioskCode, UUID placeId) {}
