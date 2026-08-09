package com.kivo.backend.kiosks.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kivo.backend.common.exception.service.KioskNotFoundException;
import com.kivo.backend.testsupport.PostgreSqlIntegrationTest;
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
class KioskBootstrapServiceIntegrationTest extends PostgreSqlIntegrationTest {

  @Autowired private KioskBootstrapService kioskBootstrapService;

  @Test
  void returnsFlatSortedBootstrapUsingRequestedLanguage() {
    var bootstrap = kioskBootstrapService.getBootstrap("demo-kiosk", "en");

    assertThat(bootstrap.kioskCode()).isEqualTo("demo-kiosk");
    assertThat(bootstrap.mallCode()).isEqualTo("demo-mall");
    assertThat(bootstrap.defaultLanguage()).isEqualTo("ru");
    assertThat(bootstrap.selectedLanguage()).isEqualTo("en");
    assertThat(bootstrap.currentFloorCode()).isEqualTo("L1");
    assertThat(bootstrap.languages())
        .extracting(language -> language.code() + ":" + language.displayOrder())
        .containsExactly("ru:10", "tt:20", "en:30", "zh:40", "ar:50");
    assertThat(bootstrap.floors())
        .singleElement()
        .satisfies(
            floor -> {
              assertThat(floor.code()).isEqualTo("L1");
              assertThat(floor.mapImageUrl())
                  .isEqualTo("https://cdn.example.com/maps/demo-mall/L1.png");
              assertThat(floor.coordinateWidth()).isEqualTo(10000);
              assertThat(floor.coordinateHeight()).isEqualTo(10000);
            });
  }

  @Test
  void usesMallDefaultLanguageWhenParameterIsOmitted() {
    assertThat(kioskBootstrapService.getBootstrap("demo-kiosk", null).selectedLanguage())
        .isEqualTo("ru");
  }

  @Test
  void rejectsUnknownAndInactiveKiosks() {
    assertThatThrownBy(() -> kioskBootstrapService.getBootstrap("unknown", "ru"))
        .isInstanceOf(KioskNotFoundException.class);
    assertThatThrownBy(() -> kioskBootstrapService.getBootstrap("inactive-kiosk", "ru"))
        .isInstanceOf(KioskNotFoundException.class);
  }
}
