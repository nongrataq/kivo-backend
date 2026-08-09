package com.kivo.backend.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kivo.backend.common.api.ResourceNotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Sql("/sql/place-test-data.sql")
class PlaceQueryServiceIntegrationTest {

  private static final UUID FOOD_CATEGORY_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000101");

  @Autowired private PlaceQueryService placeQueryService;

  @Test
  void returnsOnlyActivePlacesInDisplayOrder() {
    var places = placeQueryService.findPlaces("demo-mall", null, null, "ru");

    assertThat(places)
        .extracting(place -> place.code() + ":" + place.name())
        .containsExactly("coffee-shop:Кофейня", "fashion-shop:Бутик", "information:information");
  }

  @Test
  void filtersByCategory() {
    var places = placeQueryService.findPlaces("demo-mall", null, FOOD_CATEGORY_ID, "ru");

    assertThat(places)
        .extracting(place -> place.code())
        .containsExactly("coffee-shop", "information");
  }

  @Test
  void searchesNamesDescriptionsAndCodes() {
    assertThat(placeQueryService.findPlaces("demo-mall", "ДеСеРт", null, "ru"))
        .extracting(place -> place.code())
        .containsExactly("coffee-shop");
    assertThat(placeQueryService.findPlaces("demo-mall", "fashion", null, "ru"))
        .extracting(place -> place.code())
        .containsExactly("fashion-shop");
  }

  @Test
  void fallsBackToMallDefaultLanguageAndThenCode() {
    var places = placeQueryService.findPlaces("demo-mall", null, null, "en");

    assertThat(places)
        .extracting(place -> place.code() + ":" + place.name())
        .containsExactly(
            "coffee-shop:Coffee shop", "fashion-shop:Бутик", "information:information");
  }

  @Test
  void returnsEmptyListWhenNothingMatches() {
    assertThat(placeQueryService.findPlaces("demo-mall", "missing", null, "ru")).isEmpty();
  }

  @Test
  void rejectsUnknownMall() {
    assertThatThrownBy(() -> placeQueryService.findPlaces("unknown", null, null, "ru"))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Торговый центр не найден: unknown");
  }
}
