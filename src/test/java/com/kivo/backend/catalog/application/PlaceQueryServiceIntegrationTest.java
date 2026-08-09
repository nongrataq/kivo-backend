package com.kivo.backend.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kivo.backend.common.exception.service.MallNotFoundException;
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
@Sql("/sql/category-test-data.sql")
class PlaceQueryServiceIntegrationTest {

  private static final UUID FOOD_CATEGORY_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000101");

  @Autowired private PlaceQueryService placeQueryService;

  @Test
  void returnsOnlyActivePlacesInDisplayOrder() {
    var places = placeQueryService.findPlaces("demo-mall", null, null, "ru");

    assertThat(places)
        .extracting(place -> place.code() + ":" + place.name())
        .containsExactly(
            "grocery:Продуктовый магазин", "boutique:Бутик", "information:information");
    assertThat(places.getFirst().floorCode()).isEqualTo("L1");
    assertThat(places.getFirst().categoryCode()).isEqualTo("food");
  }

  @Test
  void filtersByCategoryAndSearchesAcrossTranslations() {
    var places = placeQueryService.findPlaces("demo-mall", "GROCERY", FOOD_CATEGORY_ID, "ru");

    assertThat(places)
        .singleElement()
        .satisfies(place -> assertThat(place.code()).isEqualTo("grocery"));
  }

  @Test
  void searchesDescriptionsIgnoringCase() {
    var places = placeQueryService.findPlaces("demo-mall", "АкСеСсУаР", null, "ru");

    assertThat(places)
        .singleElement()
        .satisfies(place -> assertThat(place.code()).isEqualTo("boutique"));
  }

  @Test
  void fallsBackToMallDefaultLanguageAndThenCode() {
    var places = placeQueryService.findPlaces("demo-mall", null, null, "en");

    assertThat(places)
        .extracting(place -> place.code() + ":" + place.name())
        .containsExactly("grocery:Grocery store", "boutique:Бутик", "information:information");
  }

  @Test
  void returnsEmptyListWhenNothingMatches() {
    assertThat(placeQueryService.findPlaces("demo-mall", "missing", null, "ru")).isEmpty();
  }

  @Test
  void rejectsUnknownMall() {
    assertThatThrownBy(() -> placeQueryService.findPlaces("unknown", null, null, "ru"))
        .isInstanceOf(MallNotFoundException.class);
  }
}
