package com.kivo.backend.navigation.application;

import java.util.UUID;

public final class GraphVersionNotFoundException extends RuntimeException {

  public GraphVersionNotFoundException(UUID mallId, UUID versionId) {
    super("Navigation graph version %s was not found for mall %s".formatted(versionId, mallId));
  }
}
