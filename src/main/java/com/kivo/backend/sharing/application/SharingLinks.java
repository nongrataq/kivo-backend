package com.kivo.backend.sharing.application;

import com.kivo.backend.sharing.configuration.SharingProperties;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class SharingLinks {

  private final SharingProperties properties;

  public URI routeUrl(String token) {
    return build("r", token);
  }

  public URI qrImageUrl(String token) {
    return build("api", "v1", "public", "routes", token, "qr.png");
  }

  public URI overlayUrl(String token, String floorCode) {
    return build("api", "v1", "public", "routes", token, "floors", floorCode, "overlay.png");
  }

  private URI build(String... pathSegments) {
    UriComponentsBuilder builder = UriComponentsBuilder.fromUri(properties.getPublicBaseUrl());
    String path = properties.getPublicBaseUrl().getPath();
    if (path == null || path.isBlank() || "/".equals(path)) {
      builder.replacePath(null);
    }
    return builder.pathSegment(pathSegments).build().encode().toUri();
  }
}
