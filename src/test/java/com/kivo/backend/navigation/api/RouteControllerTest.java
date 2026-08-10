package com.kivo.backend.navigation.api;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kivo.backend.common.configuration.WebMvcConfiguration;
import com.kivo.backend.common.exception.handler.GlobalExceptionHandler;
import com.kivo.backend.navigation.application.exception.NavigationGraphUnavailableException;
import com.kivo.backend.navigation.application.exception.RouteEndpointMismatchException;
import com.kivo.backend.navigation.application.exception.RouteFloorNotFoundException;
import com.kivo.backend.navigation.application.exception.RouteNotFoundServiceException;
import com.kivo.backend.navigation.application.route.RenderedRouteOverlay;
import com.kivo.backend.navigation.application.route.RouteDeliveryService;
import com.kivo.backend.navigation.application.route.RouteDocument;
import com.kivo.backend.navigation.application.route.RouteInstructionDocument;
import com.kivo.backend.navigation.application.route.RouteOverlayDocument;
import com.kivo.backend.navigation.domain.model.MovementType;
import com.kivo.backend.navigation.domain.routing.InstructionType;
import com.kivo.backend.navigation.domain.routing.MovementProfile;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RouteController.class)
@Import({GlobalExceptionHandler.class, WebMvcConfiguration.class, RouteResponseMapper.class})
class RouteControllerTest {

  private static final UUID ROUTE_ID = UUID.fromString("72000000-0000-0000-0000-000000000001");
  private static final UUID PLACE_ID = UUID.fromString("72000000-0000-0000-0000-000000000002");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private RouteDeliveryService routeDeliveryService;

  @Test
  void createsRouteWithLocationAndFlatPublicJson() throws Exception {
    when(routeDeliveryService.create(org.mockito.ArgumentMatchers.any())).thenReturn(route());

    mockMvc
        .perform(
            post("/api/v1/routes")
                .contentType("application/json")
                .content(
                    """
                    {"kioskCode":"demo-kiosk","placeId":"%s"}
                    """
                        .formatted(PLACE_ID)))
        .andExpect(status().isCreated())
        .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/routes/" + ROUTE_ID))
        .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
        .andExpect(jsonPath("$.profile").value("STANDARD"))
        .andExpect(jsonPath("$.totalDistanceMeters").value(20.0))
        .andExpect(jsonPath("$.instructions[0].floorCode").value("L1"))
        .andExpect(jsonPath("$.instructions[0].nodeCode").doesNotExist())
        .andExpect(
            jsonPath("$.overlays[0].imageUrl")
                .value("/api/v1/routes/" + ROUTE_ID + "/floors/L1/overlay.png"))
        .andExpect(jsonPath("$.overlays[0].polylines").doesNotExist());
  }

  @Test
  void returnsStoredRouteAndPngWithoutCaching() throws Exception {
    byte[] png = {(byte) 0x89, 0x50, 0x4e, 0x47};
    when(routeDeliveryService.get(ROUTE_ID)).thenReturn(route());
    when(routeDeliveryService.getOverlay(ROUTE_ID, "L1")).thenReturn(new RenderedRouteOverlay(png));

    mockMvc
        .perform(get("/api/v1/routes/{routeId}", ROUTE_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.routeId").value(ROUTE_ID.toString()));
    mockMvc
        .perform(get("/api/v1/routes/{routeId}/floors/L1/overlay.png", ROUTE_ID))
        .andExpect(status().isOk())
        .andExpect(content().contentType("image/png"))
        .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, png.length))
        .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
        .andExpect(content().bytes(png));
  }

  @Test
  void validatesBodyAndMapsTypedErrors() throws Exception {
    mockMvc
        .perform(post("/api/v1/routes").contentType("application/json").content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

    mockMvc
        .perform(
            post("/api/v1/routes")
                .contentType("application/json")
                .content(
                    """
                    {"kioskCode":"demo-kiosk","placeId":"%s","profile":"UNKNOWN"}
                    """
                        .formatted(PLACE_ID)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

    mockMvc
        .perform(get("/api/v1/routes/not-a-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

    when(routeDeliveryService.get(ROUTE_ID)).thenThrow(new RouteNotFoundServiceException());
    mockMvc
        .perform(get("/api/v1/routes/{routeId}", ROUTE_ID))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ROUTE_NOT_FOUND"));

    when(routeDeliveryService.getOverlay(ROUTE_ID, "L9"))
        .thenThrow(new RouteFloorNotFoundException(ROUTE_ID, "L9"));
    mockMvc
        .perform(get("/api/v1/routes/{routeId}/floors/L9/overlay.png", ROUTE_ID))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ROUTE_FLOOR_NOT_FOUND"));
  }

  @Test
  void mapsEndpointMismatchAndUnavailableGraph() throws Exception {
    when(routeDeliveryService.create(org.mockito.ArgumentMatchers.any()))
        .thenThrow(new RouteEndpointMismatchException("demo-kiosk", PLACE_ID));
    String request =
        """
        {"kioskCode":"demo-kiosk","placeId":"%s"}
        """
            .formatted(PLACE_ID);

    mockMvc
        .perform(post("/api/v1/routes").contentType("application/json").content(request))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value("ROUTE_ENDPOINT_MISMATCH"));

    doThrow(new NavigationGraphUnavailableException())
        .when(routeDeliveryService)
        .create(org.mockito.ArgumentMatchers.any());
    mockMvc
        .perform(post("/api/v1/routes").contentType("application/json").content(request))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("NAVIGATION_GRAPH_UNAVAILABLE"));
  }

  private static RouteDocument route() {
    Instant created = Instant.parse("2026-08-10T10:00:00Z");
    return new RouteDocument(
        ROUTE_ID,
        UUID.randomUUID(),
        UUID.randomUUID(),
        "demo-kiosk",
        PLACE_ID,
        MovementProfile.STANDARD,
        new BigDecimal("20.00"),
        15,
        created,
        created.plusSeconds(1800),
        List.of(
            new RouteInstructionDocument(0, InstructionType.START, "L1", null, null, null),
            new RouteInstructionDocument(
                1,
                InstructionType.CONTINUE,
                "L1",
                null,
                MovementType.CORRIDOR,
                new BigDecimal("20.00")),
            new RouteInstructionDocument(2, InstructionType.ARRIVE, "L1", null, null, null)),
        List.of(
            new RouteOverlayDocument(
                UUID.randomUUID(),
                "L1",
                10,
                "/api/v1/routes/" + ROUTE_ID + "/floors/L1/overlay.png",
                10000,
                10000,
                2048,
                2048,
                List.of(),
                List.of())));
  }
}
