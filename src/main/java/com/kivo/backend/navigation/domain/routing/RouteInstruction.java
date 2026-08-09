package com.kivo.backend.navigation.domain.routing;

import com.kivo.backend.navigation.domain.model.MovementType;
import java.math.BigDecimal;
import java.util.UUID;

public sealed interface RouteInstruction {

  InstructionType type();

  record Start(UUID floorId, String nodeCode) implements RouteInstruction {
    @Override
    public InstructionType type() {
      return InstructionType.START;
    }
  }

  record Continue(
      UUID floorId,
      String fromNodeCode,
      String toNodeCode,
      MovementType movementType,
      BigDecimal distanceMeters)
      implements RouteInstruction {
    @Override
    public InstructionType type() {
      return InstructionType.CONTINUE;
    }
  }

  record Transition(
      UUID fromFloorId,
      UUID toFloorId,
      String fromNodeCode,
      String toNodeCode,
      MovementType movementType)
      implements RouteInstruction {
    @Override
    public InstructionType type() {
      return InstructionType.TRANSITION;
    }
  }

  record Arrive(UUID floorId, String nodeCode) implements RouteInstruction {
    @Override
    public InstructionType type() {
      return InstructionType.ARRIVE;
    }
  }
}
