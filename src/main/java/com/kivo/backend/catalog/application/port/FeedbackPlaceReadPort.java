package com.kivo.backend.catalog.application.port;

import java.util.Optional;
import java.util.UUID;

public interface FeedbackPlaceReadPort {

  Optional<FeedbackPlaceReadModel> findActiveFeedbackPlace(UUID placeId);
}
