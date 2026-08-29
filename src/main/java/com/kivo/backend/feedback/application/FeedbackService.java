package com.kivo.backend.feedback.application;

import com.kivo.backend.catalog.application.port.FeedbackPlaceReadPort;
import com.kivo.backend.feedback.persistence.entity.Feedback;
import com.kivo.backend.feedback.persistence.repository.FeedbackRepository;
import com.kivo.backend.kiosks.application.port.FeedbackKioskReadPort;
import com.kivo.backend.navigation.application.port.FeedbackRouteReadModel;
import com.kivo.backend.navigation.application.port.FeedbackRouteReadPort;
import com.kivo.backend.sharing.application.FeedbackShareReadPort;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedbackService {

  private final FeedbackRouteReadPort feedbackRouteReadPort;
  private final FeedbackShareReadPort feedbackShareReadPort;
  private final FeedbackKioskReadPort feedbackKioskReadPort;
  private final FeedbackPlaceReadPort feedbackPlaceReadPort;
  private final FeedbackRepository feedbackRepository;
  private final Clock clock;

  @Transactional
  public void create(CreateFeedbackCommand request) {
    FeedbackContext context = resolveContext(request);
    var kiosk =
        feedbackKioskReadPort
            .findActiveFeedbackKiosk(context.kioskCode())
            .orElseThrow(FeedbackContextNotFoundException::new);
    var place =
        feedbackPlaceReadPort
            .findActiveFeedbackPlace(context.placeId())
            .orElseThrow(FeedbackContextNotFoundException::new);
    if (!context.mallId().equals(kiosk.mallId()) || !context.mallId().equals(place.mallId())) {
      throw new FeedbackContextNotFoundException();
    }

    Feedback feedback =
        request.routeId() != null
            ? Feedback.forRoute(
                context.mallId(),
                kiosk.kioskId(),
                place.placeId(),
                request.routeId(),
                request.rating(),
                clock.instant())
            : Feedback.forShare(
                context.mallId(),
                kiosk.kioskId(),
                place.placeId(),
                request.shareId(),
                request.rating(),
                clock.instant());
    feedbackRepository.saveAndFlush(feedback);
  }

  private FeedbackContext resolveContext(CreateFeedbackCommand request) {
    if (request.routeId() != null) {
      FeedbackRouteReadModel route =
          feedbackRouteReadPort
              .findFeedbackRoute(request.routeId())
              .orElseThrow(FeedbackContextNotFoundException::new);
      return new FeedbackContext(route.mallId(), route.kioskCode(), route.placeId());
    }
    return feedbackShareReadPort
        .findFeedbackShare(request.shareId())
        .map(share -> new FeedbackContext(share.mallId(), share.kioskCode(), share.placeId()))
        .orElseThrow(FeedbackContextNotFoundException::new);
  }

  private record FeedbackContext(UUID mallId, String kioskCode, UUID placeId) {}
}
