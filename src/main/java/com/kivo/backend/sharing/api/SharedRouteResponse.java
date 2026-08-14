package com.kivo.backend.sharing.api;

import com.kivo.backend.navigation.domain.routing.MovementProfile;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SharedRouteResponse(
    UUID shareId,
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
    List<SharedRouteInstructionResponse> instructions,
    List<SharedRouteOverlayResponse> overlays) {

  public SharedRouteResponse {
    instructions = List.copyOf(instructions);
    overlays = List.copyOf(overlays);
  }
}
