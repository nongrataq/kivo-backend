package com.kivo.backend.kiosks.application.port;

import java.util.UUID;

public record RouteStartReadModel(UUID mallId, String kioskCode, String navigationNodeCode) {}
