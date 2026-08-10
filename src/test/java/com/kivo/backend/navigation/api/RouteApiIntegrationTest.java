package com.kivo.backend.navigation.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.kivo.backend.navigation.application.GraphPublicationService;
import com.kivo.backend.testsupport.PostgreSqlIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "kivo.navigation.routes.overlay-max-dimension=256")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql({"/sql/category-test-data.sql", "/sql/navigation-test-data.sql"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RouteApiIntegrationTest extends PostgreSqlIntegrationTest {

  private static final UUID MALL_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID VERSION_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
  private static final UUID PLACE_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");

  @Autowired private MockMvc mockMvc;
  @Autowired private GraphPublicationService graphPublicationService;

  @BeforeEach
  void publishGraph() {
    graphPublicationService.publish(MALL_ID, VERSION_ID);
  }

  @Test
  void createGetAndRenderOverlayEndToEnd() throws Exception {
    String response =
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
            .andExpect(jsonPath("$.profile").value("STANDARD"))
            .andExpect(jsonPath("$.totalDistanceMeters").value(20.0))
            .andExpect(jsonPath("$.estimatedDurationSeconds").value(15))
            .andExpect(jsonPath("$.overlays.length()").value(1))
            .andExpect(jsonPath("$.overlays[0].floorCode").value("L1"))
            .andExpect(jsonPath("$.overlays[0].pixelWidth").value(256))
            .andExpect(jsonPath("$..nodeCode").doesNotExist())
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
  }
}
