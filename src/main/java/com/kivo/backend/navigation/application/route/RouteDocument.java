package com.kivo.backend.navigation.application.route;

import com.kivo.backend.navigation.domain.routing.MovementProfile;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RouteDocument(
    UUID routeId,
    UUID mallId,
    UUID graphVersionId,
    String kioskCode,
    UUID placeId,
    MovementProfile profile,
    BigDecimal totalDistanceMeters,
    long estimatedDurationSeconds,
    Instant createdAt,
    Instant expiresAt,
    List<RouteInstructionDocument> instructions,
    List<RouteOverlayDocument> overlays) {

  public RouteDocument {
    instructions = List.copyOf(instructions);
    overlays = List.copyOf(overlays);
  }
}
