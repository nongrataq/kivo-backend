package com.kivo.backend.navigation.domain.routing;

import com.kivo.backend.navigation.domain.model.GraphEdge;
import com.kivo.backend.navigation.domain.model.GraphNode;
import com.kivo.backend.navigation.domain.model.GraphSnapshot;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

abstract class BestFirstShortestPath implements ShortestPathAlgorithm {

  private static final double EPSILON = 1.0e-9;
  private static final Comparator<QueueState> QUEUE_ORDER =
      Comparator.comparingDouble(QueueState::estimatedTotalSeconds)
          .thenComparingDouble(QueueState::travelSeconds)
          .thenComparing(QueueState::distanceMeters)
          .thenComparingInt(QueueState::transitionCount)
          .thenComparing(QueueState::nodeCode);

  @Override
  public final RouteComputation findRoute(
      GraphSnapshot snapshot, RouteRequest request, RoutingCostPolicy costPolicy) {
    GraphNode start = requireNode(snapshot, request.startNodeCode(), request);
    GraphNode destination = requireNode(snapshot, request.destinationNodeCode(), request);
    if (start.code().equals(destination.code())) {
      return new RouteComputation(zeroRoute(snapshot, request, start), 0);
    }

    Heuristic heuristic = createHeuristic(snapshot, destination, request.profile(), costPolicy);
    Map<String, Label> best = new HashMap<>();
    PriorityQueue<QueueState> queue = new PriorityQueue<>(QUEUE_ORDER);
    Label startLabel = new Label(0.0, BigDecimal.ZERO, 0, null, null);
    best.put(start.code(), startLabel);
    queue.add(new QueueState(start.code(), 0.0, heuristic.estimate(start), BigDecimal.ZERO, 0));
    int explored = 0;

    while (!queue.isEmpty()) {
      QueueState current = queue.remove();
      Label currentBest = best.get(current.nodeCode());
      if (!matches(current, currentBest)) {
        continue;
      }
      explored++;
      if (current.nodeCode().equals(destination.code())) {
        List<GraphEdge> path = reconstruct(best, start.code(), destination.code());
        return new RouteComputation(
            RouteAssembler.assemble(snapshot, request, path, costPolicy), explored);
      }

      snapshot.outgoingEdges().getOrDefault(current.nodeCode(), List.of()).stream()
          .sorted(Comparator.comparing(GraphEdge::code))
          .filter(edge -> costPolicy.isAllowed(edge, request.profile()))
          .forEach(
              edge ->
                  relax(
                      snapshot,
                      request,
                      costPolicy,
                      heuristic,
                      current,
                      currentBest,
                      edge,
                      best,
                      queue));
    }
    throw new RouteNotFoundException(request);
  }

  protected abstract Heuristic createHeuristic(
      GraphSnapshot snapshot,
      GraphNode destination,
      MovementProfile profile,
      RoutingCostPolicy costPolicy);

  private static void relax(
      GraphSnapshot snapshot,
      RouteRequest request,
      RoutingCostPolicy costPolicy,
      Heuristic heuristic,
      QueueState current,
      Label currentBest,
      GraphEdge edge,
      Map<String, Label> best,
      PriorityQueue<QueueState> queue) {
    GraphNode next = snapshot.nodesByCode().get(edge.toNodeCode());
    if (next == null) {
      return;
    }
    double seconds =
        currentBest.travelSeconds() + costPolicy.travelSeconds(edge, request.profile());
    BigDecimal distance = currentBest.distanceMeters().add(edge.lengthMeters());
    GraphNode from = snapshot.nodesByCode().get(edge.fromNodeCode());
    int transitions =
        currentBest.transitionCount()
            + (from != null && !from.floorId().equals(next.floorId()) ? 1 : 0);
    Label candidate = new Label(seconds, distance, transitions, current.nodeCode(), edge);
    Label previous = best.get(next.code());
    if (previous != null && compare(candidate, previous) >= 0) {
      return;
    }
    best.put(next.code(), candidate);
    queue.add(
        new QueueState(
            next.code(), seconds, seconds + heuristic.estimate(next), distance, transitions));
  }

  private static int compare(Label left, Label right) {
    double delta = left.travelSeconds() - right.travelSeconds();
    if (Math.abs(delta) > EPSILON) {
      return delta < 0 ? -1 : 1;
    }
    int distance = left.distanceMeters().compareTo(right.distanceMeters());
    return distance != 0
        ? distance
        : Integer.compare(left.transitionCount(), right.transitionCount());
  }

  private static boolean matches(QueueState state, Label label) {
    return label != null
        && Math.abs(state.travelSeconds() - label.travelSeconds()) <= EPSILON
        && state.distanceMeters().compareTo(label.distanceMeters()) == 0
        && state.transitionCount() == label.transitionCount();
  }

  private static List<GraphEdge> reconstruct(
      Map<String, Label> best, String startCode, String destinationCode) {
    List<GraphEdge> reversed = new ArrayList<>();
    String current = destinationCode;
    while (!current.equals(startCode)) {
      Label label = best.get(current);
      if (label == null || label.edge() == null || label.previousNodeCode() == null) {
        throw new IllegalStateException("Broken route predecessor chain at " + current);
      }
      reversed.add(label.edge());
      current = label.previousNodeCode();
    }
    return reversed.reversed();
  }

  private static GraphNode requireNode(
      GraphSnapshot snapshot, String nodeCode, RouteRequest request) {
    GraphNode node = snapshot.nodesByCode().get(nodeCode);
    if (node == null) {
      throw new RouteNotFoundException(request);
    }
    return node;
  }

  private static RouteResult zeroRoute(
      GraphSnapshot snapshot, RouteRequest request, GraphNode node) {
    return new RouteResult(
        snapshot.mallId(),
        snapshot.versionId(),
        request.profile(),
        request.startNodeCode(),
        request.destinationNodeCode(),
        BigDecimal.ZERO,
        Duration.ZERO,
        List.of(),
        List.of(new RouteInstruction.Arrive(node.floorId(), node.code())));
  }

  @FunctionalInterface
  protected interface Heuristic {
    double estimate(GraphNode node);
  }

  private record Label(
      double travelSeconds,
      BigDecimal distanceMeters,
      int transitionCount,
      String previousNodeCode,
      GraphEdge edge) {}

  private record QueueState(
      String nodeCode,
      double travelSeconds,
      double estimatedTotalSeconds,
      BigDecimal distanceMeters,
      int transitionCount) {}
}
