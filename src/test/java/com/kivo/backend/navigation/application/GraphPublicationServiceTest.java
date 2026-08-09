package com.kivo.backend.navigation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kivo.backend.navigation.domain.DefaultGraphValidator;
import com.kivo.backend.navigation.domain.model.GraphAnchor;
import com.kivo.backend.navigation.domain.model.GraphEdge;
import com.kivo.backend.navigation.domain.model.GraphNode;
import com.kivo.backend.navigation.domain.model.GraphPoint;
import com.kivo.backend.navigation.domain.model.GraphSnapshot;
import com.kivo.backend.navigation.domain.model.MovementType;
import com.kivo.backend.navigation.domain.model.NodeType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class GraphPublicationServiceTest {

  private static final UUID MALL_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
  private static final UUID OLD_VERSION_ID =
      UUID.fromString("40000000-0000-0000-0000-000000000002");
  private static final UUID NEW_VERSION_ID =
      UUID.fromString("40000000-0000-0000-0000-000000000003");
  private static final UUID FLOOR_ID = UUID.fromString("40000000-0000-0000-0000-000000000004");
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-08-10T10:00:00Z"), ZoneOffset.UTC);

  @Test
  void swapsRegistryOnlyAfterDatabasePublicationSucceeds() {
    GraphSnapshot oldSnapshot = validSnapshot(OLD_VERSION_ID);
    GraphSnapshot candidate = validSnapshot(NEW_VERSION_ID);
    InMemoryActiveGraphRegistry registry = new InMemoryActiveGraphRegistry();
    registry.replace(oldSnapshot);
    AtomicBoolean committed = new AtomicBoolean();
    SimpleMeterRegistry meters = new SimpleMeterRegistry();
    var service =
        service(
            candidate, (mallId, versionId, publishedAt) -> committed.set(true), registry, meters);

    GraphSnapshot published = service.publish(MALL_ID, NEW_VERSION_ID);

    assertThat(committed).isTrue();
    assertThat(published).isSameAs(candidate);
    assertThat(registry.getRequired(MALL_ID)).isSameAs(candidate);
    assertThat(meters.counter("kivo.navigation.graph.publication", "outcome", "success").count())
        .isEqualTo(1);
  }

  @Test
  void rejectsInvalidCandidateWithoutCallingDatabaseOrReplacingOldSnapshot() {
    GraphSnapshot oldSnapshot = validSnapshot(OLD_VERSION_ID);
    GraphSnapshot invalid =
        new GraphSnapshot(
            MALL_ID,
            NEW_VERSION_ID,
            "invalid",
            validSnapshot(NEW_VERSION_ID).nodes(),
            List.of(),
            List.of(new GraphAnchor("shop", "place")),
            List.of(new GraphAnchor("lobby", "kiosk")));
    InMemoryActiveGraphRegistry registry = new InMemoryActiveGraphRegistry();
    registry.replace(oldSnapshot);
    AtomicBoolean transactionCalled = new AtomicBoolean();
    SimpleMeterRegistry meters = new SimpleMeterRegistry();
    var service =
        service(
            invalid,
            (mallId, versionId, publishedAt) -> transactionCalled.set(true),
            registry,
            meters);

    assertThatThrownBy(() -> service.publish(MALL_ID, NEW_VERSION_ID))
        .isInstanceOf(GraphValidationException.class);

    assertThat(transactionCalled).isFalse();
    assertThat(registry.getRequired(MALL_ID)).isSameAs(oldSnapshot);
    assertThat(meters.counter("kivo.navigation.graph.publication", "outcome", "rejected").count())
        .isEqualTo(1);
  }

  @Test
  void databaseFailureLeavesOldSnapshotActiveAndRecordsError() {
    GraphSnapshot oldSnapshot = validSnapshot(OLD_VERSION_ID);
    GraphSnapshot candidate = validSnapshot(NEW_VERSION_ID);
    InMemoryActiveGraphRegistry registry = new InMemoryActiveGraphRegistry();
    registry.replace(oldSnapshot);
    SimpleMeterRegistry meters = new SimpleMeterRegistry();
    var service =
        service(
            candidate,
            (mallId, versionId, publishedAt) -> {
              throw new IllegalStateException("database unavailable");
            },
            registry,
            meters);

    assertThatThrownBy(() -> service.publish(MALL_ID, NEW_VERSION_ID))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("database unavailable");

    assertThat(registry.getRequired(MALL_ID)).isSameAs(oldSnapshot);
    assertThat(meters.counter("kivo.navigation.graph.publication", "outcome", "error").count())
        .isEqualTo(1);
  }

  private static DefaultGraphPublicationService service(
      GraphSnapshot candidate,
      GraphPublicationTransaction transaction,
      InMemoryActiveGraphRegistry registry,
      SimpleMeterRegistry meters) {
    return new DefaultGraphPublicationService(
        (mallId, versionId) -> candidate,
        new DefaultGraphValidator(),
        transaction,
        registry,
        meters,
        CLOCK);
  }

  private static GraphSnapshot validSnapshot(UUID versionId) {
    GraphNode kiosk =
        new GraphNode("kiosk", FLOOR_ID, MALL_ID, 10000, 10000, 100, 100, NodeType.KIOSK);
    GraphNode place =
        new GraphNode("place", FLOOR_ID, MALL_ID, 10000, 10000, 200, 200, NodeType.PLACE_ACCESS);
    GraphEdge edge =
        new GraphEdge(
            "edge",
            "kiosk",
            "place",
            BigDecimal.TEN,
            MovementType.CORRIDOR,
            false,
            true,
            List.of(new GraphPoint(100, 100), new GraphPoint(200, 200)));
    return new GraphSnapshot(
        MALL_ID,
        versionId,
        versionId.toString(),
        List.of(kiosk, place),
        List.of(edge),
        List.of(new GraphAnchor("shop", "place")),
        List.of(new GraphAnchor("lobby", "kiosk")));
  }
}
