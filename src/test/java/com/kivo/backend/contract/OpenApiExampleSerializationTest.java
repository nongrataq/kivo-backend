package com.kivo.backend.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.kivo.backend.catalog.application.query.CategoryResponse;
import com.kivo.backend.catalog.application.query.PlaceResponse;
import com.kivo.backend.kiosks.application.query.KioskBootstrapResponse;
import com.kivo.backend.kiosks.application.query.KioskBootstrapResponse.FloorResponse;
import com.kivo.backend.kiosks.application.query.KioskBootstrapResponse.LanguageResponse;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OpenApiExampleSerializationTest {

  private static final UUID CATEGORY_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
  private static final UUID PLACE_ID = UUID.fromString("00000000-0000-0000-0000-000000000401");

  private final JsonMapper jsonMapper = JsonMapper.builder().findAndAddModules().build();
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

  private JsonNode assertExample(String path, Object response) {
    JsonNode actual = jsonMapper.valueToTree(response);
    JsonNode documented =
        openApi
            .path("paths")
            .path(path)
            .path("get")
            .path("responses")
            .path("200")
            .path("content")
            .path("application/json")
            .path("example");
    assertThat(documented.isMissingNode()).isFalse();
    assertThat(actual).isEqualTo(documented);
    return actual;
  }

  private void assertFlatObject(JsonNode item) {
    assertThat(item.isObject()).isTrue();
    item.elements().forEachRemaining(value -> assertThat(value.isArray()).isFalse());
  }

  private JsonNode readOpenApi() {
    try {
      return new YAMLMapper().readTree(Path.of("docs", "api", "openapi.yaml").toFile());
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot read OpenAPI document", exception);
    }
  }
}
