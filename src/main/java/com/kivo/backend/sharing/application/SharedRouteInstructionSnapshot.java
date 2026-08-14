package com.kivo.backend.sharing.application;

import com.kivo.backend.navigation.domain.model.MovementType;
import com.kivo.backend.navigation.domain.routing.InstructionType;
import java.math.BigDecimal;

public record SharedRouteInstructionSnapshot(
    int sequence,
    InstructionType type,
    String floorCode,
    String targetFloorCode,
    MovementType movementType,
    BigDecimal distanceMeters) {}
