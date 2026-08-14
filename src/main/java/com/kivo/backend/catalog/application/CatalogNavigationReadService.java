package com.kivo.backend.catalog.application;

import com.kivo.backend.catalog.application.port.CatalogNavigationReadPort;
import com.kivo.backend.catalog.application.port.RoutablePlaceReadModel;
import com.kivo.backend.catalog.application.port.RouteDestinationReadModel;
import com.kivo.backend.catalog.application.port.RouteFloorReadModel;
import com.kivo.backend.catalog.persistence.repository.FloorRepository;
import com.kivo.backend.catalog.persistence.repository.PlaceRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CatalogNavigationReadService implements CatalogNavigationReadPort {

  private final PlaceRepository placeRepository;
  private final FloorRepository floorRepository;

  @Override
  @Transactional(readOnly = true)
  public List<RoutablePlaceReadModel> findActiveRoutablePlaces(UUID mallId) {
    return placeRepository.findActiveRoutablePlaces(mallId);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<RouteDestinationReadModel> findActiveRouteDestination(UUID placeId) {
    return placeRepository
        .findActiveWithDetailsById(placeId)
        .map(
            place ->
                new RouteDestinationReadModel(
                    place.getId(), place.getMall().getId(), place.getNavigationNodeCode()));
  }

  @Override
  @Transactional(readOnly = true)
  public List<RouteFloorReadModel> findActiveRouteFloors(UUID mallId) {
    return floorRepository.findByMallIdAndActiveTrueOrderByDisplayOrderAscCodeAsc(mallId).stream()
        .map(
            floor ->
                new RouteFloorReadModel(
                    floor.getId(),
                    floor.getCode(),
                    floor.getDisplayOrder(),
                    floor.getMapUrl(),
                    floor.getCoordinateWidth(),
                    floor.getCoordinateHeight()))
        .toList();
  }
}
