package com.kivo.backend.navigation.api;

import com.kivo.backend.navigation.application.route.RouteCreateCommand;
import com.kivo.backend.navigation.application.route.RouteDeliveryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/routes")
public class RouteController {

  private final RouteDeliveryService routeDeliveryService;
  private final RouteResponseMapper routeResponseMapper;

  @PostMapping
  public ResponseEntity<RouteResponse> createRoute(@Valid @RequestBody CreateRouteRequest request) {
    var route =
        routeDeliveryService.create(
            new RouteCreateCommand(request.kioskCode(), request.placeId(), request.profile()));
    URI location = URI.create("/api/v1/routes/" + route.routeId());
    return ResponseEntity.created(location)
        .cacheControl(CacheControl.noStore())
        .body(routeResponseMapper.toResponse(route));
  }

  @GetMapping("/{routeId}")
  public ResponseEntity<RouteResponse> getRoute(@PathVariable UUID routeId) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(routeResponseMapper.toResponse(routeDeliveryService.get(routeId)));
  }

  @GetMapping(
      value = "/{routeId}/floors/{floorCode}/overlay.png",
      produces = MediaType.IMAGE_PNG_VALUE)
  public ResponseEntity<byte[]> getOverlay(
      @PathVariable UUID routeId, @PathVariable @Size(min = 1, max = 32) String floorCode) {
    byte[] content = routeDeliveryService.getOverlay(routeId, floorCode).content();
    return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_PNG)
        .contentLength(content.length)
        .cacheControl(CacheControl.noStore())
        .body(content);
  }
}
