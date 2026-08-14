package com.kivo.backend.sharing.application;

import static com.kivo.backend.sharing.SharingTestFixtures.CREATED_AT;
import static com.kivo.backend.sharing.SharingTestFixtures.GRAPH_VERSION_ID;
import static com.kivo.backend.sharing.SharingTestFixtures.MALL_ID;
import static com.kivo.backend.sharing.SharingTestFixtures.ROUTE_ID;
import static com.kivo.backend.sharing.SharingTestFixtures.route;
import static com.kivo.backend.sharing.SharingTestFixtures.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kivo.backend.maps.application.OverlayRenderRequest;
import com.kivo.backend.maps.application.RouteOverlayRenderer;
import com.kivo.backend.navigation.application.route.RouteDeliveryService;
import com.kivo.backend.sharing.configuration.SharingProperties;
import com.kivo.backend.sharing.persistence.entity.SharedRoute;
import com.kivo.backend.sharing.persistence.repository.SharedRouteRepository;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RouteSharingServiceTest {

  private static final String TOKEN = "a".repeat(43);
  private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4e, 0x47};

  private final RouteDeliveryService routeDeliveryService = mock(RouteDeliveryService.class);
  private final SharedRouteRepository repository = mock(SharedRouteRepository.class);
  private final SharedRouteTokenGenerator tokenGenerator = mock(SharedRouteTokenGenerator.class);
  private final SharedRouteSnapshotCodec snapshotCodec = mock(SharedRouteSnapshotCodec.class);
  private final QrCodeGenerator qrCodeGenerator = mock(QrCodeGenerator.class);
  private final RouteOverlayRenderer overlayRenderer = mock(RouteOverlayRenderer.class);
  private final SharedRouteTokenHasher tokenHasher = new SharedRouteTokenHasher();

  private RouteSharingService service;

  @BeforeEach
  void setUp() {
    SharingProperties properties = new SharingProperties();
    properties.setTtl(Duration.ofHours(2));
    properties.setPublicBaseUrl(URI.create("https://routes.example/kivo"));
    properties.setQrSize(256);
    service =
        new RouteSharingService(
            routeDeliveryService,
            repository,
            tokenGenerator,
            tokenHasher,
            snapshotCodec,
            new SharingLinks(properties),
            qrCodeGenerator,
            overlayRenderer,
            properties,
            Clock.fixed(CREATED_AT, ZoneOffset.UTC));
  }

  @Test
  void storesOnlyHashedTokenAndAnImmutableSnapshot() {
    when(routeDeliveryService.get(ROUTE_ID)).thenReturn(route());
    when(tokenGenerator.generate()).thenReturn(TOKEN);
    when(snapshotCodec.encode(any())).thenReturn("{\"schemaVersion\":1}");

    ShareRouteResult result = service.share(ROUTE_ID);

    ArgumentCaptor<SharedRoute> captor = ArgumentCaptor.forClass(SharedRoute.class);
    verify(repository).saveAndFlush(captor.capture());
    SharedRoute stored = captor.getValue();
    assertThat(stored.getTokenHash()).isEqualTo(tokenHasher.hash(TOKEN)).isNotEqualTo(TOKEN);
    assertThat(stored.getSnapshotJson()).isEqualTo("{\"schemaVersion\":1}");
    assertThat(stored.getMallId()).isEqualTo(MALL_ID);
    assertThat(stored.getGraphVersionId()).isEqualTo(GRAPH_VERSION_ID);
    assertThat(stored.getCreatedAt()).isEqualTo(CREATED_AT);
    assertThat(stored.getExpiresAt()).isEqualTo(CREATED_AT.plus(Duration.ofHours(2)));
    assertThat(result.routeUrl()).isEqualTo(URI.create("https://routes.example/kivo/r/" + TOKEN));
    assertThat(result.qrImageUrl())
        .isEqualTo(
            URI.create("https://routes.example/kivo/api/v1/public/routes/" + TOKEN + "/qr.png"));
  }

  @Test
  void regeneratesQrAndOverlayFromTheStoredSnapshot() {
    SharedRoute stored =
        SharedRoute.create(
            ROUTE_ID,
            MALL_ID,
            GRAPH_VERSION_ID,
            tokenHasher.hash(TOKEN),
            "{\"schemaVersion\":1}",
            CREATED_AT,
            CREATED_AT.plus(Duration.ofHours(2)));
    when(repository.findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(
            eq(tokenHasher.hash(TOKEN)), eq(CREATED_AT)))
        .thenReturn(Optional.of(stored));
    when(snapshotCodec.decode(stored.getSnapshotJson())).thenReturn(snapshot());
    when(qrCodeGenerator.generatePng(any(), eq(256))).thenReturn(PNG);
    when(overlayRenderer.render(any())).thenReturn(PNG);

    assertThat(service.getQrCode(TOKEN)).isEqualTo(PNG);
    assertThat(service.getOverlay(TOKEN, "L1")).isEqualTo(PNG);

    verify(qrCodeGenerator).generatePng("https://routes.example/kivo/r/" + TOKEN, 256);
    ArgumentCaptor<OverlayRenderRequest> request =
        ArgumentCaptor.forClass(OverlayRenderRequest.class);
    verify(overlayRenderer).render(request.capture());
    assertThat(request.getValue().pixelWidth()).isEqualTo(256);
    assertThat(request.getValue().polylines()).hasSize(1);
  }

  @Test
  void rejectsMalformedAndUnknownTokensWithoutLeakingTheirStatus() {
    assertThatThrownBy(() -> service.get("short")).isInstanceOf(SharedRouteNotFoundException.class);
    verifyNoInteractions(repository);

    when(repository.findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(any(), eq(CREATED_AT)))
        .thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.get(TOKEN)).isInstanceOf(SharedRouteNotFoundException.class);
  }

  @Test
  void reportsAStoredFloorMismatch() {
    SharedRoute stored =
        SharedRoute.create(
            ROUTE_ID,
            MALL_ID,
            GRAPH_VERSION_ID,
            tokenHasher.hash(TOKEN),
            "{}",
            CREATED_AT,
            CREATED_AT.plus(Duration.ofHours(2)));
    when(repository.findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(any(), eq(CREATED_AT)))
        .thenReturn(Optional.of(stored));
    when(snapshotCodec.decode("{}")).thenReturn(snapshot());

    assertThatThrownBy(() -> service.getOverlay(TOKEN, "L9"))
        .isInstanceOfSatisfying(
            SharedRouteFloorNotFoundException.class,
            exception -> assertThat(exception.getMessageArgs()).containsExactly("L9"));
  }
}
