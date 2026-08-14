package com.kivo.backend.sharing.api;

import com.kivo.backend.sharing.application.RouteSharingService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/routes")
public class RouteSharingController {

  private final RouteSharingService routeSharingService;

  @PostMapping("/{routeId}/share")
  public ResponseEntity<ShareRouteResponse> share(@PathVariable UUID routeId) {
    var result = routeSharingService.share(routeId);
    return ResponseEntity.created(result.routeUrl())
        .cacheControl(CacheControl.noStore())
        .body(
            new ShareRouteResponse(
                result.shareId(), result.routeUrl(), result.qrImageUrl(), result.expiresAt()));
  }
}
