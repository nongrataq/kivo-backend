package com.kivo.backend.sharing.api;

import com.kivo.backend.sharing.application.RouteSharingService;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class MobileRouteController {

  private static final Resource MOBILE_PAGE = new ClassPathResource("static/mobile-route.html");

  private final RouteSharingService routeSharingService;

  @GetMapping(value = "/r/{shareToken}", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<Resource> get(@PathVariable String shareToken) {
    routeSharingService.get(shareToken);
    return ResponseEntity.ok()
        .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
        .cacheControl(CacheControl.noStore())
        .body(MOBILE_PAGE);
  }
}
