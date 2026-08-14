package com.kivo.backend.sharing.application;

import java.time.Instant;
import java.util.UUID;

public record SharedRouteAccess(
    UUID shareId, String token, Instant expiresAt, SharedRouteSnapshot snapshot) {}
