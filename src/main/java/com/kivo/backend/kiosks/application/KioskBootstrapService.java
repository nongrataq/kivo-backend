package com.kivo.backend.kiosks.application;

import com.kivo.backend.catalog.application.port.CatalogBootstrapReadModel;
import com.kivo.backend.catalog.application.port.CatalogReadPort;
import com.kivo.backend.common.exception.service.KioskNotFoundException;
import com.kivo.backend.kiosks.application.query.KioskBootstrapResponse;
import com.kivo.backend.kiosks.application.query.KioskBootstrapResponse.FloorResponse;
import com.kivo.backend.kiosks.application.query.KioskBootstrapResponse.LanguageResponse;
import com.kivo.backend.kiosks.persistence.repository.KioskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KioskBootstrapService {

  private final KioskRepository kioskRepository;
  private final CatalogReadPort catalogReadPort;

  @Transactional(readOnly = true)
  public KioskBootstrapResponse getBootstrap(String kioskCode, String requestedLanguage) {
    var kiosk =
        kioskRepository
            .findActiveByCode(kioskCode)
            .orElseThrow(() -> new KioskNotFoundException(kioskCode));
    CatalogBootstrapReadModel catalog =
        catalogReadPort
            .findActiveCatalog(kiosk.getMall().getId(), requestedLanguage)
            .orElseThrow(() -> new KioskNotFoundException(kioskCode));

    return new KioskBootstrapResponse(
        kiosk.getCode(),
        catalog.mallCode(),
        catalog.mallName(),
        catalog.defaultLanguage(),
        catalog.selectedLanguage(),
        kiosk.getFloor().getCode(),
        catalog.languages().stream()
            .map(
                language ->
                    new LanguageResponse(
                        language.code(), language.displayLabel(), language.displayOrder()))
            .toList(),
        catalog.floors().stream()
            .map(
                floor ->
                    new FloorResponse(
                        floor.code(),
                        floor.name(),
                        floor.levelNumber(),
                        floor.displayOrder(),
                        floor.mapImageUrl(),
                        floor.coordinateWidth(),
                        floor.coordinateHeight()))
            .toList());
  }
}
