package com.kivo.backend.navigation.application.route;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kivo.backend.catalog.application.port.CatalogNavigationReadPort;
import com.kivo.backend.catalog.application.port.RouteDestinationReadModel;
import com.kivo.backend.kiosks.application.port.KioskNavigationReadPort;
import com.kivo.backend.kiosks.application.port.RouteStartReadModel;
import com.kivo.backend.maps.application.RouteOverlayRenderer;
import com.kivo.backend.navigation.application.ActiveGraphNotFoundException;
import com.kivo.backend.navigation.application.PlannedRoute;
import com.kivo.backend.navigation.application.RoutePlanner;
import com.kivo.backend.navigation.application.exception.NavigationGraphUnavailableException;
import com.kivo.backend.navigation.application.exception.RouteEndpointMismatchException;
import com.kivo.backend.navigation.application.exception.RouteNotFoundServiceException;
import com.kivo.backend.navigation.domain.routing.MovementProfile;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RouteDeliveryServiceTest {

  private static final UUID MALL_ID = UUID.fromString("74000000-0000-0000-0000-000000000001");
  private static final UUID PLACE_ID = UUID.fromString("74000000-0000-0000-0000-000000000002");
  private static final Instant NOW = Instant.parse("2026-08-10T10:00:00Z");

  @Mock private KioskNavigationReadPort kioskPort;
  @Mock private CatalogNavigationReadPort catalogPort;
  @Mock private RoutePlanner routePlanner;
  @Mock private RouteDocumentFactory documentFactory;
  @Mock private RouteStore routeStore;
  @Mock private RouteOverlayRenderer renderer;

  private RouteDeliveryService service;

  @BeforeEach
  void setUp() {
    service =
        new RouteDeliveryService(
            kioskPort,
            catalogPort,
            routePlanner,
            documentFactory,
            routeStore,
            renderer,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void defaultsProfileAndStoresCreatedDocument() {
    var command = new RouteCreateCommand("kiosk", PLACE_ID, null);
    var normalized = new RouteCreateCommand("kiosk", PLACE_ID, MovementProfile.STANDARD);
    PlannedRoute planned = mock(PlannedRoute.class);
    RouteDocument document = mock(RouteDocument.class);
    stubEndpoints(MALL_ID, MALL_ID, "start", "target");
    when(routePlanner.planWithSnapshot(MALL_ID, "start", "target", MovementProfile.STANDARD))
        .thenReturn(planned);
    when(catalogPort.findActiveRouteFloors(MALL_ID)).thenReturn(List.of());
    when(documentFactory.create(eq(normalized), eq(planned), anyList(), eq(NOW)))
        .thenReturn(document);

    service.create(command);

    verify(routeStore).save(document);
  }

  @Test
  void rejectsDifferentMallsAndMissingAnchorsBeforePlanning() {
    UUID otherMall = UUID.fromString("74000000-0000-0000-0000-000000000003");
    stubEndpoints(MALL_ID, otherMall, "start", "target");

    assertThatThrownBy(
            () ->
                service.create(
                    new RouteCreateCommand("kiosk", PLACE_ID, MovementProfile.ACCESSIBLE)))
        .isInstanceOf(RouteEndpointMismatchException.class);
    verify(routePlanner, never())
        .planWithSnapshot(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());

    stubEndpoints(MALL_ID, MALL_ID, null, "target");
    assertThatThrownBy(
            () ->
                service.create(new RouteCreateCommand("kiosk", PLACE_ID, MovementProfile.STANDARD)))
        .isInstanceOf(RouteNotFoundServiceException.class);
  }

  @Test
  void mapsMissingActiveGraphToStableServiceError() {
    stubEndpoints(MALL_ID, MALL_ID, "start", "target");
    when(routePlanner.planWithSnapshot(MALL_ID, "start", "target", MovementProfile.STANDARD))
        .thenThrow(new ActiveGraphNotFoundException(MALL_ID));

    assertThatThrownBy(
            () ->
                service.create(new RouteCreateCommand("kiosk", PLACE_ID, MovementProfile.STANDARD)))
        .isInstanceOf(NavigationGraphUnavailableException.class);
  }

  private void stubEndpoints(
      UUID kioskMall, UUID placeMall, String startNodeCode, String destinationNodeCode) {
    when(kioskPort.findActiveRouteStart("kiosk"))
        .thenReturn(Optional.of(new RouteStartReadModel(kioskMall, "kiosk", startNodeCode)));
    when(catalogPort.findActiveRouteDestination(PLACE_ID))
        .thenReturn(
            Optional.of(new RouteDestinationReadModel(PLACE_ID, placeMall, destinationNodeCode)));
  }
}
