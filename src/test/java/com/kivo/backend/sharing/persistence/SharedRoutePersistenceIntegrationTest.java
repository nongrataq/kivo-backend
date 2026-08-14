package com.kivo.backend.sharing.persistence;

import static com.kivo.backend.sharing.SharingTestFixtures.CREATED_AT;
import static com.kivo.backend.sharing.SharingTestFixtures.GRAPH_VERSION_ID;
import static com.kivo.backend.sharing.SharingTestFixtures.MALL_ID;
import static com.kivo.backend.sharing.SharingTestFixtures.ROUTE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kivo.backend.sharing.persistence.entity.SharedRoute;
import com.kivo.backend.sharing.persistence.repository.SharedRouteRepository;
import com.kivo.backend.testsupport.PostgreSqlIntegrationTest;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class SharedRoutePersistenceIntegrationTest extends PostgreSqlIntegrationTest {

  @Autowired private SharedRouteRepository repository;
  @Autowired private EntityManager entityManager;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void schemaMatchesHibernateAndActiveLookupHonorsExpiryAndRevocation() {
    assertThat(
            jdbcTemplate.queryForObject(
                """
                select data_type
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = 'shared_route'
                  and column_name = 'token_hash'
                """,
                String.class))
        .isEqualTo("character varying");
    assertThat(
            jdbcTemplate.queryForObject(
                """
                select character_maximum_length
                from information_schema.columns
                where table_schema = 'public'
                  and table_name = 'shared_route'
                  and column_name = 'token_hash'
                """,
                Integer.class))
        .isEqualTo(64);

    String hash = "a".repeat(64);
    SharedRoute route =
        repository.saveAndFlush(
            SharedRoute.create(
                ROUTE_ID,
                MALL_ID,
                GRAPH_VERSION_ID,
                hash,
                "{\"schemaVersion\":1}",
                CREATED_AT,
                CREATED_AT.plus(Duration.ofHours(2))));
    entityManager.clear();

    assertThat(
            repository.findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(
                hash, CREATED_AT.plus(Duration.ofHours(1))))
        .contains(route);
    assertThat(
            repository.findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(
                hash, CREATED_AT.plus(Duration.ofHours(2))))
        .isEmpty();

    SharedRoute loaded = repository.findById(route.getId()).orElseThrow();
    loaded.revoke(CREATED_AT.plusSeconds(30));
    repository.flush();
    entityManager.clear();

    assertThat(
            repository.findByTokenHashAndRevokedAtIsNullAndExpiresAtAfter(
                hash, CREATED_AT.plusSeconds(31)))
        .isEmpty();
  }

  @Test
  void databaseRejectsMalformedTokenHashes() {
    assertThatThrownBy(() -> insert(UUID.randomUUID(), "not-a-sha256-hash"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void databaseRejectsDuplicateTokenHashes() {
    insert(UUID.randomUUID(), "b".repeat(64));

    assertThatThrownBy(() -> insert(UUID.randomUUID(), "b".repeat(64)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private void insert(UUID id, String hash) {
    jdbcTemplate.update(
        """
        insert into shared_route
          (id, route_id, mall_id, graph_version_id, token_hash, snapshot_json, created_at, expires_at)
        values (?, ?, ?, ?, ?, '{}', ?, ?)
        """,
        id,
        ROUTE_ID,
        MALL_ID,
        GRAPH_VERSION_ID,
        hash,
        Timestamp.from(CREATED_AT),
        Timestamp.from(CREATED_AT.plus(Duration.ofHours(2))));
  }
}
