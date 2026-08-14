package com.kivo.backend.sharing.application;

import com.kivo.backend.maps.application.RouteOverlayRenderer;
import com.kivo.backend.navigation.application.route.RouteDeliveryService;
import com.kivo.backend.sharing.configuration.SharingProperties;
import com.kivo.backend.sharing.persistence.entity.SharedRoute;
import com.kivo.backend.sharing.persistence.repository.SharedRouteRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RouteSharingService {

  private static final int MIN_TOKEN_LENGTH = 32;
  private static final int MAX_TOKEN_LENGTH = 128;

  private final RouteDeliveryService routeDeliveryService;
  private final SharedRouteRepository sharedRouteRepository;
  private final SharedRouteTokenGenerator tokenGenerator;
  private final SharedRouteTokenHasher tokenHasher;
  private final SharedRouteSnapshotCodec snapshotCodec;
  private final SharingLinks sharingLinks;
  private final QrCodeGenerator qrCodeGenerator;
  private final RouteOverlayRenderer routeOverlayRenderer;
  private final SharingProperties properties;
  private final Clock clock;

  @Transactional
  public ShareRouteResult share(UUID routeId) {
    var route = routeDeliveryService.get(routeId);
    Instant now = clock.instant();
    Instant expiresAt = now.plus(properties.getTtl());
    String token = tokenGenerator.generate();
    String tokenHash = tokenHasher.hash(token);
    SharedRouteSnapshot snapshot = SharedRouteSnapshot.from(route);
    SharedRoute sharedRoute =
        SharedRoute.create(
            route.routeId(),
            route.mallId(),
            route.graphVersionId(),
            tokenHash,
            snapshotCodec.encode(snapshot),
            now,
            expiresAt);
    sharedRouteRepository.saveAndFlush(sharedRoute);
    return new ShareRouteResult(
        sharedRoute.getId(),
        sharingLinks.routeUrl(token),
        sharingLinks.qrImageUrl(token),
        expiresAt);
  }

  @Transactional(readOnly = true)
  public SharedRouteAccess get(String token) {
    SharedRoute sharedRoute = requiredActive(token, clock.instant());
    return new SharedRouteAccess(
        sharedRoute.getId(),
        token,
        sharedRoute.getExpiresAt(),
        snapshotCodec.decode(sharedRoute.getSnapshotJson()));
  }

  @Transactional(readOnly = true)
  public byte[] getQrCode(String token) {
    SharedRouteAccess access = get(token);
    return qrCodeGenerator.generatePng(
        sharingLinks.routeUrl(access.token()).toString(), properties.getQrSize());
  }

  @Transactional(readOnly = true)
  public byte[] getOverlay(String token, String floorCode) {
    SharedRouteAccess access = get(token);
    SharedRouteOverlaySnapshot overlay =
        access.snapshot().overlays().stream()
            .filter(candidate -> candidate.floorCode().equals(floorCode))
            .findFirst()
            .orElseThrow(() -> new SharedRouteFloorNotFoundException(floorCode));
    return routeOverlayRenderer.render(overlay.toRenderRequest());
  }

  @Transactional
  public void revoke(UUID shareId) {
    SharedRoute sharedRoute =
        sharedRouteRepository.findById(shareId).orElseThrow(SharedRouteNotFoundException::new);
    sharedRoute.revoke(clock.instant());
  }

  private SharedRoute requiredActive(String token, Instant now) {
    if (token == null || token.length() < MIN_TOKEN_LENGTH || token.length() > MAX_TOKEN_LENGTH) {
      throw new SharedRouteNotFoundException();
    }
    return sharedRouteRepository
        .findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(tokenHasher.hash(token), now)
        .orElseThrow(SharedRouteNotFoundException::new);
  }
}
