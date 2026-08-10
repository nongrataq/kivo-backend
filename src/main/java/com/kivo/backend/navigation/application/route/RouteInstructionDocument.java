package com.kivo.backend.navigation.application.route;

import com.kivo.backend.navigation.domain.model.MovementType;
import com.kivo.backend.navigation.domain.routing.InstructionType;
import java.math.BigDecimal;

public record RouteInstructionDocument(
    int sequence,
    InstructionType type,
    String floorCode,
    String targetFloorCode,
    MovementType movementType,
    BigDecimal distanceMeters) {}
