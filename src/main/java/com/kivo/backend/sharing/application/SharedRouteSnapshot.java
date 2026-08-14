package com.kivo.backend.sharing.application;

import com.kivo.backend.navigation.application.route.RouteDocument;
import com.kivo.backend.navigation.domain.routing.MovementProfile;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SharedRouteSnapshot(
    int schemaVersion,
    UUID routeId,
    UUID mallId,
    UUID graphVersionId,
    String kioskCode,
    UUID placeId,
    MovementProfile profile,
    BigDecimal totalDistanceMeters,
    long estimatedDurationSeconds,
    Instant routeCreatedAt,
    List<SharedRouteInstructionSnapshot> instructions,
    List<SharedRouteOverlaySnapshot> overlays) {

  public static final int CURRENT_SCHEMA_VERSION = 1;

  public SharedRouteSnapshot {
    instructions = List.copyOf(instructions);
    overlays = List.copyOf(overlays);
  }

  public static SharedRouteSnapshot from(RouteDocument route) {
    return new SharedRouteSnapshot(
        CURRENT_SCHEMA_VERSION,
        route.routeId(),
        route.mallId(),
        route.graphVersionId(),
        route.kioskCode(),
        route.placeId(),
        route.profile(),
        route.totalDistanceMeters(),
        route.estimatedDurationSeconds(),
        route.createdAt(),
        route.instructions().stream()
            .map(
                instruction ->
                    new SharedRouteInstructionSnapshot(
                        instruction.sequence(),
                        instruction.type(),
                        instruction.floorCode(),
                        instruction.targetFloorCode(),
                        instruction.movementType(),
                        instruction.distanceMeters()))
            .toList(),
        route.overlays().stream()
            .map(
                overlay ->
                    new SharedRouteOverlaySnapshot(
                        overlay.floorId(),
                        overlay.floorCode(),
                        overlay.displayOrder(),
                        overlay.mapImageUrl(),
                        overlay.coordinateWidth(),
                        overlay.coordinateHeight(),
                        overlay.pixelWidth(),
                        overlay.pixelHeight(),
                        overlay.polylines(),
                        overlay.markers()))
            .toList());
  }
}
