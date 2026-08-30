package com.kivo.backend.catalog.application;

import com.kivo.backend.catalog.application.port.CatalogBootstrapReadModel;
import com.kivo.backend.catalog.application.port.CatalogFloorReadModel;
import com.kivo.backend.catalog.application.port.CatalogLanguageReadModel;
import com.kivo.backend.catalog.application.port.CatalogReadPort;
import com.kivo.backend.catalog.application.port.MallIdentityReadPort;
import com.kivo.backend.catalog.persistence.entity.MallLanguage;
import com.kivo.backend.catalog.persistence.repository.FloorRepository;
import com.kivo.backend.catalog.persistence.repository.MallRepository;
import com.kivo.backend.common.localization.LanguageResolver;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CatalogReadService implements CatalogReadPort, MallIdentityReadPort {

  private final MallRepository mallRepository;
  private final FloorRepository floorRepository;

  @Override
  @Transactional(readOnly = true)
  public Optional<UUID> findActiveMallIdByCode(String mallCode) {
    return mallRepository.findByCodeAndActiveTrue(mallCode).map(mall -> mall.getId());
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<CatalogBootstrapReadModel> findActiveCatalog(
      UUID mallId, String requestedLanguage) {
    return mallRepository
        .findByIdAndActiveTrue(mallId)
        .map(
            mall ->
                new CatalogBootstrapReadModel(
                    mall.getCode(),
                    mall.getName(),
                    mall.getDefaultLanguage(),
                    LanguageResolver.resolve(requestedLanguage, mall.getDefaultLanguage()),
                    mall.getLanguages().stream()
                        .filter(MallLanguage::isActive)
                        .sorted(
                            Comparator.comparingInt(MallLanguage::getDisplayOrder)
                                .thenComparing(MallLanguage::getLanguage))
                        .map(
                            language ->
                                new CatalogLanguageReadModel(
                                    language.getLanguage(),
                                    language.getDisplayLabel(),
                                    language.getDisplayOrder()))
                        .toList(),
                    floorRepository
                        .findByMallIdAndActiveTrueOrderByDisplayOrderAscCodeAsc(mallId)
                        .stream()
                        .map(
                            floor ->
                                new CatalogFloorReadModel(
                                    floor.getCode(),
                                    floor.getName(),
                                    floor.getLevelNumber(),
                                    floor.getDisplayOrder(),
                                    floor.getMapUrl(),
                                    floor.getCoordinateWidth(),
                                    floor.getCoordinateHeight()))
                        .toList()));
  }
}
