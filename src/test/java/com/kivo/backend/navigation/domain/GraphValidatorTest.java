package com.kivo.backend.navigation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kivo.backend.navigation.domain.model.GraphAnchor;
import com.kivo.backend.navigation.domain.model.GraphEdge;
import com.kivo.backend.navigation.domain.model.GraphNode;
import com.kivo.backend.navigation.domain.model.GraphPoint;
import com.kivo.backend.navigation.domain.model.GraphSnapshot;
import com.kivo.backend.navigation.domain.model.GraphValidationCode;
import com.kivo.backend.navigation.domain.model.MovementType;
import com.kivo.backend.navigation.domain.model.NodeType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GraphValidatorTest {

  private static final UUID MALL_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
  private static final UUID VERSION_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
  private static final UUID FLOOR_ONE_ID = UUID.fromString("20000000-0000-0000-0000-000000000003");
  private static final UUID FLOOR_TWO_ID = UUID.fromString("20000000-0000-0000-0000-000000000004");

  private final DefaultGraphValidator validator = new DefaultGraphValidator();

  @Test
  void acceptsValidReachableGraph() {
    assertThat(validator.validate(validSnapshot()).isValid()).isTrue();
  }

  @Test
  void snapshotCollectionsAreDeeplyImmutable() {
    List<GraphNode> nodes = new ArrayList<>(validNodes());
    List<GraphPoint> geometry = new ArrayList<>(List.of(point(100, 100), point(200, 200)));
    GraphEdge edge = edge("edge", "kiosk", "place", geometry);
    GraphSnapshot snapshot = snapshot(nodes, List.of(edge), places("place"), kiosks("kiosk"));

    nodes.clear();
    geometry.clear();

    assertThat(snapshot.nodesByCode()).hasSize(2);
    assertThat(snapshot.edgesByCode().get("edge").geometry()).hasSize(2);
    assertThatThrownBy(() -> snapshot.nodesByCode().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> snapshot.outgoingEdges().get("kiosk").clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void reportsDuplicateCodesAndUnknownEndpoints() {
    var duplicate = node("kiosk", FLOOR_ONE_ID, 300, 300, NodeType.WAYPOINT);
    var edge = edge("broken", "missing", "place", List.of(point(100, 100), point(200, 200)));
    GraphSnapshot snapshot =
        snapshot(
            List.of(validNodes().get(0), validNodes().get(1), duplicate),
            List.of(edge, edge),
            places("place"),
            kiosks("kiosk"));

    assertThat(validator.validate(snapshot).issues())
        .extracting(issue -> issue.code())
        .contains(
            GraphValidationCode.DUPLICATE_NODE_CODE,
            GraphValidationCode.DUPLICATE_EDGE_CODE,
            GraphValidationCode.EDGE_ENDPOINT_NOT_FOUND);
  }

  @Test
  void reportsCoordinatesLengthAndSameFloorGeometryErrors() {
    var outside = node("kiosk", FLOOR_ONE_ID, 10001, 100, NodeType.KIOSK);
    var invalidEdge =
        new GraphEdge(
            "edge",
            "kiosk",
            "place",
            BigDecimal.ZERO,
            MovementType.CORRIDOR,
            false,
            true,
            List.of(point(101, 100), point(10001, 200)));
    GraphSnapshot snapshot =
        snapshot(
            List.of(outside, validNodes().get(1)),
            List.of(invalidEdge),
            places("place"),
            kiosks("kiosk"));

    assertThat(validator.validate(snapshot).issues())
        .extracting(issue -> issue.code())
        .contains(
            GraphValidationCode.NODE_OUT_OF_BOUNDS,
            GraphValidationCode.EDGE_LENGTH_INVALID,
            GraphValidationCode.EDGE_GEOMETRY_ENDPOINT_MISMATCH,
            GraphValidationCode.EDGE_GEOMETRY_OUT_OF_BOUNDS);
  }

  @Test
  void reportsInvalidInterFloorEdges() {
    var destination = node("place", FLOOR_TWO_ID, 200, 200, NodeType.PLACE_ACCESS);
    var corridor = edge("corridor", "kiosk", "place", List.of());
    var elevatorWithGeometry =
        new GraphEdge(
            "elevator",
            "kiosk",
            "place",
            BigDecimal.TEN,
            MovementType.ELEVATOR,
            false,
            true,
            List.of(point(100, 100), point(200, 200)));
    GraphSnapshot snapshot =
        snapshot(
            List.of(validNodes().get(0), destination),
            List.of(corridor, elevatorWithGeometry),
            places("place"),
            kiosks("kiosk"));

    assertThat(validator.validate(snapshot).issues())
        .extracting(issue -> issue.code())
        .contains(
            GraphValidationCode.INTER_FLOOR_MOVEMENT_INVALID,
            GraphValidationCode.INTER_FLOOR_GEOMETRY_NOT_EMPTY);
  }

  @Test
  void reportsMissingAnchorsAndDirectedUnreachablePlaces() {
    GraphEdge reverse =
        edge("reverse", "place", "kiosk", List.of(point(200, 200), point(100, 100)));
    GraphSnapshot snapshot =
        snapshot(
            validNodes(),
            List.of(reverse),
            List.of(new GraphAnchor("shop", "place"), new GraphAnchor("orphan", "missing-place")),
            List.of(new GraphAnchor("lobby", "kiosk"), new GraphAnchor("bad", "missing-kiosk")));

    assertThat(validator.validate(snapshot).issues())
        .extracting(issue -> issue.code())
        .contains(
            GraphValidationCode.PLACE_NODE_NOT_FOUND,
            GraphValidationCode.KIOSK_NODE_NOT_FOUND,
            GraphValidationCode.PLACE_UNREACHABLE);
  }

  @Test
  void closedEdgesDoNotProvideReachability() {
    GraphEdge closed =
        new GraphEdge(
            "closed",
            "kiosk",
            "place",
            BigDecimal.TEN,
            MovementType.CORRIDOR,
            true,
            true,
            List.of(point(100, 100), point(200, 200)));

    assertThat(
            validator
                .validate(snapshot(validNodes(), List.of(closed), places("place"), kiosks("kiosk")))
                .issues())
        .extracting(issue -> issue.code())
        .contains(GraphValidationCode.PLACE_UNREACHABLE);
  }

  private static GraphSnapshot validSnapshot() {
    return snapshot(
        validNodes(),
        List.of(edge("edge", "kiosk", "place", List.of(point(100, 100), point(200, 200)))),
        places("place"),
        kiosks("kiosk"));
  }

  private static List<GraphNode> validNodes() {
    return List.of(
        node("kiosk", FLOOR_ONE_ID, 100, 100, NodeType.KIOSK),
        node("place", FLOOR_ONE_ID, 200, 200, NodeType.PLACE_ACCESS));
  }

  private static GraphNode node(String code, UUID floorId, int x, int y, NodeType type) {
    return new GraphNode(code, floorId, MALL_ID, 10000, 10000, x, y, type);
  }

  private static GraphEdge edge(String code, String from, String to, List<GraphPoint> geometry) {
    return new GraphEdge(
        code, from, to, BigDecimal.TEN, MovementType.CORRIDOR, false, true, geometry);
  }

  private static GraphPoint point(int x, int y) {
    return new GraphPoint(x, y);
  }

  private static List<GraphAnchor> places(String nodeCode) {
    return List.of(new GraphAnchor("shop", nodeCode));
  }

  private static List<GraphAnchor> kiosks(String nodeCode) {
    return List.of(new GraphAnchor("lobby", nodeCode));
  }

  private static GraphSnapshot snapshot(
      List<GraphNode> nodes,
      List<GraphEdge> edges,
      List<GraphAnchor> places,
      List<GraphAnchor> kiosks) {
    return new GraphSnapshot(MALL_ID, VERSION_ID, "v1", nodes, edges, places, kiosks);
  }
}
