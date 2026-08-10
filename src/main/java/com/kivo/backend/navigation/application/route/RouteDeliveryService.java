package com.kivo.backend.navigation.application.route;

import com.kivo.backend.catalog.application.port.CatalogNavigationReadPort;
import com.kivo.backend.common.exception.service.KioskNotFoundException;
import com.kivo.backend.common.exception.service.PlaceNotFoundException;
import com.kivo.backend.kiosks.application.port.KioskNavigationReadPort;
import com.kivo.backend.maps.application.RouteOverlayRenderer;
import com.kivo.backend.navigation.application.ActiveGraphNotFoundException;
import com.kivo.backend.navigation.application.RoutePlanner;
import com.kivo.backend.navigation.application.exception.NavigationGraphUnavailableException;
import com.kivo.backend.navigation.application.exception.RouteEndpointMismatchException;
import com.kivo.backend.navigation.application.exception.RouteFloorNotFoundException;
import com.kivo.backend.navigation.application.exception.RouteNotFoundServiceException;
import com.kivo.backend.navigation.domain.routing.MovementProfile;
import com.kivo.backend.navigation.domain.routing.RouteNotFoundException;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RouteDeliveryService {

  private final KioskNavigationReadPort kioskNavigationReadPort;
  private final CatalogNavigationReadPort catalogNavigationReadPort;
  private final RoutePlanner routePlanner;
  private final RouteDocumentFactory routeDocumentFactory;
  private final RouteStore routeStore;
  private final RouteOverlayRenderer routeOverlayRenderer;
  private final Clock clock;

  public RouteDocument create(RouteCreateCommand command) {
    var start =
        kioskNavigationReadPort
            .findActiveRouteStart(command.kioskCode())
            .orElseThrow(() -> new KioskNotFoundException(command.kioskCode()));
    var destination =
        catalogNavigationReadPort
            .findActiveRouteDestination(command.placeId())
            .orElseThrow(() -> new PlaceNotFoundException(command.placeId()));
    if (!start.mallId().equals(destination.mallId())) {
      throw new RouteEndpointMismatchException(command.kioskCode(), command.placeId());
    }
    if (start.navigationNodeCode() == null
        || start.navigationNodeCode().isBlank()
        || destination.navigationNodeCode() == null
        || destination.navigationNodeCode().isBlank()) {
      throw new RouteNotFoundServiceException();
    }

    MovementProfile profile =
        command.profile() == null ? MovementProfile.STANDARD : command.profile();
    try {
      var plannedRoute =
          routePlanner.planWithSnapshot(
              start.mallId(),
              start.navigationNodeCode(),
              destination.navigationNodeCode(),
              profile);
      var route =
          routeDocumentFactory.create(
              new RouteCreateCommand(command.kioskCode(), command.placeId(), profile),
              plannedRoute,
              catalogNavigationReadPort.findActiveRouteFloors(start.mallId()),
              clock.instant());
      routeStore.save(route);
      return route;
    } catch (RouteNotFoundException exception) {
      throw new RouteNotFoundServiceException();
    } catch (ActiveGraphNotFoundException exception) {
      throw new NavigationGraphUnavailableException();
    }
  }

  public RouteDocument get(UUID routeId) {
    return routeStore
        .find(routeId, clock.instant())
        .orElseThrow(RouteNotFoundServiceException::new);
  }

  public RenderedRouteOverlay getOverlay(UUID routeId, String floorCode) {
    RouteDocument route = get(routeId);
    RouteOverlayDocument overlay =
        route.overlays().stream()
            .filter(candidate -> candidate.floorCode().equals(floorCode))
            .findFirst()
            .orElseThrow(() -> new RouteFloorNotFoundException(routeId, floorCode));
    return new RenderedRouteOverlay(routeOverlayRenderer.render(overlay.toRenderRequest()));
  }
}
