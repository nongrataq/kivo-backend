package com.kivo.backend.catalog.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kivo.backend.catalog.persistence.entity.Category;
import com.kivo.backend.catalog.persistence.entity.CategoryTranslation;
import com.kivo.backend.catalog.persistence.entity.Mall;
import com.kivo.backend.catalog.persistence.entity.MallLanguage;
import com.kivo.backend.catalog.persistence.repository.CategoryRepository;
import com.kivo.backend.catalog.persistence.repository.MallRepository;
import com.kivo.backend.catalog.persistence.repository.PlaceRepository;
import com.kivo.backend.testsupport.PostgreSqlIntegrationTest;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Sql("/sql/category-test-data.sql")
class CatalogPersistenceIntegrationTest extends PostgreSqlIntegrationTest {

  private static final UUID FOOD_CATEGORY_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000101");

  @Autowired private CategoryRepository categoryRepository;
  @Autowired private MallRepository mallRepository;
  @Autowired private PlaceRepository placeRepository;
  @Autowired private EntityManager entityManager;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void repositoriesPreserveOrderingAndSearchLimits() {
    assertThat(categoryRepository.findActiveByMallCode("demo-mall"))
        .extracting(Category::getCode)
        .containsExactly("food", "fashion");

    var placeIds =
        placeRepository.searchActiveIds(
            "demo-mall", "магазин", FOOD_CATEGORY_ID, PageRequest.of(0, 1));

    assertThat(placeIds).containsExactly(UUID.fromString("00000000-0000-0000-0000-000000000401"));
  }

  @Test
  void translationLifecycleUsesCascadeAndOrphanRemoval() {
    Category category = categoryRepository.findById(FOOD_CATEGORY_ID).orElseThrow();
    CategoryTranslation translation = CategoryTranslation.create("tt", "Гастрономия");

    category.addTranslation(translation);
    categoryRepository.flush();
    UUID translationId = translation.getId();
    assertThat(translationId).isNotNull();

    category.removeTranslation(translation);
    categoryRepository.flush();
    entityManager.clear();

    assertThat(entityManager.find(CategoryTranslation.class, translationId)).isNull();
  }

  @Test
  void mallLanguageLifecycleUsesCascadeAndOrphanRemoval() {
    Mall mall = mallRepository.findByCodeAndActiveTrue("demo-mall").orElseThrow();
    MallLanguage language = MallLanguage.create("de", "DE", 60);

    mall.addLanguage(language);
    mallRepository.flush();
    UUID languageId = language.getId();
    assertThat(languageId).isNotNull();

    mall.removeLanguage(language);
    mallRepository.flush();
    entityManager.clear();

    assertThat(entityManager.find(MallLanguage.class, languageId)).isNull();
  }

  @Test
  void rejectsPlaceLinkedToFloorFromAnotherMall() {
    insertSecondMallAndFloor();

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    insert into place
                      (id, mall_id, floor_id, category_id, code, display_order, active)
                    values (?, ?, ?, ?, ?, ?, true)
                    """,
                    UUID.fromString("00000000-0000-0000-0000-000000000701"),
                    UUID.fromString("00000000-0000-0000-0000-000000000001"),
                    UUID.fromString("00000000-0000-0000-0000-000000000702"),
                    FOOD_CATEGORY_ID,
                    "invalid-floor-mall",
                    100))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rejectsKioskLinkedToFloorFromAnotherMall() {
    insertSecondMallAndFloor();

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    insert into kiosk
                      (id, mall_id, floor_id, code, enabled)
                    values (?, ?, ?, ?, true)
                    """,
                    UUID.fromString("00000000-0000-0000-0000-000000000703"),
                    UUID.fromString("00000000-0000-0000-0000-000000000001"),
                    UUID.fromString("00000000-0000-0000-0000-000000000702"),
                    "invalid-kiosk"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rejectsPlaceLinkedToCategoryFromAnotherMall() {
    insertSecondMallAndFloor();
    jdbcTemplate.update(
        """
        insert into category (id, mall_id, code, display_order, active)
        values (?, ?, ?, ?, true)
        """,
        UUID.fromString("00000000-0000-0000-0000-000000000705"),
        UUID.fromString("00000000-0000-0000-0000-000000000700"),
        "second-category",
        10);

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    insert into place
                      (id, mall_id, floor_id, category_id, code, display_order, active)
                    values (?, ?, ?, ?, ?, ?, true)
                    """,
                    UUID.fromString("00000000-0000-0000-0000-000000000706"),
                    UUID.fromString("00000000-0000-0000-0000-000000000001"),
                    UUID.fromString("00000000-0000-0000-0000-000000000301"),
                    UUID.fromString("00000000-0000-0000-0000-000000000705"),
                    "invalid-category-mall",
                    100))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void kioskCodeIsGloballyUnique() {
    insertSecondMallAndFloor();

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    insert into kiosk
                      (id, mall_id, floor_id, code, enabled)
                    values (?, ?, ?, ?, true)
                    """,
                    UUID.fromString("00000000-0000-0000-0000-000000000704"),
                    UUID.fromString("00000000-0000-0000-0000-000000000700"),
                    UUID.fromString("00000000-0000-0000-0000-000000000702"),
                    "demo-kiosk"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private void insertSecondMallAndFloor() {
    jdbcTemplate.update(
        """
        insert into mall
          (id, code, name, default_language, timezone, active, created_at)
        values (?, ?, ?, ?, ?, true, current_timestamp)
        """,
        UUID.fromString("00000000-0000-0000-0000-000000000700"),
        "constraint-mall",
        "ТЦ для проверки ограничений",
        "ru",
        "Europe/Moscow");
    jdbcTemplate.update(
        """
        insert into floor
          (id, mall_id, code, name, level_number, display_order,
           coordinate_width, coordinate_height, active)
        values (?, ?, ?, ?, ?, ?, 10000, 10000, true)
        """,
        UUID.fromString("00000000-0000-0000-0000-000000000702"),
        UUID.fromString("00000000-0000-0000-0000-000000000700"),
        "L1",
        "Первый этаж",
        1,
        10);
  }
}
