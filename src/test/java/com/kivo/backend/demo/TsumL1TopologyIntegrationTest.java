package com.kivo.backend.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import com.kivo.backend.navigation.application.ActiveGraphProvider;
import com.kivo.backend.navigation.domain.DefaultGraphValidator;
import com.kivo.backend.navigation.domain.model.GraphSnapshot;
import com.kivo.backend.navigation.domain.model.MovementType;
import com.kivo.backend.navigation.domain.routing.AStarShortestPath;
import com.kivo.backend.navigation.domain.routing.DijkstraShortestPath;
import com.kivo.backend.navigation.domain.routing.MovementProfile;
import com.kivo.backend.navigation.domain.routing.RouteRequest;
import com.kivo.backend.navigation.domain.routing.RoutingCostPolicy;
import com.kivo.backend.testsupport.PostgreSqlIntegrationTest;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("demo")
class TsumL1TopologyIntegrationTest extends PostgreSqlIntegrationTest {

  private static final UUID DEMO_MALL_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final RoutingCostPolicy COSTS = RoutingCostPolicy.defaults();

  @Autowired private ActiveGraphProvider activeGraphProvider;

  @Test
  void loadsValidReachablePublicTopologyWithoutUnverifiedFloorTransitions() {
    GraphSnapshot snapshot = activeGraphProvider.getRequired(DEMO_MALL_ID);

    assertThat(snapshot.versionCode()).isEqualTo("tsum-l1-demo-2026-07-16-v1");
    assertThat(snapshot.nodes()).hasSize(44);
    assertThat(snapshot.edges()).hasSize(94);
    assertThat(snapshot.places()).hasSize(16);
    assertThat(snapshot.kiosks())
        .singleElement()
        .satisfies(kiosk -> assertThat(kiosk.resourceCode()).isEqualTo("tsum-l1-main-kiosk"));
    assertThat(new DefaultGraphValidator().validate(snapshot).issues()).isEmpty();
    assertThat(snapshot.edges())
        .allSatisfy(
            edge -> {
              assertThat(edge.movementType()).isEqualTo(MovementType.CORRIDOR);
              assertThat(edge.wheelchairAccessible()).isTrue();
            });
  }

  @Test
  void realTopologyMatchesDijkstraAndStaysWithinPerformanceBudget() {
    GraphSnapshot snapshot = activeGraphProvider.getRequired(DEMO_MALL_ID);
    var dijkstra = new DijkstraShortestPath();
    var aStar = new AStarShortestPath();
    String start = snapshot.kiosks().getFirst().nodeCode();

    snapshot
        .places()
        .forEach(
            place -> {
              RouteRequest request =
                  new RouteRequest(start, place.nodeCode(), MovementProfile.STANDARD);
              var reference = dijkstra.findRoute(snapshot, request, COSTS).route();
              var optimized = aStar.findRoute(snapshot, request, COSTS).route();
              assertThat(optimized.totalDistanceMeters())
                  .isEqualByComparingTo(reference.totalDistanceMeters());
              assertThat(optimized.estimatedDuration()).isEqualTo(reference.estimatedDuration());
            });

    assertTimeoutPreemptively(
        Duration.ofSeconds(3),
        () -> {
          long checksum = 0;
          for (int iteration = 0; iteration < 250; iteration++) {
            for (var place : snapshot.places()) {
              var computation =
                  aStar.findRoute(
                      snapshot,
                      new RouteRequest(start, place.nodeCode(), MovementProfile.STANDARD),
                      COSTS);
              checksum += computation.exploredNodeCount();
            }
          }
          assertThat(checksum).isPositive();
        });
  }
}
