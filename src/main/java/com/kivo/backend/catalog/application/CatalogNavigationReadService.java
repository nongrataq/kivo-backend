package com.kivo.backend.catalog.application;

import com.kivo.backend.catalog.application.port.CatalogNavigationReadPort;
import com.kivo.backend.catalog.application.port.RoutablePlaceReadModel;
import com.kivo.backend.catalog.persistence.repository.PlaceRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CatalogNavigationReadService implements CatalogNavigationReadPort {

  private final PlaceRepository placeRepository;

  @Override
  @Transactional(readOnly = true)
  public List<RoutablePlaceReadModel> findActiveRoutablePlaces(UUID mallId) {
    return placeRepository.findActiveRoutablePlaces(mallId);
  }
}
