package com.kivo.backend.navigation.domain.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import com.kivo.backend.navigation.domain.model.GraphEdge;
import com.kivo.backend.navigation.domain.model.GraphNode;
import com.kivo.backend.navigation.domain.model.GraphPoint;
import com.kivo.backend.navigation.domain.model.GraphSnapshot;
import com.kivo.backend.navigation.domain.model.MovementType;
import com.kivo.backend.navigation.domain.model.NodeType;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AStarShortestPathTest {

  private static final UUID MALL_ID = UUID.fromString("51000000-0000-0000-0000-000000000001");
  private static final UUID VERSION_ID = UUID.fromString("51000000-0000-0000-0000-000000000002");
  private static final UUID FLOOR_ID = UUID.fromString("51000000-0000-0000-0000-000000000003");
  private static final RoutingCostPolicy COSTS = RoutingCostPolicy.defaults();

  private final ShortestPathAlgorithm dijkstra = new DijkstraShortestPath();
  private final ShortestPathAlgorithm aStar = new AStarShortestPath();

  @Test
  void matchesDijkstraOnDeterministicGeneratedGraphs() {
    for (int seed = 0; seed < 30; seed++) {
      GraphSnapshot snapshot = generatedGraph(seed, 40);
      RouteRequest request = new RouteRequest("n0", "n39", MovementProfile.STANDARD);

      RouteResult expected = dijkstra.findRoute(snapshot, request, COSTS).route();
      RouteResult actual = aStar.findRoute(snapshot, request, COSTS).route();

      assertThat(actual.estimatedDuration())
          .as("seed %s", seed)
          .isEqualTo(expected.estimatedDuration());
      assertThat(actual.totalDistanceMeters())
          .as("seed %s", seed)
          .isEqualByComparingTo(expected.totalDistanceMeters());
    }
  }

  @Test
  void usesZeroHeuristicSafelyAcrossFloors() {
    UUID secondFloor = UUID.fromString("51000000-0000-0000-0000-000000000004");
    GraphSnapshot snapshot =
        snapshot(
            List.of(node("start", FLOOR_ID, 0, 0), node("target", secondFloor, 100, 100)),
            List.of(
                new GraphEdge(
                    "lift",
                    "start",
                    "target",
                    BigDecimal.ONE,
                    MovementType.ELEVATOR,
                    false,
                    true,
                    List.of())));
    RouteRequest request = new RouteRequest("start", "target", MovementProfile.STANDARD);

    assertThat(aStar.findRoute(snapshot, request, COSTS).route().estimatedDuration())
        .isEqualTo(dijkstra.findRoute(snapshot, request, COSTS).route().estimatedDuration());
  }

  @Test
  void realisticGridCompletesWithinBudgetAndExploresNoMoreThanDijkstra() {
    GraphSnapshot grid = grid(60, 60);
    RouteRequest request = new RouteRequest("n-0-0", "n-59-59", MovementProfile.STANDARD);

    assertTimeoutPreemptively(
        Duration.ofSeconds(3),
        () -> {
          RouteComputation reference = dijkstra.findRoute(grid, request, COSTS);
          RouteComputation optimized = aStar.findRoute(grid, request, COSTS);

          assertThat(optimized.route().estimatedDuration())
              .isEqualTo(reference.route().estimatedDuration());
          assertThat(optimized.exploredNodeCount())
              .isLessThanOrEqualTo(reference.exploredNodeCount());
        });
  }

  private static GraphSnapshot generatedGraph(int seed, int size) {
    Random random = new Random(seed);
    List<GraphNode> nodes = new ArrayList<>();
    List<GraphEdge> edges = new ArrayList<>();
    for (int index = 0; index < size; index++) {
      nodes.add(node("n" + index, FLOOR_ID, index * 10, (index % 5) * 10));
      if (index > 0) {
        edges.add(edge("line-" + index, "n" + (index - 1), "n" + index, 2));
      }
    }
    for (int index = 0; index < size - 2; index++) {
      int target = index + 2 + random.nextInt(Math.min(6, size - index - 1) - 1);
      int dx = (target - index) * 10;
      int dy = ((target % 5) - (index % 5)) * 10;
      int length = (int) Math.ceil(Math.hypot(dx, dy) / 5.0) + random.nextInt(4);
      edges.add(edge("shortcut-" + index, "n" + index, "n" + target, length));
    }
    return snapshot(nodes, edges);
  }

  private static GraphSnapshot grid(int width, int height) {
    List<GraphNode> nodes = new ArrayList<>(width * height);
    List<GraphEdge> edges = new ArrayList<>((width - 1) * height * 2 + (height - 1) * width * 2);
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        nodes.add(node(code(x, y), FLOOR_ID, x * 10, y * 10));
        if (x > 0) {
          edges.add(edge("e-l-" + x + '-' + y, code(x, y), code(x - 1, y), 1));
          edges.add(edge("e-r-" + x + '-' + y, code(x - 1, y), code(x, y), 1));
        }
        if (y > 0) {
          edges.add(edge("e-u-" + x + '-' + y, code(x, y), code(x, y - 1), 1));
          edges.add(edge("e-d-" + x + '-' + y, code(x, y - 1), code(x, y), 1));
        }
      }
    }
    return snapshot(nodes, edges);
  }

  private static String code(int x, int y) {
    return "n-" + x + '-' + y;
  }

  private static GraphNode node(String code, UUID floorId, int x, int y) {
    return new GraphNode(code, floorId, MALL_ID, 10000, 10000, x, y, NodeType.WAYPOINT);
  }

  private static GraphEdge edge(String code, String from, String to, int length) {
    return new GraphEdge(
        code,
        from,
        to,
        BigDecimal.valueOf(length),
        MovementType.CORRIDOR,
        false,
        true,
        List.of(new GraphPoint(0, 0), new GraphPoint(1, 1)));
  }

  private static GraphSnapshot snapshot(List<GraphNode> nodes, List<GraphEdge> edges) {
    return new GraphSnapshot(MALL_ID, VERSION_ID, "astar-test", nodes, edges, List.of(), List.of());
  }
}
