package com.kivo.backend.navigation.application;

import com.kivo.backend.navigation.application.port.FeedbackRouteReadModel;
import com.kivo.backend.navigation.application.port.FeedbackRouteReadPort;
import com.kivo.backend.navigation.application.route.RouteStore;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeedbackRouteReadService implements FeedbackRouteReadPort {

  private final RouteStore routeStore;
  private final Clock clock;

  @Override
  public Optional<FeedbackRouteReadModel> findFeedbackRoute(UUID routeId) {
    return routeStore
        .find(routeId, clock.instant())
        .map(
            route ->
                new FeedbackRouteReadModel(
                    route.routeId(), route.mallId(), route.kioskCode(), route.placeId()));
  }
}
