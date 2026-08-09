package com.kivo.backend.navigation.application;

import com.kivo.backend.navigation.domain.model.GraphValidationResult;

public final class GraphValidationException extends RuntimeException {

  private final GraphValidationResult validationResult;

  public GraphValidationException(GraphValidationResult validationResult) {
    super(
        "Navigation graph validation failed with %d issue(s)"
            .formatted(validationResult.issues().size()));
    this.validationResult = validationResult;
  }

  public GraphValidationResult getValidationResult() {
    return validationResult;
  }
}
