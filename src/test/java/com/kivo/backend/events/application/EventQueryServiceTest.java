package com.kivo.backend.events.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kivo.backend.catalog.application.port.EventCatalogReadPort;
import com.kivo.backend.catalog.application.port.EventMallReadModel;
import com.kivo.backend.catalog.application.port.EventPlaceReadModel;
import com.kivo.backend.events.persistence.entity.Event;
import com.kivo.backend.events.persistence.entity.EventPublicationState;
import com.kivo.backend.events.persistence.entity.EventTranslation;
import com.kivo.backend.events.persistence.repository.EventRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class EventQueryServiceTest {

  private static final UUID MALL_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID PLACE_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");
  private static final UUID EVENT_ID = UUID.fromString("75000000-0000-0000-0000-000000000001");

  @Mock private EventCatalogReadPort catalog;
  @Mock private EventRepository repository;

  @Test
  void computesTwentyFiveHourDstDayAndFallsBackToMallLanguage() {
    Instant now = Instant.parse("2026-10-25T12:00:00Z");
    EventQueryService service = service(now);
    when(catalog.findActiveEventMall("berlin-mall"))
        .thenReturn(
            Optional.of(new EventMallReadModel(MALL_ID, "berlin-mall", "Europe/Berlin", "ru")));
    when(catalog.findActiveRoutableEventPlaces(MALL_ID, "en", "ru"))
        .thenReturn(
            List.of(new EventPlaceReadModel(PLACE_ID, MALL_ID, "grocery", "Grocery store", "L1")));
    when(repository.findPublishedTodayIds(eq(MALL_ID), any(), any(), any(), eq(now), any()))
        .thenReturn(List.of(EVENT_ID));
    Event event = event(now);
    event.addTranslation(EventTranslation.create("ru", "Дегустация", "Описание"));
    when(repository.findAllWithTranslationsByIdIn(List.of(EVENT_ID))).thenReturn(List.of(event));

    List<EventResponse> result = service.findToday("berlin-mall", "en");

    assertThat(result)
        .singleElement()
        .satisfies(
            response -> {
              assertThat(response.title()).isEqualTo("Дегустация");
              assertThat(response.placeName()).isEqualTo("Grocery store");
            });
    ArgumentCaptor<Instant> start = ArgumentCaptor.forClass(Instant.class);
    ArgumentCaptor<Instant> end = ArgumentCaptor.forClass(Instant.class);
    verify(repository)
        .findPublishedTodayIds(
            eq(MALL_ID), any(), start.capture(), end.capture(), eq(now), any(Pageable.class));
    assertThat(start.getValue()).isEqualTo(Instant.parse("2026-10-24T22:00:00Z"));
    assertThat(end.getValue()).isEqualTo(Instant.parse("2026-10-25T23:00:00Z"));
  }

  @Test
  void switchesLocalDayAtMallMidnight() {
    Instant now = Instant.parse("2026-08-09T21:00:00Z");
    EventQueryService service = service(now);
    when(catalog.findActiveEventMall("demo-mall"))
        .thenReturn(
            Optional.of(new EventMallReadModel(MALL_ID, "demo-mall", "Europe/Moscow", "ru")));
    when(catalog.findActiveRoutableEventPlaces(MALL_ID, "ru", "ru"))
        .thenReturn(
            List.of(new EventPlaceReadModel(PLACE_ID, MALL_ID, "grocery", "Магазин", "L1")));
    when(repository.findPublishedTodayIds(eq(MALL_ID), any(), any(), any(), eq(now), any()))
        .thenReturn(List.of());

    service.findToday("demo-mall", null);

    ArgumentCaptor<Instant> start = ArgumentCaptor.forClass(Instant.class);
    ArgumentCaptor<Instant> end = ArgumentCaptor.forClass(Instant.class);
    verify(repository)
        .findPublishedTodayIds(
            eq(MALL_ID), any(), start.capture(), end.capture(), eq(now), any(Pageable.class));
    assertThat(start.getValue()).isEqualTo(now);
    assertThat(end.getValue()).isEqualTo(Instant.parse("2026-08-10T21:00:00Z"));
  }

  private EventQueryService service(Instant now) {
    return new EventQueryService(catalog, repository, Clock.fixed(now, ZoneOffset.UTC));
  }

  private Event event(Instant now) {
    Event event =
        Event.create(
            MALL_ID,
            PLACE_ID,
            "tasting",
            now.plusSeconds(60),
            now.plusSeconds(3600),
            EventPublicationState.PUBLISHED,
            now.minusSeconds(60),
            null,
            10);
    try {
      var field =
          com.kivo.backend.common.persistence.AbstractUuidEntity.class.getDeclaredField("id");
      field.setAccessible(true);
      field.set(event, EVENT_ID);
      return event;
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
