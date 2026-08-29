package com.kivo.backend.events.application;

import com.kivo.backend.catalog.application.port.EventCatalogReadPort;
import com.kivo.backend.catalog.application.port.EventPlaceReadModel;
import com.kivo.backend.common.exception.service.MallNotFoundException;
import com.kivo.backend.common.localization.LanguageResolver;
import com.kivo.backend.events.persistence.entity.Event;
import com.kivo.backend.events.persistence.entity.EventTranslation;
import com.kivo.backend.events.persistence.repository.EventRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventQueryService {

  static final int MAX_RESULTS = 50;

  private final EventCatalogReadPort eventCatalogReadPort;
  private final EventRepository eventRepository;
  private final Clock clock;

  @Transactional(readOnly = true)
  public List<EventResponse> findToday(String mallCode, String requestedLanguage) {
    var mall =
        eventCatalogReadPort
            .findActiveEventMall(mallCode)
            .orElseThrow(() -> new MallNotFoundException(mallCode));
    String language = LanguageResolver.resolve(requestedLanguage, mall.defaultLanguage());
    Map<UUID, EventPlaceReadModel> places =
        eventCatalogReadPort
            .findActiveRoutableEventPlaces(mall.mallId(), language, mall.defaultLanguage())
            .stream()
            .collect(Collectors.toMap(EventPlaceReadModel::placeId, Function.identity()));
    if (places.isEmpty()) {
      return List.of();
    }

    Instant now = clock.instant();
    ZoneId zone = ZoneId.of(mall.timezone());
    LocalDate today = now.atZone(zone).toLocalDate();
    Instant dayStart = today.atStartOfDay(zone).toInstant();
    Instant dayEnd = today.plusDays(1).atStartOfDay(zone).toInstant();
    List<UUID> eventIds =
        eventRepository.findPublishedTodayIds(
            mall.mallId(), places.keySet(), dayStart, dayEnd, now, PageRequest.of(0, MAX_RESULTS));
    if (eventIds.isEmpty()) {
      return List.of();
    }

    Map<UUID, Event> events =
        eventRepository.findAllWithTranslationsByIdIn(eventIds).stream()
            .collect(
                Collectors.toMap(
                    Event::getId, Function.identity(), (left, right) -> left, HashMap::new));
    return eventIds.stream()
        .map(events::get)
        .filter(java.util.Objects::nonNull)
        .map(
            event ->
                toResponse(event, places.get(event.getPlaceId()), language, mall.defaultLanguage()))
        .toList();
  }

  private EventResponse toResponse(
      Event event, EventPlaceReadModel place, String language, String fallbackLanguage) {
    Optional<EventTranslation> translation =
        findTranslation(event, language).or(() -> findTranslation(event, fallbackLanguage));
    return new EventResponse(
        event.getId(),
        event.getCode(),
        translation.map(EventTranslation::getTitle).orElse(event.getCode()),
        translation.map(EventTranslation::getDescription).orElse(null),
        event.getStartsAt(),
        event.getEndsAt(),
        place.placeId(),
        place.placeCode(),
        place.placeName(),
        place.floorCode());
  }

  private Optional<EventTranslation> findTranslation(Event event, String language) {
    return event.getTranslations().stream()
        .filter(translation -> translation.getLanguage().equalsIgnoreCase(language))
        .findFirst();
  }
}
