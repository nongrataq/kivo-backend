package com.kivo.backend.sharing.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kivo.backend.navigation.application.GraphPublicationService;
import com.kivo.backend.sharing.application.RouteSharingService;
import com.kivo.backend.sharing.application.ShareRouteResult;
import com.kivo.backend.testsupport.MutableClock;
import com.kivo.backend.testsupport.PostgreSqlIntegrationTest;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
    properties = {
      "kivo.navigation.routes.overlay-max-dimension=256",
      "kivo.sharing.public-base-url=https://routes.example",
      "kivo.sharing.ttl=2h",
      "kivo.sharing.qr-size=256"
    })
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(SharingApiIntegrationTest.ClockConfiguration.class)
@Sql(
    scripts = {"/sql/category-test-data.sql", "/sql/navigation-test-data.sql"},
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SharingApiIntegrationTest extends PostgreSqlIntegrationTest {

  private static final UUID MALL_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID VERSION_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
  private static final UUID PLACE_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");
  private static final Instant NOW = Instant.parse("2026-08-10T10:00:00Z");

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private GraphPublicationService graphPublicationService;
  @Autowired private RouteSharingService routeSharingService;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private MutableClock clock;

  @BeforeAll
  void publishGraph() {
    graphPublicationService.publish(MALL_ID, VERSION_ID);
  }

  @BeforeEach
  void resetClock() {
    clock.set(NOW);
  }

  @Test
  void routeToQrToPublicMobileRouteWorksEndToEndWithoutPersistingTheRawToken() throws Exception {
    UUID routeId = createRoute();

    String shareJson =
        mockMvc
            .perform(post("/api/v1/routes/{routeId}/share", routeId))
            .andExpect(status().isCreated())
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
            .andExpect(jsonPath("$.expiresAt").value("2026-08-10T12:00:00Z"))
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    JsonNode share = objectMapper.readTree(shareJson);
    UUID shareId = UUID.fromString(share.path("shareId").asText());
    URI routeUrl = URI.create(share.path("routeUrl").asText());
    URI qrImageUrl = URI.create(share.path("qrImageUrl").asText());
    String token = token(routeUrl);

    assertThat(routeUrl).isEqualTo(URI.create("https://routes.example/r/" + token));
    assertThat(qrImageUrl)
        .isEqualTo(URI.create("https://routes.example/api/v1/public/routes/" + token + "/qr.png"));
    assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]{43}");
    assertThat(
            jdbcTemplate.queryForObject(
                "select token_hash from shared_route where id = ?", String.class, shareId))
        .matches("[0-9a-f]{64}")
        .isNotEqualTo(token);
    assertThat(
            jdbcTemplate.queryForObject(
                "select snapshot_json from shared_route where id = ?", String.class, shareId))
        .doesNotContain(token)
        .contains("\"schemaVersion\":1", "\"mapImageUrl\"");

    mockMvc
        .perform(get("/api/v1/public/routes/{shareToken}", token))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
        .andExpect(jsonPath("$.shareId").value(shareId.toString()))
        .andExpect(jsonPath("$.routeId").value(routeId.toString()))
        .andExpect(jsonPath("$.graphVersionId").value(VERSION_ID.toString()))
        .andExpect(
            jsonPath("$.overlays[0].mapImageUrl")
                .value("https://cdn.example.com/maps/demo-mall/L1.png"))
        .andExpect(
            jsonPath("$.overlays[0].imageUrl")
                .value(
                    "https://routes.example/api/v1/public/routes/"
                        + token
                        + "/floors/L1/overlay.png"))
        .andExpect(jsonPath("$..token").doesNotExist())
        .andExpect(jsonPath("$..geometry").doesNotExist());

    byte[] qrPng =
        mockMvc
            .perform(get("/api/v1/public/routes/{shareToken}/qr.png", token))
            .andExpect(status().isOk())
            .andExpect(content().contentType("image/png"))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    assertPng(qrPng);
    BufferedImage qrImage = ImageIO.read(new ByteArrayInputStream(qrPng));
    assertThat(qrImage.getWidth()).isEqualTo(256);
    assertThat(qrImage.getHeight()).isEqualTo(256);

    byte[] overlayPng =
        mockMvc
            .perform(get("/api/v1/public/routes/{shareToken}/floors/L1/overlay.png", token))
            .andExpect(status().isOk())
            .andExpect(content().contentType("image/png"))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    assertPng(overlayPng);
    assertThat(ImageIO.read(new ByteArrayInputStream(overlayPng)).getColorModel().hasAlpha())
        .isTrue();

    mockMvc
        .perform(get("/r/{shareToken}", token))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("text/html"))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Ваш маршрут")));
  }

  @Test
  void sharedRouteExpiresAtTheExactBoundary() throws Exception {
    ShareData share = share(createRoute());

    clock.advance(Duration.ofHours(2));

    mockMvc
        .perform(get("/api/v1/public/routes/{shareToken}", share.token()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("SHARED_ROUTE_NOT_FOUND"));
    mockMvc
        .perform(get("/r/{shareToken}", share.token()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("SHARED_ROUTE_NOT_FOUND"));
  }

  @Test
  void revokedRouteCannotBeFetchedOrRendered() throws Exception {
    ShareData share = share(createRoute());

    routeSharingService.revoke(share.shareId());

    assertThat(
            jdbcTemplate.queryForObject(
                "select revoked_at is not null from shared_route where id = ?",
                Boolean.class,
                share.shareId()))
        .isTrue();
    mockMvc
        .perform(get("/api/v1/public/routes/{shareToken}", share.token()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("SHARED_ROUTE_NOT_FOUND"));
    mockMvc
        .perform(get("/api/v1/public/routes/{shareToken}/qr.png", share.token()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("SHARED_ROUTE_NOT_FOUND"));
  }

  @Test
  void concurrentSharesProduceIndependentTokensAndRows() throws Exception {
    UUID routeId = createRoute();
    int requestCount = 12;
    List<Callable<ShareRouteResult>> tasks =
        java.util.stream.IntStream.range(0, requestCount)
            .mapToObj(
                ignored -> (Callable<ShareRouteResult>) () -> routeSharingService.share(routeId))
            .toList();
    List<ShareRouteResult> results;

    try (var executor = Executors.newFixedThreadPool(6)) {
      results =
          executor.invokeAll(tasks).stream()
              .map(
                  future -> {
                    try {
                      return future.get();
                    } catch (Exception exception) {
                      throw new IllegalStateException(exception);
                    }
                  })
              .toList();
    }

    assertThat(results).extracting(ShareRouteResult::shareId).doesNotHaveDuplicates();
    assertThat(results).extracting(result -> token(result.routeUrl())).doesNotHaveDuplicates();
    assertThat(new HashSet<>(results.stream().map(ShareRouteResult::routeUrl).toList()))
        .hasSize(requestCount);
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from shared_route where route_id = ?", Integer.class, routeId))
        .isEqualTo(requestCount);
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(distinct token_hash) from shared_route where route_id = ?",
                Integer.class,
                routeId))
        .isEqualTo(requestCount);
  }

  private UUID createRoute() throws Exception {
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
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    return UUID.fromString(objectMapper.readTree(response).path("routeId").asText());
  }

  private ShareData share(UUID routeId) throws Exception {
    String response =
        mockMvc
            .perform(post("/api/v1/routes/{routeId}/share", routeId))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString(StandardCharsets.UTF_8);
    JsonNode json = objectMapper.readTree(response);
    URI routeUrl = URI.create(json.path("routeUrl").asText());
    return new ShareData(UUID.fromString(json.path("shareId").asText()), token(routeUrl));
  }

  private static String token(URI routeUrl) {
    String path = routeUrl.getPath();
    return path.substring(path.lastIndexOf('/') + 1);
  }

  private static void assertPng(byte[] content) {
    assertThat(content).startsWith((byte) 0x89, (byte) 0x50, (byte) 0x4e, (byte) 0x47);
  }

  private record ShareData(UUID shareId, String token) {}

  @TestConfiguration(proxyBeanMethods = false)
  static class ClockConfiguration {

    @Bean
    @Primary
    MutableClock mutableClock() {
      return new MutableClock(NOW, ZoneOffset.UTC);
    }
  }
}
