package com.kivo.backend.navigation.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kivo.backend.navigation.application.GraphPublicationTransaction;
import com.kivo.backend.navigation.domain.model.GraphVersionState;
import com.kivo.backend.navigation.persistence.repository.NavigationGraphPublicationRepository;
import com.kivo.backend.navigation.persistence.repository.NavigationGraphVersionRepository;
import com.kivo.backend.testsupport.PostgreSqlIntegrationTest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@ActiveProfiles("test")
@Sql({"/sql/category-test-data.sql", "/sql/navigation-test-data.sql"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GraphPublicationRollbackIntegrationTest extends PostgreSqlIntegrationTest {

  private static final UUID MALL_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID VERSION_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

  @Autowired private GraphPublicationTransaction publicationTransaction;
  @Autowired private NavigationGraphPublicationRepository publicationRepository;
  @Autowired private NavigationGraphVersionRepository graphVersionRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void databaseErrorRollsBackPublicationPointerAndVersionState() {
    jdbcTemplate.execute(
        """
        create function fail_graph_validation_update() returns trigger
        language plpgsql as $$
        begin
          raise exception 'forced graph publication failure';
        end;
        $$
        """);
    jdbcTemplate.execute(
        """
        create trigger trg_fail_graph_validation_update
        before update of state on navigation_graph_version
        for each row execute function fail_graph_validation_update()
        """);

    assertThatThrownBy(
            () ->
                publicationTransaction.publish(
                    MALL_ID, VERSION_ID, Instant.parse("2026-08-10T10:00:00Z")))
        .isInstanceOf(RuntimeException.class)
        .hasStackTraceContaining("forced graph publication failure");

    assertThat(publicationRepository.count()).isZero();
    assertThat(graphVersionRepository.findById(VERSION_ID).orElseThrow().getState())
        .isEqualTo(GraphVersionState.DRAFT);
  }
}
