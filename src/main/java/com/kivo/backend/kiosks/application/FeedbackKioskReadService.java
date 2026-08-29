package com.kivo.backend.kiosks.application;

import com.kivo.backend.kiosks.application.port.FeedbackKioskReadModel;
import com.kivo.backend.kiosks.application.port.FeedbackKioskReadPort;
import com.kivo.backend.kiosks.persistence.repository.KioskRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedbackKioskReadService implements FeedbackKioskReadPort {

  private final KioskRepository kioskRepository;

  @Override
  @Transactional(readOnly = true)
  public Optional<FeedbackKioskReadModel> findActiveFeedbackKiosk(String kioskCode) {
    return kioskRepository
        .findActiveByCode(kioskCode)
        .map(
            kiosk ->
                new FeedbackKioskReadModel(
                    kiosk.getId(), kiosk.getMall().getId(), kiosk.getCode()));
  }
}
