package com.kivo.backend.navigation.application.port;

import java.util.Optional;
import java.util.UUID;

public interface FeedbackRouteReadPort {

  Optional<FeedbackRouteReadModel> findFeedbackRoute(UUID routeId);
}
