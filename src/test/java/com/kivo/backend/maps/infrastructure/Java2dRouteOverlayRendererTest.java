package com.kivo.backend.maps.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.kivo.backend.maps.application.OverlayMarker;
import com.kivo.backend.maps.application.OverlayMarkerType;
import com.kivo.backend.maps.application.OverlayPoint;
import com.kivo.backend.maps.application.OverlayPolyline;
import com.kivo.backend.maps.application.OverlayRenderRequest;
import java.io.ByteArrayInputStream;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class Java2dRouteOverlayRendererTest {

  private final Java2dRouteOverlayRenderer renderer = new Java2dRouteOverlayRenderer();

  @Test
  void rendersTransparentPngWithClippedGeometryAndMarkers() throws Exception {
    var request =
        new OverlayRenderRequest(
            10000,
            5000,
            256,
            128,
            List.of(
                new OverlayPolyline(
                    List.of(new OverlayPoint(-100, 2500), new OverlayPoint(10100, 2500)))),
            List.of(
                new OverlayMarker(new OverlayPoint(0, 2500), OverlayMarkerType.START),
                new OverlayMarker(new OverlayPoint(10000, 2500), OverlayMarkerType.FINISH)));

    byte[] png = renderer.render(request);
    var image = ImageIO.read(new ByteArrayInputStream(png));

    assertThat(png).startsWith(0x89, 0x50, 0x4e, 0x47);
    assertThat(image.getWidth()).isEqualTo(256);
    assertThat(image.getHeight()).isEqualTo(128);
    assertThat(image.getColorModel().hasAlpha()).isTrue();
    assertThat((image.getRGB(128, 10) >>> 24) & 0xff).isZero();
    assertThat((image.getRGB(128, 64) >>> 24) & 0xff).isPositive();
  }

  @Test
  void rendersCombinedMarkerForZeroRoute() throws Exception {
    byte[] png =
        renderer.render(
            new OverlayRenderRequest(
                100,
                100,
                100,
                100,
                List.of(),
                List.of(
                    new OverlayMarker(new OverlayPoint(50, 50), OverlayMarkerType.START_FINISH))));

    var image = ImageIO.read(new ByteArrayInputStream(png));

    assertThat((image.getRGB(50, 50) >>> 24) & 0xff).isEqualTo(255);
    assertThat(image.getRGB(50, 50) & 0x00ffffff).isEqualTo(0x00dc2626);
  }
}
