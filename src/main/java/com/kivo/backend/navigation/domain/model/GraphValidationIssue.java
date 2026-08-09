package com.kivo.backend.navigation.domain.model;

import java.util.Objects;

public record GraphValidationIssue(GraphValidationCode code, String subjectCode, String message) {

  public GraphValidationIssue {
    Objects.requireNonNull(code, "code");
    Objects.requireNonNull(subjectCode, "subjectCode");
    Objects.requireNonNull(message, "message");
  }
}
