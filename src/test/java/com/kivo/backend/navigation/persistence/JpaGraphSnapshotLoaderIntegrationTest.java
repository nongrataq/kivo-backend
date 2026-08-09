package com.kivo.backend.navigation.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.kivo.backend.navigation.application.GraphSnapshotLoader;
import com.kivo.backend.navigation.domain.DefaultGraphValidator;
import com.kivo.backend.navigation.domain.model.GraphEdge;
import com.kivo.backend.testsupport.PostgreSqlIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Sql({"/sql/category-test-data.sql", "/sql/navigation-test-data.sql"})
@Transactional
class JpaGraphSnapshotLoaderIntegrationTest extends PostgreSqlIntegrationTest {

  private static final UUID MALL_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID VERSION_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

  @Autowired private GraphSnapshotLoader loader;

  @Test
  void loadsDeterministicImmutableSnapshotThroughModuleReadPorts() {
    var snapshot = loader.load(MALL_ID, VERSION_ID);

    assertThat(snapshot.versionCode()).isEqualTo("test-v1");
    assertThat(snapshot.nodesByCode().keySet())
        .containsExactlyInAnyOrder(
            "node-kiosk", "node-grocery", "node-boutique", "node-information");
    assertThat(snapshot.edges())
        .extracting(GraphEdge::code)
        .containsExactly("edge-a", "edge-b", "edge-c");
    assertThat(snapshot.places())
        .extracting(anchor -> anchor.resourceCode() + ":" + anchor.nodeCode())
        .containsExactly(
            "grocery:node-grocery", "boutique:node-boutique", "information:node-information");
    assertThat(snapshot.kiosks())
        .extracting(anchor -> anchor.resourceCode() + ":" + anchor.nodeCode())
        .containsExactly("demo-kiosk:node-kiosk");
    assertThat(new DefaultGraphValidator().validate(snapshot).isValid()).isTrue();
  }
}
