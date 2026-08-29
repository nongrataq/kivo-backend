package com.kivo.backend.sharing.application;

import java.util.UUID;

public record FeedbackShareReadModel(UUID shareId, UUID mallId, String kioskCode, UUID placeId) {}
