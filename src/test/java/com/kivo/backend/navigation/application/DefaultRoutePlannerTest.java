package com.kivo.backend.navigation.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.kivo.backend.navigation.domain.model.GraphEdge;
import com.kivo.backend.navigation.domain.model.GraphNode;
import com.kivo.backend.navigation.domain.model.GraphPoint;
import com.kivo.backend.navigation.domain.model.GraphSnapshot;
import com.kivo.backend.navigation.domain.model.MovementType;
import com.kivo.backend.navigation.domain.model.NodeType;
import com.kivo.backend.navigation.domain.routing.AStarShortestPath;
import com.kivo.backend.navigation.domain.routing.MovementProfile;
import com.kivo.backend.navigation.domain.routing.RoutingCostPolicy;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DefaultRoutePlannerTest {

  private static final UUID MALL_ID = UUID.fromString("52000000-0000-0000-0000-000000000001");
  private static final UUID VERSION_ID = UUID.fromString("52000000-0000-0000-0000-000000000002");
  private static final UUID FLOOR_ID = UUID.fromString("52000000-0000-0000-0000-000000000003");

  @Test
  void plansAgainstCurrentlyActiveSnapshot() {
    InMemoryActiveGraphRegistry registry = new InMemoryActiveGraphRegistry();
    GraphSnapshot snapshot = snapshot();
    registry.replace(snapshot);
    RoutePlanner planner =
        new DefaultRoutePlanner(registry, new AStarShortestPath(), RoutingCostPolicy.defaults());

    var planned = planner.planWithSnapshot(MALL_ID, "start", "target", MovementProfile.STANDARD);
    var route = planned.route();

    assertThat(planned.snapshot()).isSameAs(snapshot);
    assertThat(route.mallId()).isEqualTo(MALL_ID);
    assertThat(route.graphVersionId()).isEqualTo(VERSION_ID);
    assertThat(route.steps()).singleElement().extracting(step -> step.edgeCode()).isEqualTo("edge");
  }

  private static GraphSnapshot snapshot() {
    GraphNode start = new GraphNode("start", FLOOR_ID, MALL_ID, 10000, 10000, 0, 0, NodeType.KIOSK);
    GraphNode target =
        new GraphNode("target", FLOOR_ID, MALL_ID, 10000, 10000, 100, 0, NodeType.PLACE_ACCESS);
    GraphEdge edge =
        new GraphEdge(
            "edge",
            "start",
            "target",
            BigDecimal.TEN,
            MovementType.CORRIDOR,
            false,
            true,
            List.of(new GraphPoint(0, 0), new GraphPoint(100, 0)));
    return new GraphSnapshot(
        MALL_ID,
        VERSION_ID,
        "planner",
        List.of(start, target),
        List.of(edge),
        List.of(),
        List.of());
  }
}
