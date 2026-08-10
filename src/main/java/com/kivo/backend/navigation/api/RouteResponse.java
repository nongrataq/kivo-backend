package com.kivo.backend.navigation.api;

import com.kivo.backend.navigation.domain.routing.MovementProfile;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RouteResponse(
    UUID routeId,
    String kioskCode,
    UUID placeId,
    MovementProfile profile,
    BigDecimal totalDistanceMeters,
    long estimatedDurationSeconds,
    Instant createdAt,
    Instant expiresAt,
    List<RouteInstructionResponse> instructions,
    List<RouteOverlayResponse> overlays) {

  public RouteResponse {
    instructions = List.copyOf(instructions);
    overlays = List.copyOf(overlays);
  }
}
