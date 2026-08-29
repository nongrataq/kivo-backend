package com.kivo.backend.events.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kivo.backend.testsupport.MutableClock;
import com.kivo.backend.testsupport.PostgreSqlIntegrationTest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(EventApiIntegrationTest.ClockConfiguration.class)
@Sql(scripts = "/sql/category-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Sql(scripts = "/sql/events-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EventApiIntegrationTest extends PostgreSqlIntegrationTest {

  private static final Instant NOW = Instant.parse("2026-08-10T10:00:00Z");
  private static final UUID MALL_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID PLACE_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private MutableClock clock;

  @BeforeEach
  void resetClock() {
    clock.set(NOW);
  }

  @Test
  void appliesLocalMidnightPublicationWindowOrderingAndLocalizationFallback() throws Exception {
    mockMvc
        .perform(get("/api/v1/malls/demo-mall/events/today").queryParam("lang", "en"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].code").value("midnight-overlap"))
        .andExpect(jsonPath("$[0].title").value("Night offer"))
        .andExpect(jsonPath("$[0].startsAt").value("2026-08-09T20:30:00Z"))
        .andExpect(jsonPath("$[1].code").value("tasting"))
        .andExpect(jsonPath("$[1].title").value("Дегустация"))
        .andExpect(jsonPath("$[1].placeName").value("Grocery store"))
        .andExpect(jsonPath("$[1].floorCode").value("L1"));
  }

  @Test
  void limitsResponseToFiftyEvents() throws Exception {
    for (int index = 0; index < 55; index++) {
      UUID eventId = new UUID(0x7600000000000000L, index + 1L);
      jdbcTemplate.update(
          """
          insert into event
            (id, mall_id, place_id, code, starts_at, ends_at, publication_state,
             publication_starts_at, display_order)
          values (?, ?, ?, ?, ?, ?, 'PUBLISHED', ?, ?)
          """,
          eventId,
          MALL_ID,
          PLACE_ID,
          "bulk-" + index,
          java.sql.Timestamp.from(NOW.plusSeconds(60L + index)),
          java.sql.Timestamp.from(NOW.plusSeconds(3600L + index)),
          java.sql.Timestamp.from(NOW.minusSeconds(60)),
          index);
    }

    String response =
        mockMvc
            .perform(get("/api/v1/malls/demo-mall/events/today"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(tools.jackson.databind.json.JsonMapper.builder().build().readTree(response).size())
        .isEqualTo(50);
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
