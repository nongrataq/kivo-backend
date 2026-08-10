package com.kivo.backend.catalog.application.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatalogNavigationReadPort {

  List<RoutablePlaceReadModel> findActiveRoutablePlaces(UUID mallId);

  Optional<RouteDestinationReadModel> findActiveRouteDestination(UUID placeId);

  List<RouteFloorReadModel> findActiveRouteFloors(UUID mallId);
}
