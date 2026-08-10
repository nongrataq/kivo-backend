package com.kivo.backend.navigation.api;

import com.kivo.backend.navigation.application.route.RouteDocument;
import org.springframework.stereotype.Component;

@Component
public class RouteResponseMapper {

  public RouteResponse toResponse(RouteDocument route) {
    return new RouteResponse(
        route.routeId(),
        route.kioskCode(),
        route.placeId(),
        route.profile(),
        route.totalDistanceMeters(),
        route.estimatedDurationSeconds(),
        route.createdAt(),
        route.expiresAt(),
        route.instructions().stream()
            .map(
                instruction ->
                    new RouteInstructionResponse(
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
                    new RouteOverlayResponse(
                        overlay.floorCode(),
                        overlay.displayOrder(),
                        overlay.imageUrl(),
                        overlay.pixelWidth(),
                        overlay.pixelHeight()))
            .toList());
  }
}
