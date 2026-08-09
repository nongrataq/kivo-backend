package com.kivo.backend.catalog.application;

import com.kivo.backend.catalog.api.PlaceMapper;
import com.kivo.backend.catalog.api.PlaceResponse;
import com.kivo.backend.catalog.persistence.entity.Mall;
import com.kivo.backend.catalog.persistence.repository.MallRepository;
import com.kivo.backend.catalog.persistence.repository.PlaceRepository;
import com.kivo.backend.common.api.ResourceNotFoundException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlaceQueryService {

  private static final int MAX_RESULTS = 100;

  private final MallRepository mallRepository;
  private final PlaceRepository placeRepository;

  public PlaceQueryService(MallRepository mallRepository, PlaceRepository placeRepository) {
    this.mallRepository = mallRepository;
    this.placeRepository = placeRepository;
  }

  @Transactional(readOnly = true)
  public List<PlaceResponse> findPlaces(
      String mallCode, String query, UUID categoryId, String requestedLanguage) {
    Mall mall =
        mallRepository
            .findByCodeAndActiveTrue(mallCode)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "MALL_NOT_FOUND", "Торговый центр не найден: " + mallCode));
    String language = requestedLanguage.toLowerCase(Locale.ROOT);
    String normalizedQuery = normalizeQuery(query);
    List<UUID> placeIds =
        placeRepository.findActiveIdsByMallCodeAndFilters(
            mallCode, categoryId, normalizedQuery, PageRequest.of(0, MAX_RESULTS));

    if (placeIds.isEmpty()) {
      return List.of();
    }

    return placeRepository.findAllWithDetailsByIdIn(placeIds).stream()
        .map(place -> PlaceMapper.toPlaceResponse(place, language, mall.getDefaultLanguage()))
        .toList();
  }

  private String normalizeQuery(String query) {
    if (query == null || query.isBlank()) {
      return null;
    }
    return query.trim();
  }
}
