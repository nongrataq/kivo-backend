package com.kivo.backend.feedback.application;

import java.util.UUID;

public record CreateFeedbackCommand(UUID routeId, UUID shareId, int rating) {}
