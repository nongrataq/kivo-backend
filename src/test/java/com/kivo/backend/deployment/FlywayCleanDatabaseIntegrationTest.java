package com.kivo.backend.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Arrays;
import java.util.Comparator;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class FlywayCleanDatabaseIntegrationTest {

  @Container
  private static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer(
          DockerImageName.parse(
                  "postgres:17.10-alpine3.24@sha256:742f40ea20b9ff2ff31db5458d127452988a2164df9e17441e191f3b72252193")
              .asCompatibleSubstituteFor("postgres"));

  @Test
  void migratesEmptyDatabaseToLatestResolvedVersionAndValidatesIt() {
    Flyway flyway =
        Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .locations("classpath:db/migration")
            .load();

    assertThat(flyway.info().current()).isNull();

    flyway.migrate();

    assertThatCode(flyway::validate).doesNotThrowAnyException();
    assertThat(flyway.info().pending()).isEmpty();

    MigrationInfo current = flyway.info().current();
    MigrationVersion latestResolvedVersion =
        Arrays.stream(flyway.info().all())
            .map(MigrationInfo::getVersion)
            .filter(version -> version != null)
            .max(Comparator.naturalOrder())
            .orElseThrow();

    assertThat(current).isNotNull();
    assertThat(current.getVersion()).isEqualTo(latestResolvedVersion);
  }
}
