package com.kivo.backend.catalog.api;

import com.kivo.backend.catalog.persistence.entity.Place;
import com.kivo.backend.catalog.persistence.entity.PlaceTranslation;
import java.util.Optional;

public class PlaceMapper {

  public static PlaceResponse toPlaceResponse(
      Place place, String language, String fallbackLanguage) {
    Optional<PlaceTranslation> translation = findTranslation(place, language);
    if (translation.isEmpty()) {
      translation = findTranslation(place, fallbackLanguage);
    }

    String name = translation.map(PlaceTranslation::getName).orElse(place.getCode());
    String description = translation.map(PlaceTranslation::getDescription).orElse(null);
    PlaceResponse placeResponse =
        new PlaceResponse(
            place.getId(),
            place.getCode(),
            name,
            description,
            place.getCategory().getId(),
            place.getFloor().getCode(),
            place.getNavigationNodeCode(),
            place.getDisplayOrder());
    return placeResponse;
  }

  private static Optional<PlaceTranslation> findTranslation(Place place, String language) {
    return place.getTranslations().stream()
        .filter(translation -> translation.getLanguage().equalsIgnoreCase(language))
        .findFirst();
  }
}
