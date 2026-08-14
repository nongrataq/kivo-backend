package com.kivo.backend.sharing;

import com.kivo.backend.maps.application.OverlayMarker;
import com.kivo.backend.maps.application.OverlayMarkerType;
import com.kivo.backend.maps.application.OverlayPoint;
import com.kivo.backend.maps.application.OverlayPolyline;
import com.kivo.backend.navigation.application.route.RouteDocument;
import com.kivo.backend.navigation.application.route.RouteInstructionDocument;
import com.kivo.backend.navigation.application.route.RouteOverlayDocument;
import com.kivo.backend.navigation.domain.model.MovementType;
import com.kivo.backend.navigation.domain.routing.InstructionType;
import com.kivo.backend.navigation.domain.routing.MovementProfile;
import com.kivo.backend.sharing.application.SharedRouteSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class SharingTestFixtures {

  public static final UUID ROUTE_ID = UUID.fromString("73000000-0000-0000-0000-000000000001");
  public static final UUID MALL_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  public static final UUID GRAPH_VERSION_ID =
      UUID.fromString("30000000-0000-0000-0000-000000000001");
  public static final UUID FLOOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000301");
  public static final UUID PLACE_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");
  public static final Instant CREATED_AT = Instant.parse("2026-08-10T10:00:00Z");

  private SharingTestFixtures() {}

  public static RouteDocument route() {
    return new RouteDocument(
        ROUTE_ID,
        MALL_ID,
        GRAPH_VERSION_ID,
        "demo-kiosk",
        PLACE_ID,
        MovementProfile.STANDARD,
        new BigDecimal("20.00"),
        15,
        CREATED_AT,
        CREATED_AT.plusSeconds(1800),
        List.of(
            new RouteInstructionDocument(0, InstructionType.START, "L1", null, null, null),
            new RouteInstructionDocument(
                1,
                InstructionType.CONTINUE,
                "L1",
                null,
                MovementType.CORRIDOR,
                new BigDecimal("20.00")),
            new RouteInstructionDocument(2, InstructionType.ARRIVE, "L1", null, null, null)),
        List.of(
            new RouteOverlayDocument(
                FLOOR_ID,
                "L1",
                10,
                "https://cdn.example.com/maps/demo-mall/L1.png",
                "/api/v1/routes/" + ROUTE_ID + "/floors/L1/overlay.png",
                10000,
                10000,
                256,
                256,
                List.of(
                    new OverlayPolyline(
                        List.of(new OverlayPoint(100, 100), new OverlayPoint(300, 100)))),
                List.of(
                    new OverlayMarker(new OverlayPoint(100, 100), OverlayMarkerType.START),
                    new OverlayMarker(new OverlayPoint(300, 100), OverlayMarkerType.FINISH)))));
  }

  public static SharedRouteSnapshot snapshot() {
    return SharedRouteSnapshot.from(route());
  }
}
