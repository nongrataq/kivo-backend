package com.kivo.backend.events.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kivo.backend.events.persistence.entity.Event;
import com.kivo.backend.events.persistence.entity.EventPublicationState;
import com.kivo.backend.events.persistence.entity.EventTranslation;
import com.kivo.backend.events.persistence.repository.EventRepository;
import com.kivo.backend.feedback.persistence.entity.Feedback;
import com.kivo.backend.feedback.persistence.repository.FeedbackRepository;
import com.kivo.backend.testsupport.PostgreSqlIntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Sql("/sql/category-test-data.sql")
class EventsFeedbackPersistenceIntegrationTest extends PostgreSqlIntegrationTest {

  private static final UUID MALL_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID PLACE_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");
  private static final UUID KIOSK_ID = UUID.fromString("00000000-0000-0000-0000-000000000601");
  private static final UUID ROUTE_ID = UUID.fromString("73000000-0000-0000-0000-000000000001");
  private static final Instant NOW = Instant.parse("2026-08-10T10:00:00Z");

  @Autowired private EventRepository eventRepository;
  @Autowired private FeedbackRepository feedbackRepository;
  @Autowired private EntityManager entityManager;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void persistsEventTranslationAndAnonymousRouteFeedback() {
    Event event =
        Event.create(
            MALL_ID,
            PLACE_ID,
            "tasting",
            NOW,
            NOW.plusSeconds(3600),
            EventPublicationState.PUBLISHED,
            NOW.minusSeconds(60),
            null,
            10);
    event.addTranslation(EventTranslation.create("ru", "Дегустация", null));
    eventRepository.saveAndFlush(event);
    Feedback feedback =
        feedbackRepository.saveAndFlush(
            Feedback.forRoute(MALL_ID, KIOSK_ID, PLACE_ID, ROUTE_ID, 5, NOW));
    entityManager.clear();

    assertThat(eventRepository.findAllWithTranslationsByIdIn(java.util.List.of(event.getId())))
        .singleElement()
        .satisfies(loaded -> assertThat(loaded.getTranslations()).singleElement());
    assertThat(feedbackRepository.findById(feedback.getId()))
        .get()
        .satisfies(
            loaded -> {
              assertThat(loaded.getRating()).isEqualTo(5);
              assertThat(loaded.getRouteId()).isEqualTo(ROUTE_ID);
              assertThat(loaded.getShareId()).isNull();
            });
  }

  @Test
  void databaseRejectsCrossMallEvent() {
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    insert into event
                      (id, mall_id, place_id, code, starts_at, ends_at, publication_state,
                       publication_starts_at, display_order)
                    values (?, ?, ?, 'cross-mall', ?, ?, 'PUBLISHED', ?, 0)
                    """,
                    UUID.randomUUID(),
                    UUID.fromString("00000000-0000-0000-0000-000000000800"),
                    PLACE_ID,
                    java.sql.Timestamp.from(NOW),
                    java.sql.Timestamp.from(NOW.plusSeconds(60)),
                    java.sql.Timestamp.from(NOW.minusSeconds(60))))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void databaseRejectsInvalidRating() {
    assertThatThrownBy(() -> insertFeedback(0, ROUTE_ID, null))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void databaseRejectsMissingContext() {
    assertThatThrownBy(() -> insertFeedback(3, null, null))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private void insertFeedback(int rating, UUID routeId, UUID shareId) {
    jdbcTemplate.update(
        """
        insert into feedback
          (id, mall_id, kiosk_id, place_id, route_id, share_id, rating, created_at)
        values (?, ?, ?, ?, ?, ?, ?, ?)
        """,
        UUID.randomUUID(),
        MALL_ID,
        KIOSK_ID,
        PLACE_ID,
        routeId,
        shareId,
        rating,
        java.sql.Timestamp.from(NOW));
  }
}
