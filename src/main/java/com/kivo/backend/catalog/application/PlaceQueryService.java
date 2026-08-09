package com.kivo.backend.catalog.application;

import com.kivo.backend.catalog.api.PlaceMapper;
import com.kivo.backend.catalog.api.PlaceResponse;
import com.kivo.backend.catalog.persistence.entity.Mall;
import com.kivo.backend.catalog.persistence.entity.Place;
import com.kivo.backend.catalog.persistence.entity.PlaceTranslation;
import com.kivo.backend.catalog.persistence.repository.MallRepository;
import com.kivo.backend.catalog.persistence.repository.PlaceRepository;
import com.kivo.backend.common.exception.service.MallNotFoundException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaceQueryService {

  static final int MAX_RESULTS = 50;

  private final MallRepository mallRepository;
  private final PlaceRepository placeRepository;
  private final PlaceMapper placeMapper;

  @Transactional(readOnly = true)
  public List<PlaceResponse> findPlaces(
      String mallCode, String query, UUID categoryId, String requestedLanguage) {
    Mall mall =
        mallRepository
            .findByCodeAndActiveTrue(mallCode)
            .orElseThrow(() -> new MallNotFoundException(mallCode));
    String language = requestedLanguage.toLowerCase(Locale.ROOT);
    String normalizedQuery = normalizeQuery(query);
    List<UUID> placeIds =
        placeRepository.searchActiveIds(
            mallCode, normalizedQuery, categoryId, PageRequest.of(0, MAX_RESULTS));

    if (placeIds.isEmpty()) {
      return List.of();
    }

    return placeRepository.findAllWithDetailsByIdIn(placeIds).stream()
        .map(place -> toResponse(place, language, mall.getDefaultLanguage()))
        .toList();
  }

  private PlaceResponse toResponse(Place place, String language, String fallbackLanguage) {
    return findTranslation(place, language)
        .or(() -> findTranslation(place, fallbackLanguage))
        .map(placeMapper::toPlaceResponse)
        .orElseGet(() -> placeMapper.toPlaceResponse(place));
  }

  private Optional<PlaceTranslation> findTranslation(Place place, String language) {
    return place.getTranslations().stream()
        .filter(translation -> translation.getLanguage().equalsIgnoreCase(language))
        .findFirst();
  }

  private String normalizeQuery(String query) {
    if (query == null || query.isBlank()) {
      return null;
    }
    return query.trim();
  }
}
