package com.kivo.backend.sharing.api;

import com.kivo.backend.sharing.application.RouteSharingService;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public/routes")
public class PublicRouteController {

  private final RouteSharingService routeSharingService;
  private final SharedRouteResponseMapper responseMapper;

  @GetMapping("/{shareToken}")
  public ResponseEntity<SharedRouteResponse> get(@PathVariable String shareToken) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(responseMapper.toResponse(routeSharingService.get(shareToken)));
  }

  @GetMapping(value = "/{shareToken}/qr.png", produces = MediaType.IMAGE_PNG_VALUE)
  public ResponseEntity<byte[]> getQrCode(@PathVariable String shareToken) {
    byte[] content = routeSharingService.getQrCode(shareToken);
    return png(content);
  }

  @GetMapping(
      value = "/{shareToken}/floors/{floorCode}/overlay.png",
      produces = MediaType.IMAGE_PNG_VALUE)
  public ResponseEntity<byte[]> getOverlay(
      @PathVariable String shareToken, @PathVariable @Size(min = 1, max = 32) String floorCode) {
    byte[] content = routeSharingService.getOverlay(shareToken, floorCode);
    return png(content);
  }

  private static ResponseEntity<byte[]> png(byte[] content) {
    return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_PNG)
        .contentLength(content.length)
        .cacheControl(CacheControl.noStore())
        .body(content);
  }
}
