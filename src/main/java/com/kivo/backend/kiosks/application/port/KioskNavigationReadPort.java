package com.kivo.backend.kiosks.application.port;

import java.util.List;
import java.util.UUID;

public interface KioskNavigationReadPort {

  List<KioskStartReadModel> findEnabledKioskStarts(UUID mallId);
}
