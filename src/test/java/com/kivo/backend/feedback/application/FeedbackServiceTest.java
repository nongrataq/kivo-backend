package com.kivo.backend.feedback.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kivo.backend.catalog.application.port.FeedbackPlaceReadModel;
import com.kivo.backend.catalog.application.port.FeedbackPlaceReadPort;
import com.kivo.backend.feedback.persistence.repository.FeedbackRepository;
import com.kivo.backend.kiosks.application.port.FeedbackKioskReadModel;
import com.kivo.backend.kiosks.application.port.FeedbackKioskReadPort;
import com.kivo.backend.navigation.application.port.FeedbackRouteReadModel;
import com.kivo.backend.navigation.application.port.FeedbackRouteReadPort;
import com.kivo.backend.sharing.application.FeedbackShareReadPort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

  private static final UUID ROUTE_ID = UUID.fromString("73000000-0000-0000-0000-000000000001");
  private static final UUID MALL_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID KIOSK_ID = UUID.fromString("00000000-0000-0000-0000-000000000601");
  private static final UUID PLACE_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");
  private static final Instant NOW = Instant.parse("2026-08-10T10:00:00Z");

  @Mock private FeedbackRouteReadPort routePort;
  @Mock private FeedbackShareReadPort sharePort;
  @Mock private FeedbackKioskReadPort kioskPort;
  @Mock private FeedbackPlaceReadPort placePort;
  @Mock private FeedbackRepository repository;
  private FeedbackService service;

  @BeforeEach
  void setUp() {
    service =
        new FeedbackService(
            routePort,
            sharePort,
            kioskPort,
            placePort,
            repository,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void resolvesAllContextThroughReadPortsAndPersistsNoText() {
    when(routePort.findFeedbackRoute(ROUTE_ID))
        .thenReturn(
            Optional.of(new FeedbackRouteReadModel(ROUTE_ID, MALL_ID, "demo-kiosk", PLACE_ID)));
    when(kioskPort.findActiveFeedbackKiosk("demo-kiosk"))
        .thenReturn(Optional.of(new FeedbackKioskReadModel(KIOSK_ID, MALL_ID, "demo-kiosk")));
    when(placePort.findActiveFeedbackPlace(PLACE_ID))
        .thenReturn(Optional.of(new FeedbackPlaceReadModel(PLACE_ID, MALL_ID)));

    service.create(new CreateFeedbackCommand(ROUTE_ID, null, 5));

    verify(repository)
        .saveAndFlush(
            argThat(
                feedback ->
                    ROUTE_ID.equals(feedback.getRouteId())
                        && feedback.getShareId() == null
                        && feedback.getRating() == 5
                        && NOW.equals(feedback.getCreatedAt())));
  }

  @Test
  void rejectsCrossMallContext() {
    UUID foreignMall = UUID.fromString("00000000-0000-0000-0000-000000000800");
    when(routePort.findFeedbackRoute(ROUTE_ID))
        .thenReturn(
            Optional.of(new FeedbackRouteReadModel(ROUTE_ID, MALL_ID, "demo-kiosk", PLACE_ID)));
    when(kioskPort.findActiveFeedbackKiosk("demo-kiosk"))
        .thenReturn(Optional.of(new FeedbackKioskReadModel(KIOSK_ID, foreignMall, "demo-kiosk")));
    when(placePort.findActiveFeedbackPlace(PLACE_ID))
        .thenReturn(Optional.of(new FeedbackPlaceReadModel(PLACE_ID, MALL_ID)));

    assertThatThrownBy(() -> service.create(new CreateFeedbackCommand(ROUTE_ID, null, 4)))
        .isInstanceOf(FeedbackContextNotFoundException.class);
  }
}
