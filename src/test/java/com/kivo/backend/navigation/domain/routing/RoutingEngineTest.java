package com.kivo.backend.navigation.domain.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kivo.backend.navigation.domain.model.GraphEdge;
import com.kivo.backend.navigation.domain.model.GraphNode;
import com.kivo.backend.navigation.domain.model.GraphPoint;
import com.kivo.backend.navigation.domain.model.GraphSnapshot;
import com.kivo.backend.navigation.domain.model.MovementType;
import com.kivo.backend.navigation.domain.model.NodeType;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RoutingEngineTest {

  private static final UUID MALL_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
  private static final UUID VERSION_ID = UUID.fromString("50000000-0000-0000-0000-000000000002");
  private static final UUID FLOOR_ONE = UUID.fromString("50000000-0000-0000-0000-000000000003");
  private static final UUID FLOOR_TWO = UUID.fromString("50000000-0000-0000-0000-000000000004");

  private final RoutingCostPolicy costs = RoutingCostPolicy.defaults();
  private final ShortestPathAlgorithm dijkstra = new DijkstraShortestPath();

  @Test
  void computesTimeCostAndFiltersAccessibleEdges() {
    GraphEdge corridor = edge("corridor", "a", "b", 14, MovementType.CORRIDOR, false, true);
    GraphEdge stairs = edge("stairs", "a", "b", 14, MovementType.STAIRS, false, true);

    assertThat(costs.travelSeconds(corridor, MovementProfile.STANDARD)).isEqualTo(10.0);
    assertThat(costs.travelSeconds(stairs, MovementProfile.STANDARD)).isEqualTo(30.0);
    assertThat(costs.isAllowed(stairs, MovementProfile.ACCESSIBLE)).isFalse();
    assertThat(
            costs.isAllowed(
                edge("blocked", "a", "b", 14, MovementType.CORRIDOR, false, false),
                MovementProfile.ACCESSIBLE))
        .isFalse();
    assertThat(
            costs.isAllowed(
                edge("closed", "a", "b", 14, MovementType.CORRIDOR, true, true),
                MovementProfile.STANDARD))
        .isFalse();
  }

  @Test
  void dijkstraBuildsMetricsStepsAndStructuredInstructions() {
    List<GraphNode> nodes =
        List.of(
            node("start", FLOOR_ONE, 0, 0),
            node("hall", FLOOR_ONE, 100, 0),
            node("lift-up", FLOOR_ONE, 200, 0),
            node("lift-down", FLOOR_TWO, 200, 0),
            node("target", FLOOR_TWO, 300, 0));
    List<GraphEdge> edges =
        List.of(
            edge("e1", "start", "hall", 10, MovementType.CORRIDOR, false, true),
            edge("e2", "hall", "lift-up", 10, MovementType.CORRIDOR, false, true),
            edge("e3", "lift-up", "lift-down", 1, MovementType.ELEVATOR, false, true),
            edge("e4", "lift-down", "target", 10, MovementType.CORRIDOR, false, true));

    RouteResult route =
        dijkstra
            .findRoute(
                snapshot(nodes, edges),
                new RouteRequest("start", "target", MovementProfile.STANDARD),
                costs)
            .route();

    assertThat(route.totalDistanceMeters()).isEqualByComparingTo("31");
    assertThat(route.estimatedDuration()).isEqualTo(Duration.ofMillis(67143));
    assertThat(route.steps())
        .extracting(RouteStep::edgeCode)
        .containsExactly("e1", "e2", "e3", "e4");
    assertThat(route.instructions())
        .extracting(RouteInstruction::type)
        .containsExactly(
            InstructionType.START,
            InstructionType.CONTINUE,
            InstructionType.TRANSITION,
            InstructionType.CONTINUE,
            InstructionType.ARRIVE);
    assertThat(route.instructions().get(1))
        .isEqualTo(
            new RouteInstruction.Continue(
                FLOOR_ONE, "start", "lift-up", MovementType.CORRIDOR, new BigDecimal("20")));
  }

  @Test
  void respectsDirectionAndClosedEdges() {
    GraphSnapshot snapshot =
        snapshot(
            List.of(node("a", FLOOR_ONE, 0, 0), node("b", FLOOR_ONE, 100, 0)),
            List.of(edge("a-to-b", "a", "b", 10, MovementType.CORRIDOR, true, true)));

    assertThatThrownBy(
            () ->
                dijkstra.findRoute(
                    snapshot, new RouteRequest("a", "b", MovementProfile.STANDARD), costs))
        .isInstanceOf(RouteNotFoundException.class);
    assertThatThrownBy(
            () ->
                dijkstra.findRoute(
                    snapshot, new RouteRequest("b", "a", MovementProfile.STANDARD), costs))
        .isInstanceOf(RouteNotFoundException.class);
  }

  @Test
  void accessibleProfileChoosesElevatorInsteadOfStairs() {
    List<GraphNode> nodes =
        List.of(
            node("start", FLOOR_ONE, 0, 0),
            node("stairs", FLOOR_TWO, 100, 0),
            node("elevator", FLOOR_TWO, 200, 0),
            node("target", FLOOR_TWO, 300, 0));
    List<GraphEdge> edges =
        List.of(
            edge("stairs", "start", "stairs", 1, MovementType.STAIRS, false, true),
            edge("stairs-target", "stairs", "target", 1, MovementType.CORRIDOR, false, true),
            edge("elevator", "start", "elevator", 1, MovementType.ELEVATOR, false, true),
            edge("elevator-target", "elevator", "target", 1, MovementType.CORRIDOR, false, true));
    GraphSnapshot snapshot = snapshot(nodes, edges);

    RouteResult standard =
        dijkstra
            .findRoute(
                snapshot, new RouteRequest("start", "target", MovementProfile.STANDARD), costs)
            .route();
    RouteResult accessible =
        dijkstra
            .findRoute(
                snapshot, new RouteRequest("start", "target", MovementProfile.ACCESSIBLE), costs)
            .route();

    assertThat(standard.steps())
        .extracting(RouteStep::edgeCode)
        .containsExactly("stairs", "stairs-target");
    assertThat(accessible.steps())
        .extracting(RouteStep::edgeCode)
        .containsExactly("elevator", "elevator-target");
  }

  @Test
  void tieBreakIsStableRegardlessOfInputOrder() {
    List<GraphNode> nodes =
        List.of(
            node("start", FLOOR_ONE, 0, 0),
            node("a", FLOOR_ONE, 100, 0),
            node("b", FLOOR_ONE, 100, 100),
            node("target", FLOOR_ONE, 200, 0));
    List<GraphEdge> edges =
        new ArrayList<>(
            List.of(
                edge("start-b", "start", "b", 10, MovementType.CORRIDOR, false, true),
                edge("b-target", "b", "target", 10, MovementType.CORRIDOR, false, true),
                edge("start-a", "start", "a", 10, MovementType.CORRIDOR, false, true),
                edge("a-target", "a", "target", 10, MovementType.CORRIDOR, false, true)));
    GraphSnapshot first = snapshot(nodes, edges);
    Collections.reverse(edges);
    GraphSnapshot second = snapshot(nodes, edges);

    assertThat(routeEdgeCodes(dijkstra, first)).containsExactly("start-a", "a-target");
    assertThat(routeEdgeCodes(dijkstra, second)).containsExactly("start-a", "a-target");
  }

  @Test
  void sameStartAndDestinationReturnsZeroRoute() {
    GraphSnapshot snapshot = snapshot(List.of(node("same", FLOOR_ONE, 0, 0)), List.of());

    RouteResult route =
        dijkstra
            .findRoute(snapshot, new RouteRequest("same", "same", MovementProfile.STANDARD), costs)
            .route();

    assertThat(route.totalDistanceMeters()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(route.estimatedDuration()).isZero();
    assertThat(route.steps()).isEmpty();
    assertThat(route.instructions())
        .containsExactly(new RouteInstruction.Arrive(FLOOR_ONE, "same"));
  }

  private static List<String> routeEdgeCodes(
      ShortestPathAlgorithm algorithm, GraphSnapshot snapshot) {
    return algorithm
        .findRoute(
            snapshot,
            new RouteRequest("start", "target", MovementProfile.STANDARD),
            RoutingCostPolicy.defaults())
        .route()
        .steps()
        .stream()
        .map(RouteStep::edgeCode)
        .toList();
  }

  private static GraphNode node(String code, UUID floorId, int x, int y) {
    return new GraphNode(code, floorId, MALL_ID, 10000, 10000, x, y, NodeType.WAYPOINT);
  }

  private static GraphEdge edge(
      String code,
      String from,
      String to,
      int length,
      MovementType type,
      boolean closed,
      boolean accessible) {
    List<GraphPoint> geometry =
        type.isFloorTransition() ? List.of() : List.of(new GraphPoint(0, 0), new GraphPoint(1, 1));
    return new GraphEdge(
        code, from, to, BigDecimal.valueOf(length), type, closed, accessible, geometry);
  }

  private static GraphSnapshot snapshot(List<GraphNode> nodes, List<GraphEdge> edges) {
    return new GraphSnapshot(
        MALL_ID, VERSION_ID, "routing-test", nodes, edges, List.of(), List.of());
  }
}
