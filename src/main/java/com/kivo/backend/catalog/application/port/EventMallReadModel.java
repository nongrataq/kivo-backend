package com.kivo.backend.catalog.application.port;

import java.util.UUID;

public record EventMallReadModel(
    UUID mallId, String mallCode, String timezone, String defaultLanguage) {}
