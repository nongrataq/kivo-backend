package com.kivo.backend.kiosks.application.port;

import java.util.UUID;

public record FeedbackKioskReadModel(UUID kioskId, UUID mallId, String kioskCode) {}
