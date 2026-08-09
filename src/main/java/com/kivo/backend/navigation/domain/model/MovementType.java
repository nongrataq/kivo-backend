package com.kivo.backend.navigation.domain.model;

public enum MovementType {
  CORRIDOR,
  STAIRS,
  ESCALATOR,
  ELEVATOR;

  public boolean isFloorTransition() {
    return this != CORRIDOR;
  }
}
