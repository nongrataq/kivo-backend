package com.kivo.backend.sharing.api;

import static com.kivo.backend.sharing.SharingTestFixtures.CREATED_AT;
import static com.kivo.backend.sharing.SharingTestFixtures.snapshot;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kivo.backend.common.configuration.WebMvcConfiguration;
import com.kivo.backend.common.exception.handler.GlobalExceptionHandler;
import com.kivo.backend.sharing.application.RouteSharingService;
import com.kivo.backend.sharing.application.ShareRouteResult;
import com.kivo.backend.sharing.application.SharedRouteAccess;
import com.kivo.backend.sharing.application.SharedRouteFloorNotFoundException;
import com.kivo.backend.sharing.application.SharedRouteNotFoundException;
import com.kivo.backend.sharing.application.SharingLinks;
import com.kivo.backend.sharing.configuration.SharingConfiguration;
import java.net.URI;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    value = {
      RouteSharingController.class,
      PublicRouteController.class,
      MobileRouteController.class
    },
    properties = "kivo.sharing.public-base-url=https://routes.example")
@AutoConfigureMockMvc(addFilters = false)
@Import({
  GlobalExceptionHandler.class,
  WebMvcConfiguration.class,
  SharingConfiguration.class,
  SharingLinks.class,
  SharedRouteResponseMapper.class
})
class RouteSharingControllerTest {

  private static final UUID ROUTE_ID = UUID.fromString("73000000-0000-0000-0000-000000000001");
  private static final UUID SHARE_ID = UUID.fromString("74000000-0000-0000-0000-000000000001");
  private static final String TOKEN = "a".repeat(43);
  private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4e, 0x47};

  @Autowired private MockMvc mockMvc;

  @MockitoBean private RouteSharingService routeSharingService;

  @Test
  void createsShareWithAbsoluteLinksAndNoStore() throws Exception {
    URI routeUrl = URI.create("https://routes.example/r/" + TOKEN);
    URI qrUrl = URI.create("https://routes.example/api/v1/public/routes/" + TOKEN + "/qr.png");
    when(routeSharingService.share(ROUTE_ID))
        .thenReturn(
            new ShareRouteResult(SHARE_ID, routeUrl, qrUrl, CREATED_AT.plus(Duration.ofHours(2))));

    mockMvc
        .perform(post("/api/v1/routes/{routeId}/share", ROUTE_ID))
        .andExpect(status().isCreated())
        .andExpect(header().string(HttpHeaders.LOCATION, routeUrl.toString()))
        .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
        .andExpect(jsonPath("$.shareId").value(SHARE_ID.toString()))
        .andExpect(jsonPath("$.routeUrl").value(routeUrl.toString()))
        .andExpect(jsonPath("$.qrImageUrl").value(qrUrl.toString()))
        .andExpect(jsonPath("$.expiresAt").value("2026-08-10T12:00:00Z"));

    mockMvc
        .perform(post("/api/v1/routes/not-a-uuid/share"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void servesPublicSnapshotQrOverlayAndMobilePageWithoutCaching() throws Exception {
    SharedRouteAccess access = access();
    when(routeSharingService.get(TOKEN)).thenReturn(access);
    when(routeSharingService.getQrCode(TOKEN)).thenReturn(PNG);
    when(routeSharingService.getOverlay(TOKEN, "L1")).thenReturn(PNG);

    mockMvc
        .perform(get("/api/v1/public/routes/{shareToken}", TOKEN))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
        .andExpect(jsonPath("$.shareId").value(SHARE_ID.toString()))
        .andExpect(jsonPath("$.routeId").value(ROUTE_ID.toString()))
        .andExpect(
            jsonPath("$.overlays[0].mapImageUrl")
                .value("https://cdn.example.com/maps/demo-mall/L1.png"))
        .andExpect(
            jsonPath("$.overlays[0].imageUrl")
                .value(
                    "https://routes.example/api/v1/public/routes/"
                        + TOKEN
                        + "/floors/L1/overlay.png"))
        .andExpect(jsonPath("$..polylines").doesNotExist())
        .andExpect(jsonPath("$..token").doesNotExist());

    mockMvc
        .perform(get("/api/v1/public/routes/{shareToken}/qr.png", TOKEN))
        .andExpect(status().isOk())
        .andExpect(content().contentType("image/png"))
        .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, PNG.length))
        .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
        .andExpect(content().bytes(PNG));

    mockMvc
        .perform(get("/api/v1/public/routes/{shareToken}/floors/L1/overlay.png", TOKEN))
        .andExpect(status().isOk())
        .andExpect(content().contentType("image/png"))
        .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
        .andExpect(content().bytes(PNG));

    mockMvc
        .perform(get("/r/{shareToken}", TOKEN))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("text/html"))
        .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Ваш маршрут")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("const basePath")));
  }

  @Test
  void mapsUnknownExpiredRevokedAndMissingFloorToStableNotFoundErrors() throws Exception {
    when(routeSharingService.get(TOKEN)).thenThrow(new SharedRouteNotFoundException());
    when(routeSharingService.getQrCode(TOKEN)).thenThrow(new SharedRouteNotFoundException());
    when(routeSharingService.getOverlay(TOKEN, "L9"))
        .thenThrow(new SharedRouteFloorNotFoundException("L9"));

    mockMvc
        .perform(get("/api/v1/public/routes/{shareToken}", TOKEN))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("SHARED_ROUTE_NOT_FOUND"));
    mockMvc
        .perform(get("/api/v1/public/routes/{shareToken}/qr.png", TOKEN))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("SHARED_ROUTE_NOT_FOUND"));
    mockMvc
        .perform(get("/api/v1/public/routes/{shareToken}/floors/L9/overlay.png", TOKEN))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("SHARED_ROUTE_FLOOR_NOT_FOUND"));

    String oversizedFloor = "x".repeat(33);
    mockMvc
        .perform(
            get(
                "/api/v1/public/routes/{shareToken}/floors/{floorCode}/overlay.png",
                TOKEN,
                oversizedFloor))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  private static SharedRouteAccess access() {
    return new SharedRouteAccess(SHARE_ID, TOKEN, CREATED_AT.plus(Duration.ofHours(2)), snapshot());
  }
}
