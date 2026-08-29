package com.kivo.backend.feedback.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kivo.backend.navigation.application.GraphPublicationService;
import com.kivo.backend.testsupport.MutableClock;
import com.kivo.backend.testsupport.PostgreSqlIntegrationTest;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "kivo.sharing.public-base-url=https://routes.example")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(FeedbackE2eIntegrationTest.ClockConfiguration.class)
@Sql(
    scripts = {"/sql/category-test-data.sql", "/sql/navigation-test-data.sql"},
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FeedbackE2eIntegrationTest extends PostgreSqlIntegrationTest {

  private static final UUID MALL_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID VERSION_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
  private static final UUID PLACE_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");
  private static final UUID KIOSK_ID = UUID.fromString("00000000-0000-0000-0000-000000000601");
  private static final Instant NOW = Instant.parse("2026-08-10T10:00:00Z");

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private GraphPublicationService graphPublicationService;
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
  void bootstrapRouteShareAndBothAnonymousFeedbackContextsPersistEndToEnd() throws Exception {
    UUID routeId = createRoute();

    mockMvc
        .perform(
            post("/api/v1/feedback")
                .contentType("application/json")
                .content("{\"routeId\":\"%s\",\"rating\":5}".formatted(routeId)))
        .andExpect(status().isNoContent());

    JsonNode share =
        objectMapper.readTree(
            mockMvc
                .perform(post("/api/v1/routes/{routeId}/share", routeId))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
    UUID shareId = UUID.fromString(share.path("shareId").asText());
    assertThat(URI.create(share.path("routeUrl").asText()).isAbsolute()).isTrue();

    mockMvc
        .perform(
            post("/api/v1/feedback")
                .contentType("application/json")
                .content("{\"shareId\":\"%s\",\"rating\":4}".formatted(shareId)))
        .andExpect(status().isNoContent());

    assertThat(
            jdbcTemplate.queryForList(
                """
                select mall_id, kiosk_id, place_id, route_id, share_id, rating
                from feedback
                order by rating desc
                """))
        .hasSize(2)
        .allSatisfy(
            row -> {
              assertThat(row.get("mall_id")).isEqualTo(MALL_ID);
              assertThat(row.get("kiosk_id")).isEqualTo(KIOSK_ID);
              assertThat(row.get("place_id")).isEqualTo(PLACE_ID);
              assertThat(row.keySet()).doesNotContain("comment", "email", "name");
            });
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from feedback where route_id = ? and share_id is null",
                Integer.class,
                routeId))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from feedback where share_id = ? and route_id is null",
                Integer.class,
                shareId))
        .isEqualTo(1);
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
            .getContentAsString();
    return UUID.fromString(objectMapper.readTree(response).path("routeId").asText());
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class ClockConfiguration {

    @Bean
    @Primary
    MutableClock mutableClock() {
      return new MutableClock(NOW, ZoneOffset.UTC);
    }
  }
}
