package com.kivo.backend.sharing.application;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record ShareRouteResult(UUID shareId, URI routeUrl, URI qrImageUrl, Instant expiresAt) {}
