package com.kivo.backend.events.application;

import java.time.Instant;
import java.util.UUID;

public record EventResponse(
    UUID id,
    String code,
    String title,
    String description,
    Instant startsAt,
    Instant endsAt,
    UUID placeId,
    String placeCode,
    String placeName,
    String floorCode) {}
