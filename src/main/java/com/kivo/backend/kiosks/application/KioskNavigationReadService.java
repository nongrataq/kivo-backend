package com.kivo.backend.kiosks.application;

import com.kivo.backend.kiosks.application.port.KioskNavigationReadPort;
import com.kivo.backend.kiosks.application.port.KioskStartReadModel;
import com.kivo.backend.kiosks.application.port.RouteStartReadModel;
import com.kivo.backend.kiosks.persistence.repository.KioskRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KioskNavigationReadService implements KioskNavigationReadPort {

  private final KioskRepository kioskRepository;

  @Override
  @Transactional(readOnly = true)
  public List<KioskStartReadModel> findEnabledKioskStarts(UUID mallId) {
    return kioskRepository.findEnabledStarts(mallId);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<RouteStartReadModel> findActiveRouteStart(String kioskCode) {
    return kioskRepository
        .findActiveByCode(kioskCode)
        .map(
            kiosk ->
                new RouteStartReadModel(
                    kiosk.getMall().getId(), kiosk.getCode(), kiosk.getStartNavigationNodeCode()));
  }
}
