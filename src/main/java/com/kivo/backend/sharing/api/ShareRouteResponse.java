package com.kivo.backend.sharing.api;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record ShareRouteResponse(UUID shareId, URI routeUrl, URI qrImageUrl, Instant expiresAt) {}
