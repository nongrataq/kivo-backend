package com.kivo.backend.catalog.application.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventCatalogReadPort {

  Optional<EventMallReadModel> findActiveEventMall(String mallCode);

  List<EventPlaceReadModel> findActiveRoutableEventPlaces(
      UUID mallId, String requestedLanguage, String fallbackLanguage);
}
