package com.kivo.backend.kiosks.application.port;

import java.util.Optional;

public interface FeedbackKioskReadPort {

  Optional<FeedbackKioskReadModel> findActiveFeedbackKiosk(String kioskCode);
}
