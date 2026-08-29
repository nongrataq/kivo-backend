package com.kivo.backend.catalog.application;

import com.kivo.backend.catalog.application.port.EventCatalogReadPort;
import com.kivo.backend.catalog.application.port.EventMallReadModel;
import com.kivo.backend.catalog.application.port.EventPlaceReadModel;
import com.kivo.backend.catalog.application.port.FeedbackPlaceReadModel;
import com.kivo.backend.catalog.application.port.FeedbackPlaceReadPort;
import com.kivo.backend.catalog.persistence.entity.Place;
import com.kivo.backend.catalog.persistence.entity.PlaceTranslation;
import com.kivo.backend.catalog.persistence.repository.MallRepository;
import com.kivo.backend.catalog.persistence.repository.PlaceRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventCatalogReadService implements EventCatalogReadPort, FeedbackPlaceReadPort {

  private final MallRepository mallRepository;
  private final PlaceRepository placeRepository;

  @Override
  @Transactional(readOnly = true)
  public Optional<EventMallReadModel> findActiveEventMall(String mallCode) {
    return mallRepository
        .findByCodeAndActiveTrue(mallCode)
        .map(
            mall ->
                new EventMallReadModel(
                    mall.getId(), mall.getCode(), mall.getTimezone(), mall.getDefaultLanguage()));
  }

  @Override
  @Transactional(readOnly = true)
  public List<EventPlaceReadModel> findActiveRoutableEventPlaces(
      UUID mallId, String requestedLanguage, String fallbackLanguage) {
    return placeRepository.findActiveRoutableWithDetailsByMallId(mallId).stream()
        .map(
            place ->
                new EventPlaceReadModel(
                    place.getId(),
                    place.getMall().getId(),
                    place.getCode(),
                    placeName(place, requestedLanguage, fallbackLanguage),
                    place.getFloor().getCode()))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<FeedbackPlaceReadModel> findActiveFeedbackPlace(UUID placeId) {
    return placeRepository
        .findActiveWithDetailsById(placeId)
        .filter(place -> place.getNavigationNodeCode() != null)
        .filter(place -> !place.getNavigationNodeCode().isBlank())
        .map(place -> new FeedbackPlaceReadModel(place.getId(), place.getMall().getId()));
  }

  private String placeName(Place place, String requestedLanguage, String fallbackLanguage) {
    return findTranslation(place, requestedLanguage)
        .or(() -> findTranslation(place, fallbackLanguage))
        .map(PlaceTranslation::getName)
        .orElse(place.getCode());
  }

  private Optional<PlaceTranslation> findTranslation(Place place, String language) {
    return place.getTranslations().stream()
        .filter(translation -> translation.getLanguage().equalsIgnoreCase(language))
        .findFirst();
  }
}
