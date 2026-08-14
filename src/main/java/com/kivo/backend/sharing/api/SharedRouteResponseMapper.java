package com.kivo.backend.sharing.api;

import com.kivo.backend.sharing.application.SharedRouteAccess;
import com.kivo.backend.sharing.application.SharingLinks;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SharedRouteResponseMapper {

  private final SharingLinks sharingLinks;

  public SharedRouteResponse toResponse(SharedRouteAccess access) {
    var snapshot = access.snapshot();
    return new SharedRouteResponse(
        access.shareId(),
        snapshot.routeId(),
        snapshot.mallId(),
        snapshot.graphVersionId(),
        snapshot.kioskCode(),
        snapshot.placeId(),
        snapshot.profile(),
        snapshot.totalDistanceMeters(),
        snapshot.estimatedDurationSeconds(),
        snapshot.routeCreatedAt(),
        access.expiresAt(),
        snapshot.instructions().stream()
            .map(
                instruction ->
                    new SharedRouteInstructionResponse(
                        instruction.sequence(),
                        instruction.type(),
                        instruction.floorCode(),
                        instruction.targetFloorCode(),
                        instruction.movementType(),
                        instruction.distanceMeters()))
            .toList(),
        snapshot.overlays().stream()
            .map(
                overlay ->
                    new SharedRouteOverlayResponse(
                        overlay.floorCode(),
                        overlay.displayOrder(),
                        overlay.mapImageUrl(),
                        sharingLinks.overlayUrl(access.token(), overlay.floorCode()),
                        overlay.pixelWidth(),
                        overlay.pixelHeight()))
            .toList());
  }
}
