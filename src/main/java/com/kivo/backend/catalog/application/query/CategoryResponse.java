package com.kivo.backend.catalog.application.query;

import java.util.UUID;

public record CategoryResponse(UUID id, String code, String name, int displayOrder) {}
