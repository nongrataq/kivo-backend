package com.kivo.backend.navigation.api;

import com.kivo.backend.navigation.domain.model.MovementType;
import com.kivo.backend.navigation.domain.routing.InstructionType;
import java.math.BigDecimal;

public record RouteInstructionResponse(
    int sequence,
    InstructionType type,
    String floorCode,
    String targetFloorCode,
    MovementType movementType,
    BigDecimal distanceMeters) {}
