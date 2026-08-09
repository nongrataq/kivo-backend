package com.kivo.backend.navigation.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.kivo.backend.navigation.application.ActiveGraphRestorer;
import com.kivo.backend.navigation.application.GraphPublicationService;
import com.kivo.backend.navigation.application.GraphSnapshotLoader;
import com.kivo.backend.navigation.application.InMemoryActiveGraphRegistry;
import com.kivo.backend.navigation.domain.GraphValidator;
import com.kivo.backend.navigation.domain.model.GraphVersionState;
import com.kivo.backend.navigation.persistence.repository.NavigationGraphPublicationRepository;
import com.kivo.backend.navigation.persistence.repository.NavigationGraphVersionRepository;
import com.kivo.backend.testsupport.PostgreSqlIntegrationTest;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@ActiveProfiles("test")
@Sql({"/sql/category-test-data.sql", "/sql/navigation-test-data.sql"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GraphPublicationIntegrationTest extends PostgreSqlIntegrationTest {

  private static final UUID MALL_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID VERSION_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

  @Autowired private GraphPublicationService publicationService;
  @Autowired private InMemoryActiveGraphRegistry activeGraphRegistry;
  @Autowired private NavigationGraphPublicationRepository publicationRepository;
  @Autowired private NavigationGraphVersionRepository graphVersionRepository;
  @Autowired private GraphSnapshotLoader snapshotLoader;
  @Autowired private GraphValidator graphValidator;

  @Test
  void concurrentFirstPublicationIsSerializedAndCanBeRestored() throws Exception {
    CountDownLatch start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var first = executor.submit(() -> publishAfter(start));
      var second = executor.submit(() -> publishAfter(start));
      start.countDown();

      assertThat(first.get(30, TimeUnit.SECONDS).versionId()).isEqualTo(VERSION_ID);
      assertThat(second.get(30, TimeUnit.SECONDS).versionId()).isEqualTo(VERSION_ID);
    }

    assertThat(publicationRepository.count()).isEqualTo(1);
    assertThat(graphVersionRepository.findById(VERSION_ID).orElseThrow().getState())
        .isEqualTo(GraphVersionState.VALIDATED);
    assertThat(activeGraphRegistry.getRequired(MALL_ID).versionId()).isEqualTo(VERSION_ID);

    InMemoryActiveGraphRegistry restoredRegistry = new InMemoryActiveGraphRegistry();
    new ActiveGraphRestorer(publicationRepository, snapshotLoader, graphValidator, restoredRegistry)
        .restoreAll();

    assertThat(restoredRegistry.getRequired(MALL_ID).versionId()).isEqualTo(VERSION_ID);
  }

  private com.kivo.backend.navigation.domain.model.GraphSnapshot publishAfter(
      CountDownLatch start) {
    try {
      if (!start.await(10, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Concurrent publication did not start in time");
      }
      return publicationService.publish(MALL_ID, VERSION_ID);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while publishing graph", exception);
    }
  }
}
