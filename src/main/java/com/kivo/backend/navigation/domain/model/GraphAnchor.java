package com.kivo.backend.navigation.domain.model;

import java.util.Objects;

public record GraphAnchor(String resourceCode, String nodeCode) {

  public GraphAnchor {
    Objects.requireNonNull(resourceCode, "resourceCode");
  }
}
