package com.kivo.backend.navigation.domain;

import com.kivo.backend.navigation.domain.model.GraphAnchor;
import com.kivo.backend.navigation.domain.model.GraphEdge;
import com.kivo.backend.navigation.domain.model.GraphNode;
import com.kivo.backend.navigation.domain.model.GraphPoint;
import com.kivo.backend.navigation.domain.model.GraphSnapshot;
import com.kivo.backend.navigation.domain.model.GraphValidationCode;
import com.kivo.backend.navigation.domain.model.GraphValidationIssue;
import com.kivo.backend.navigation.domain.model.GraphValidationResult;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DefaultGraphValidator implements GraphValidator {

  @Override
  public GraphValidationResult validate(GraphSnapshot snapshot) {
    List<GraphValidationIssue> issues = new ArrayList<>();
    snapshot.duplicateNodeCodes().stream()
        .sorted()
        .forEach(
            code ->
                add(issues, GraphValidationCode.DUPLICATE_NODE_CODE, code, "Duplicate node code"));
    snapshot.duplicateEdgeCodes().stream()
        .sorted()
        .forEach(
            code ->
                add(issues, GraphValidationCode.DUPLICATE_EDGE_CODE, code, "Duplicate edge code"));

    snapshot.nodes().forEach(node -> validateNode(snapshot, node, issues));
    snapshot.edges().forEach(edge -> validateEdge(snapshot, edge, issues));
    validateAnchors(snapshot, snapshot.places(), true, issues);
    validateAnchors(snapshot, snapshot.kiosks(), false, issues);
    validateReachability(snapshot, issues);
    return new GraphValidationResult(issues);
  }

  private static void validateNode(
      GraphSnapshot snapshot, GraphNode node, List<GraphValidationIssue> issues) {
    if (!snapshot.mallId().equals(node.floorMallId())) {
      add(
          issues,
          GraphValidationCode.FLOOR_MALL_MISMATCH,
          node.code(),
          "Node floor belongs to another mall");
    }
    if (node.coordinateWidth() <= 0
        || node.coordinateHeight() <= 0
        || node.x() < 0
        || node.y() < 0
        || node.x() > Math.min(10000, node.coordinateWidth())
        || node.y() > Math.min(10000, node.coordinateHeight())) {
      add(
          issues,
          GraphValidationCode.NODE_OUT_OF_BOUNDS,
          node.code(),
          "Node coordinates are outside floor bounds");
    }
  }

  private static void validateEdge(
      GraphSnapshot snapshot, GraphEdge edge, List<GraphValidationIssue> issues) {
    GraphNode from = snapshot.nodesByCode().get(edge.fromNodeCode());
    GraphNode to = snapshot.nodesByCode().get(edge.toNodeCode());
    if (from == null || to == null) {
      add(
          issues,
          GraphValidationCode.EDGE_ENDPOINT_NOT_FOUND,
          edge.code(),
          "Edge endpoint does not exist");
      return;
    }
    if (edge.lengthMeters() == null || edge.lengthMeters().compareTo(BigDecimal.ZERO) <= 0) {
      add(
          issues,
          GraphValidationCode.EDGE_LENGTH_INVALID,
          edge.code(),
          "Edge length must be positive");
    }

    boolean interFloor = !from.floorId().equals(to.floorId());
    if (interFloor) {
      if (!edge.movementType().isFloorTransition()) {
        add(
            issues,
            GraphValidationCode.INTER_FLOOR_MOVEMENT_INVALID,
            edge.code(),
            "Inter-floor edge must use a transition movement type");
      }
      if (!edge.geometry().isEmpty()) {
        add(
            issues,
            GraphValidationCode.INTER_FLOOR_GEOMETRY_NOT_EMPTY,
            edge.code(),
            "Inter-floor edge must not contain 2D geometry");
      }
      return;
    }

    if (edge.geometry().size() < 2) {
      add(
          issues,
          GraphValidationCode.EDGE_GEOMETRY_TOO_SHORT,
          edge.code(),
          "Same-floor edge requires at least two geometry points");
      return;
    }
    GraphPoint first = edge.geometry().getFirst();
    GraphPoint last = edge.geometry().getLast();
    if (!first.equals(from.point()) || !last.equals(to.point())) {
      add(
          issues,
          GraphValidationCode.EDGE_GEOMETRY_ENDPOINT_MISMATCH,
          edge.code(),
          "Geometry endpoints must match edge nodes");
    }
    if (edge.geometry().stream().anyMatch(point -> outsideFloor(point, from))) {
      add(
          issues,
          GraphValidationCode.EDGE_GEOMETRY_OUT_OF_BOUNDS,
          edge.code(),
          "Geometry point is outside floor bounds");
    }
  }

  private static boolean outsideFloor(GraphPoint point, GraphNode node) {
    return point.x() < 0
        || point.y() < 0
        || point.x() > Math.min(10000, node.coordinateWidth())
        || point.y() > Math.min(10000, node.coordinateHeight());
  }

  private static void validateAnchors(
      GraphSnapshot snapshot,
      List<GraphAnchor> anchors,
      boolean place,
      List<GraphValidationIssue> issues) {
    GraphValidationCode code =
        place ? GraphValidationCode.PLACE_NODE_NOT_FOUND : GraphValidationCode.KIOSK_NODE_NOT_FOUND;
    anchors.stream()
        .filter(
            anchor ->
                anchor.nodeCode() == null || !snapshot.nodesByCode().containsKey(anchor.nodeCode()))
        .forEach(
            anchor -> add(issues, code, anchor.resourceCode(), "Referenced node does not exist"));
  }

  private static void validateReachability(
      GraphSnapshot snapshot, List<GraphValidationIssue> issues) {
    List<GraphAnchor> validPlaces =
        snapshot.places().stream()
            .filter(anchor -> snapshot.nodesByCode().containsKey(anchor.nodeCode()))
            .toList();
    snapshot.kiosks().stream()
        .filter(anchor -> snapshot.nodesByCode().containsKey(anchor.nodeCode()))
        .forEach(
            kiosk -> {
              Set<String> reachable = reachableFrom(snapshot, kiosk.nodeCode());
              validPlaces.stream()
                  .filter(place -> !reachable.contains(place.nodeCode()))
                  .forEach(
                      place ->
                          add(
                              issues,
                              GraphValidationCode.PLACE_UNREACHABLE,
                              kiosk.resourceCode() + "->" + place.resourceCode(),
                              "Place is not reachable from kiosk"));
            });
  }

  private static Set<String> reachableFrom(GraphSnapshot snapshot, String startNodeCode) {
    Set<String> visited = new HashSet<>();
    ArrayDeque<String> queue = new ArrayDeque<>();
    visited.add(startNodeCode);
    queue.add(startNodeCode);
    while (!queue.isEmpty()) {
      String current = queue.removeFirst();
      snapshot.outgoingEdges().getOrDefault(current, List.of()).stream()
          .filter(edge -> !edge.closed())
          .map(GraphEdge::toNodeCode)
          .filter(snapshot.nodesByCode()::containsKey)
          .filter(visited::add)
          .forEach(queue::addLast);
    }
    return visited;
  }

  private static void add(
      List<GraphValidationIssue> issues,
      GraphValidationCode code,
      String subjectCode,
      String message) {
    issues.add(new GraphValidationIssue(code, subjectCode, message));
  }
}
