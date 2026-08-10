package com.kivo.backend.navigation.api;

import com.kivo.backend.navigation.domain.routing.MovementProfile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateRouteRequest(
    @NotBlank @Size(max = 64) String kioskCode, @NotNull UUID placeId, MovementProfile profile) {}
