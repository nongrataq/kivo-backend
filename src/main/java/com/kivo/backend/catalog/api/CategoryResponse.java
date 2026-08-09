package com.kivo.backend.catalog.api;

import java.util.UUID;

public record CategoryResponse(UUID id, String code, String name, int displayOrder) {}
