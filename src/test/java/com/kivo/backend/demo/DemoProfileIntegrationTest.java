package com.kivo.backend.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.kivo.backend.testsupport.PostgreSqlIntegrationTest;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "kivo.navigation.routes.overlay-max-dimension=2048")
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class DemoProfileIntegrationTest extends PostgreSqlIntegrationTest {

  private static final UUID GALLERY_PLACE_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000401");
  private static final UUID TUGANNAR_PLACE_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000415");

  @Autowired private MockMvc mockMvc;

  @Test
  void startsWithPublishedGraphAndRunsNavigationCoreDemoWithoutTestFixtures() throws Exception {
    mockMvc
        .perform(get("/api/v1/kiosks/tsum-l1-main-kiosk/bootstrap").queryParam("lang", "en"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.mallCode").value("tsum-demo"))
        .andExpect(jsonPath("$.selectedLanguage").value("en"))
        .andExpect(jsonPath("$.currentFloorCode").value("L1"))
        .andExpect(jsonPath("$.floors[0].mapImageUrl").value("/demo/maps/tsum-l1.png"))
        .andExpect(jsonPath("$.floors[0].coordinateWidth").value(10000))
        .andExpect(jsonPath("$.floors[0].coordinateHeight").value(8358));

    mockMvc
        .perform(get("/api/v1/malls/tsum-demo/places").queryParam("lang", "ru"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(16));

    mockMvc
        .perform(
            get("/api/v1/malls/tsum-demo/places")
                .queryParam("q", "Gallery")
                .queryParam("lang", "en"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(GALLERY_PLACE_ID.toString()))
        .andExpect(jsonPath("$[0].name").value("Gallery"));

    String response =
        mockMvc
            .perform(
                post("/api/v1/routes")
                    .contentType("application/json")
                    .content(
                        """
                        {"kioskCode":"tsum-l1-main-kiosk","placeId":"%s"}
                        """
                            .formatted(TUGANNAR_PLACE_ID)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.profile").value("STANDARD"))
            .andExpect(jsonPath("$.totalDistanceMeters").isNumber())
            .andExpect(jsonPath("$.overlays.length()").value(1))
            .andExpect(jsonPath("$.overlays[0].floorCode").value("L1"))
            .andExpect(jsonPath("$.overlays[0].pixelWidth").value(2048))
            .andExpect(jsonPath("$.overlays[0].pixelHeight").value(1712))
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID routeId =
        UUID.fromString(JsonMapper.builder().build().readTree(response).path("routeId").asText());

    mockMvc
        .perform(get("/api/v1/routes/{routeId}", routeId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.routeId").value(routeId.toString()));

    byte[] overlayPng =
        mockMvc
            .perform(get("/api/v1/routes/{routeId}/floors/L1/overlay.png", routeId))
            .andExpect(status().isOk())
            .andExpect(content().contentType("image/png"))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    assertThat(overlayPng).startsWith((byte) 0x89, (byte) 0x50, (byte) 0x4e, (byte) 0x47);
    writeVisualQaComposite(overlayPng);

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
        .andExpect(jsonPath("$.overlays[0].mapImageUrl").value("/demo/maps/tsum-l1.png"));

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
        .perform(get("/demo/maps/tsum-l1.png"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("image/png"));
  }

  private static void writeVisualQaComposite(byte[] overlayPng) throws Exception {
    BufferedImage map;
    try (var input = new ClassPathResource("static/demo/maps/tsum-l1.png").getInputStream()) {
      map = ImageIO.read(input);
    }
    BufferedImage overlay = ImageIO.read(new ByteArrayInputStream(overlayPng));
    assertThat(map).isNotNull();
    assertThat(overlay).isNotNull();
    assertThat(overlay.getWidth()).isEqualTo(map.getWidth());
    assertThat(overlay.getHeight()).isEqualTo(map.getHeight());

    BufferedImage composite =
        new BufferedImage(map.getWidth(), map.getHeight(), BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = composite.createGraphics();
    try {
      graphics.drawImage(map, 0, 0, null);
      graphics.drawImage(overlay, 0, 0, null);
    } finally {
      graphics.dispose();
    }

    Path output = Path.of("build", "reports", "visual-qa", "tsum-l1-route-to-tugannar.png");
    Files.createDirectories(output.getParent());
    assertThat(ImageIO.write(composite, "png", output.toFile())).isTrue();
  }
}
