package com.kivo.backend.maps.infrastructure;

import com.kivo.backend.maps.application.OverlayMarker;
import com.kivo.backend.maps.application.OverlayMarkerType;
import com.kivo.backend.maps.application.OverlayPoint;
import com.kivo.backend.maps.application.OverlayPolyline;
import com.kivo.backend.maps.application.OverlayRenderRequest;
import com.kivo.backend.maps.application.RouteOverlayRenderer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

@Component
public class Java2dRouteOverlayRenderer implements RouteOverlayRenderer {

  private static final Color ROUTE_COLOR = Color.decode("#2563EB");
  private static final Color START_COLOR = Color.decode("#16A34A");
  private static final Color FINISH_COLOR = Color.decode("#DC2626");
  private static final Color TRANSITION_COLOR = Color.decode("#F59E0B");
  private static final Color WHITE = Color.WHITE;
  private static final float ROUTE_STROKE = 12.0f;
  private static final double OUTER_MARKER_RADIUS = 16.0;
  private static final double INNER_MARKER_RADIUS = 12.0;

  @Override
  public byte[] render(OverlayRenderRequest request) {
    BufferedImage image =
        new BufferedImage(request.pixelWidth(), request.pixelHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = image.createGraphics();
    try {
      configure(graphics);
      drawPolylines(graphics, request);
      drawMarkers(graphics, request);
    } finally {
      graphics.dispose();
    }
    return encode(image);
  }

  private static void configure(Graphics2D graphics) {
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    graphics.setStroke(
        new BasicStroke(ROUTE_STROKE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
  }

  private static void drawPolylines(Graphics2D graphics, OverlayRenderRequest request) {
    graphics.setColor(ROUTE_COLOR);
    for (OverlayPolyline polyline : request.polylines()) {
      if (polyline.points().size() < 2) {
        continue;
      }
      Path2D path = new Path2D.Double();
      OverlayPoint first = polyline.points().getFirst();
      path.moveTo(scaleX(first.x(), request), scaleY(first.y(), request));
      polyline.points().stream()
          .skip(1)
          .forEach(point -> path.lineTo(scaleX(point.x(), request), scaleY(point.y(), request)));
      graphics.draw(path);
    }
  }

  private static void drawMarkers(Graphics2D graphics, OverlayRenderRequest request) {
    for (OverlayMarker marker : request.markers()) {
      double x = scaleX(marker.point().x(), request);
      double y = scaleY(marker.point().y(), request);
      drawCircle(graphics, x, y, OUTER_MARKER_RADIUS, WHITE);
      if (marker.type() == OverlayMarkerType.START_FINISH) {
        drawCircle(graphics, x, y, INNER_MARKER_RADIUS, START_COLOR);
        drawCircle(graphics, x, y, INNER_MARKER_RADIUS / 2.0, FINISH_COLOR);
      } else {
        drawCircle(graphics, x, y, INNER_MARKER_RADIUS, color(marker.type()));
      }
    }
  }

  private static Color color(OverlayMarkerType type) {
    return switch (type) {
      case START -> START_COLOR;
      case FINISH -> FINISH_COLOR;
      case TRANSITION -> TRANSITION_COLOR;
      case START_FINISH ->
          throw new IllegalArgumentException("Combined marker is drawn separately");
    };
  }

  private static void drawCircle(
      Graphics2D graphics, double x, double y, double radius, Color color) {
    graphics.setColor(color);
    graphics.fill(new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2));
  }

  private static double scaleX(int x, OverlayRenderRequest request) {
    return scale(x, request.coordinateWidth(), request.pixelWidth());
  }

  private static double scaleY(int y, OverlayRenderRequest request) {
    return scale(y, request.coordinateHeight(), request.pixelHeight());
  }

  private static double scale(int coordinate, int coordinateSize, int pixelSize) {
    int clipped = Math.clamp(coordinate, 0, coordinateSize);
    return clipped * Math.max(0, pixelSize - 1) / (double) coordinateSize;
  }

  private static byte[] encode(BufferedImage image) {
    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      if (!ImageIO.write(image, "png", output)) {
        throw new IllegalStateException("PNG writer is unavailable");
      }
      return output.toByteArray();
    } catch (IOException exception) {
      throw new IllegalStateException("Could not render route overlay", exception);
    }
  }
}
