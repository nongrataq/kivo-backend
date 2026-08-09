package com.kivo.backend.navigation.application;

import java.util.UUID;

public final class ActiveGraphNotFoundException extends RuntimeException {

  public ActiveGraphNotFoundException(UUID mallId) {
    super("No active navigation graph is loaded for mall " + mallId);
  }
}
