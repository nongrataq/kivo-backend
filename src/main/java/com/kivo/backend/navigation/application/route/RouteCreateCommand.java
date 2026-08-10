package com.kivo.backend.navigation.application.route;

import com.kivo.backend.navigation.domain.routing.MovementProfile;
import java.util.UUID;

public record RouteCreateCommand(String kioskCode, UUID placeId, MovementProfile profile) {}
