package com.kivo.backend.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.kivo.backend.catalog.application.query.CategoryResponse;
import com.kivo.backend.catalog.application.query.PlaceResponse;
import com.kivo.backend.events.application.EventResponse;
import com.kivo.backend.feedback.api.CreateFeedbackRequest;
import com.kivo.backend.kiosks.application.query.KioskBootstrapResponse;
import com.kivo.backend.kiosks.application.query.KioskBootstrapResponse.FloorResponse;
import com.kivo.backend.kiosks.application.query.KioskBootstrapResponse.LanguageResponse;
import com.kivo.backend.navigation.api.RouteInstructionResponse;
import com.kivo.backend.navigation.api.RouteOverlayResponse;
import com.kivo.backend.navigation.api.RouteResponse;
import com.kivo.backend.navigation.domain.model.MovementType;
import com.kivo.backend.navigation.domain.routing.InstructionType;
import com.kivo.backend.navigation.domain.routing.MovementProfile;
import com.kivo.backend.sharing.api.ShareRouteResponse;
import com.kivo.backend.sharing.api.SharedRouteInstructionResponse;
import com.kivo.backend.sharing.api.SharedRouteOverlayResponse;
import com.kivo.backend.sharing.api.SharedRouteResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OpenApiExampleSerializationTest {

  private static final UUID CATEGORY_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
  private static final UUID PLACE_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");
  private static final UUID ROUTE_ID = UUID.fromString("73000000-0000-0000-0000-000000000001");
  private static final UUID SHARE_ID = UUID.fromString("74000000-0000-0000-0000-000000000001");
  private static final UUID EVENT_ID = UUID.fromString("75000000-0000-0000-0000-000000000001");
  private static final UUID MALL_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID GRAPH_VERSION_ID =
      UUID.fromString("30000000-0000-0000-0000-000000000001");
  private static final String SHARE_TOKEN = "a".repeat(43);

  private final JsonMapper jsonMapper =
      JsonMapper.builder()
          .findAndAddModules()
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .build();
  private final JsonNode openApi = readOpenApi();

  @Test
  void bootstrapExampleMatchesJacksonAndKeepsCollectionItemsFlat() {
    var response =
        new KioskBootstrapResponse(
            "demo-kiosk",
            "demo-mall",
            "Демо ТЦ",
            "ru",
            "en",
            "L1",
            List.of(new LanguageResponse("ru", "RU", 10), new LanguageResponse("en", "EN", 30)),
            List.of(
                new FloorResponse(
                    "L1",
                    "Первый этаж",
                    1,
                    10,
                    "https://cdn.example.com/maps/demo-mall/L1.png",
                    10000,
                    10000)));

    JsonNode json = assertExample("/kiosks/{kioskCode}/bootstrap", response);

    json.path("languages").forEach(this::assertFlatObject);
    json.path("floors").forEach(this::assertFlatObject);
    assertThat(json.has("startNavigationNodeCode")).isFalse();
  }

  @Test
  void categoryAndPlaceExamplesMatchJackson() {
    var category = new CategoryResponse(CATEGORY_ID, "food", "Гастрономия", 10);
    var place =
        new PlaceResponse(
            PLACE_ID, "grocery", "Grocery store", "Fresh groceries", "L1", CATEGORY_ID, "food", 10);

    assertExample("/malls/{mallCode}/categories", List.of(category));
    assertExample("/malls/{mallCode}/places", List.of(place));
    JsonNode placeJson = assertExample("/places/{placeId}", place);
    assertThat(placeJson.has("navigationNodeCode")).isFalse();
  }

  @Test
  void routeExamplesMatchJacksonAndKeepCollectionItemsFlat() {
    var response =
        new RouteResponse(
            ROUTE_ID,
            "demo-kiosk",
            PLACE_ID,
            MovementProfile.STANDARD,
            new BigDecimal("20.00"),
            15,
            Instant.parse("2026-08-10T10:00:00Z"),
            Instant.parse("2026-08-10T10:30:00Z"),
            List.of(
                new RouteInstructionResponse(0, InstructionType.START, "L1", null, null, null),
                new RouteInstructionResponse(
                    1,
                    InstructionType.CONTINUE,
                    "L1",
                    null,
                    MovementType.CORRIDOR,
                    new BigDecimal("20.00")),
                new RouteInstructionResponse(2, InstructionType.ARRIVE, "L1", null, null, null)),
            List.of(
                new RouteOverlayResponse(
                    "L1",
                    10,
                    "/api/v1/routes/73000000-0000-0000-0000-000000000001/floors/L1/overlay.png",
                    2048,
                    2048)));

    JsonNode created = assertExample("/routes", "post", "201", response);
    JsonNode fetched = assertExample("/routes/{routeId}", "get", "200", response);

    assertThat(created).isEqualTo(fetched);
    created.path("instructions").forEach(this::assertFlatObject);
    created.path("overlays").forEach(this::assertFlatObject);
    assertThat(created.toString()).doesNotContain("nodeCode", "geometry", "polylines");
  }

  @Test
  void sharingExamplesMatchJacksonAndDoNotExposeSnapshotGeometryOrToken() {
    Instant routeCreatedAt = Instant.parse("2026-08-10T10:00:00Z");
    Instant expiresAt = Instant.parse("2026-08-10T12:00:00Z");
    URI routeUrl = URI.create("https://routes.example/r/" + SHARE_TOKEN);
    URI qrImageUrl =
        URI.create("https://routes.example/api/v1/public/routes/" + SHARE_TOKEN + "/qr.png");
    var share = new ShareRouteResponse(SHARE_ID, routeUrl, qrImageUrl, expiresAt);
    var sharedRoute =
        new SharedRouteResponse(
            SHARE_ID,
            ROUTE_ID,
            MALL_ID,
            GRAPH_VERSION_ID,
            "demo-kiosk",
            PLACE_ID,
            MovementProfile.STANDARD,
            new BigDecimal("20.00"),
            15,
            routeCreatedAt,
            expiresAt,
            List.of(
                new SharedRouteInstructionResponse(
                    0, InstructionType.START, "L1", null, null, null),
                new SharedRouteInstructionResponse(
                    1,
                    InstructionType.CONTINUE,
                    "L1",
                    null,
                    MovementType.CORRIDOR,
                    new BigDecimal("20.00")),
                new SharedRouteInstructionResponse(
                    2, InstructionType.ARRIVE, "L1", null, null, null)),
            List.of(
                new SharedRouteOverlayResponse(
                    "L1",
                    10,
                    "https://cdn.example.com/maps/demo-mall/L1.png",
                    URI.create(
                        "https://routes.example/api/v1/public/routes/"
                            + SHARE_TOKEN
                            + "/floors/L1/overlay.png"),
                    256,
                    256)));

    assertExample("/routes/{routeId}/share", "post", "201", share);
    JsonNode json = assertExample("/public/routes/{shareToken}", sharedRoute);
    json.path("instructions").forEach(this::assertFlatObject);
    json.path("overlays").forEach(this::assertFlatObject);
    assertThat(json.toString()).doesNotContain("token", "geometry", "polylines", "markers");
  }

  @Test
  void eventAndFeedbackExamplesMatchJacksonAndRemainFlatAndAnonymous() {
    var event =
        new EventResponse(
            EVENT_ID,
            "tasting",
            "Дегустация",
            "Новинки фермерских продуктов",
            Instant.parse("2026-08-10T10:00:00Z"),
            Instant.parse("2026-08-10T11:00:00Z"),
            PLACE_ID,
            "grocery",
            "Продуктовый магазин",
            "L1");
    JsonNode eventJson = assertExample("/malls/{mallCode}/events/today", List.of(event));
    eventJson.forEach(this::assertFlatObject);

    CreateFeedbackRequest feedback = new CreateFeedbackRequest(ROUTE_ID, null, 5);
    JsonNode actual = jsonMapper.valueToTree(feedback);
    JsonNode documented =
        openApi
            .path("paths")
            .path("/feedback")
            .path("post")
            .path("requestBody")
            .path("content")
            .path("application/json")
            .path("example");
    assertThat(actual.toString()).isEqualTo(documented.toString());
    assertThat(actual.has("comment")).isFalse();
    assertThat(
            openApi
                .path("components")
                .path("schemas")
                .path("CreateFeedback")
                .path("additionalProperties")
                .asBoolean())
        .isFalse();
  }

  private JsonNode assertExample(String path, Object response) {
    return assertExample(path, "get", "200", response);
  }

  private JsonNode assertExample(String path, String method, String status, Object response) {
    JsonNode actual = jsonMapper.valueToTree(response);
    JsonNode documented =
        openApi
            .path("paths")
            .path(path)
            .path(method)
            .path("responses")
            .path(status)
            .path("content")
            .path("application/json")
            .path("example");
    assertThat(documented.isMissingNode()).isFalse();
    assertThat(actual.toString()).isEqualTo(documented.toString());
    return actual;
  }

  private void assertFlatObject(JsonNode item) {
    assertThat(item.isObject()).isTrue();
    item.elements().forEachRemaining(value -> assertThat(value.isArray()).isFalse());
  }

  private JsonNode readOpenApi() {
    try {
      return YAMLMapper.builder()
          .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
          .build()
          .readTree(Path.of("docs", "api", "openapi.yaml").toFile());
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot read OpenAPI document", exception);
    }
  }
}
