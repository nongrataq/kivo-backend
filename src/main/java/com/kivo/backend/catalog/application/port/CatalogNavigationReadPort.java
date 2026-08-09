package com.kivo.backend.catalog.application.port;

import java.util.List;
import java.util.UUID;

public interface CatalogNavigationReadPort {

  List<RoutablePlaceReadModel> findActiveRoutablePlaces(UUID mallId);
}
