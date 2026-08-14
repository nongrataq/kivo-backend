package com.kivo.backend.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.kivo.backend.testsupport.PostgreSqlIntegrationTest;
import java.net.URI;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "kivo.navigation.routes.overlay-max-dimension=256")
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class DemoProfileIntegrationTest extends PostgreSqlIntegrationTest {

  private static final UUID GROCERY_PLACE_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000401");

  @Autowired private MockMvc mockMvc;

  @Test
  void startsWithPublishedGraphAndRunsNavigationCoreDemoWithoutTestFixtures() throws Exception {
    mockMvc
        .perform(get("/api/v1/kiosks/demo-kiosk/bootstrap").queryParam("lang", "en"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.mallCode").value("demo-mall"))
        .andExpect(jsonPath("$.selectedLanguage").value("en"))
        .andExpect(jsonPath("$.currentFloorCode").value("L1"))
        .andExpect(jsonPath("$.floors[0].mapImageUrl").value("/demo/maps/L1.svg"));

    mockMvc
        .perform(get("/api/v1/malls/demo-mall/places").queryParam("lang", "ru"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));

    mockMvc
        .perform(
            get("/api/v1/malls/demo-mall/places")
                .queryParam("q", "Grocery")
                .queryParam("lang", "en"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(GROCERY_PLACE_ID.toString()))
        .andExpect(jsonPath("$[0].name").value("Grocery store"));

    String response =
        mockMvc
            .perform(
                post("/api/v1/routes")
                    .contentType("application/json")
                    .content(
                        """
                        {"kioskCode":"demo-kiosk","placeId":"%s"}
                        """
                            .formatted(GROCERY_PLACE_ID)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.profile").value("STANDARD"))
            .andExpect(jsonPath("$.totalDistanceMeters").value(63.0))
            .andExpect(jsonPath("$.overlays.length()").value(1))
            .andExpect(jsonPath("$.overlays[0].floorCode").value("L1"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID routeId =
        UUID.fromString(JsonMapper.builder().build().readTree(response).path("routeId").asText());

    mockMvc
        .perform(get("/api/v1/routes/{routeId}", routeId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.routeId").value(routeId.toString()));

    byte[] png =
        mockMvc
            .perform(get("/api/v1/routes/{routeId}/floors/L1/overlay.png", routeId))
            .andExpect(status().isOk())
            .andExpect(content().contentType("image/png"))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    assertThat(png).startsWith((byte) 0x89, (byte) 0x50, (byte) 0x4e, (byte) 0x47);

    String shareResponse =
        mockMvc
            .perform(post("/api/v1/routes/{routeId}/share", routeId))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.shareId").isString())
            .andExpect(jsonPath("$.routeUrl").isString())
            .andExpect(jsonPath("$.qrImageUrl").isString())
            .andReturn()
            .getResponse()
            .getContentAsString();
    URI routeUrl =
        URI.create(JsonMapper.builder().build().readTree(shareResponse).path("routeUrl").asText());
    String shareToken = routeUrl.getPath().substring(routeUrl.getPath().lastIndexOf('/') + 1);

    mockMvc
        .perform(get("/api/v1/public/routes/{shareToken}", shareToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.routeId").value(routeId.toString()))
        .andExpect(jsonPath("$.mallId").value("00000000-0000-0000-0000-000000000001"))
        .andExpect(jsonPath("$.overlays[0].mapImageUrl").value("/demo/maps/L1.svg"));

    byte[] qrPng =
        mockMvc
            .perform(get("/api/v1/public/routes/{shareToken}/qr.png", shareToken))
            .andExpect(status().isOk())
            .andExpect(content().contentType("image/png"))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    assertThat(qrPng).startsWith((byte) 0x89, (byte) 0x50, (byte) 0x4e, (byte) 0x47);

    mockMvc
        .perform(get("/demo/maps/L1.svg"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("image/svg+xml"));
  }
}
