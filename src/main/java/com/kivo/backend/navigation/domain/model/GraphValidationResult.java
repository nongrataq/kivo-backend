package com.kivo.backend.navigation.domain.model;

import java.util.List;

public record GraphValidationResult(List<GraphValidationIssue> issues) {

  public GraphValidationResult {
    issues = List.copyOf(issues);
  }

  public boolean isValid() {
    return issues.isEmpty();
  }
}
