package com.kivo.backend.navigation.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class GraphSnapshot {

  private final UUID mallId;
  private final UUID versionId;
  private final String versionCode;
  private final List<GraphNode> nodes;
  private final List<GraphEdge> edges;
  private final List<GraphAnchor> places;
  private final List<GraphAnchor> kiosks;
  private final Map<String, GraphNode> nodesByCode;
  private final Map<String, GraphEdge> edgesByCode;
  private final Map<String, List<GraphEdge>> outgoingEdges;
  private final Set<String> duplicateNodeCodes;
  private final Set<String> duplicateEdgeCodes;

  public GraphSnapshot(
      UUID mallId,
      UUID versionId,
      String versionCode,
      List<GraphNode> nodes,
      List<GraphEdge> edges,
      List<GraphAnchor> places,
      List<GraphAnchor> kiosks) {
    this.mallId = Objects.requireNonNull(mallId, "mallId");
    this.versionId = Objects.requireNonNull(versionId, "versionId");
    this.versionCode = Objects.requireNonNull(versionCode, "versionCode");
    this.nodes = List.copyOf(nodes);
    this.edges = List.copyOf(edges);
    this.places = List.copyOf(places);
    this.kiosks = List.copyOf(kiosks);

    BuildResult<GraphNode> nodeIndex = indexByCode(this.nodes, GraphNode::code);
    BuildResult<GraphEdge> edgeIndex = indexByCode(this.edges, GraphEdge::code);
    this.nodesByCode = nodeIndex.values();
    this.edgesByCode = edgeIndex.values();
    this.duplicateNodeCodes = nodeIndex.duplicates();
    this.duplicateEdgeCodes = edgeIndex.duplicates();
    this.outgoingEdges = buildAdjacency(this.nodesByCode.keySet(), this.edges);
  }

  public UUID mallId() {
    return mallId;
  }

  public UUID versionId() {
    return versionId;
  }

  public String versionCode() {
    return versionCode;
  }

  public List<GraphNode> nodes() {
    return nodes;
  }

  public List<GraphEdge> edges() {
    return edges;
  }

  public List<GraphAnchor> places() {
    return places;
  }

  public List<GraphAnchor> kiosks() {
    return kiosks;
  }

  public Map<String, GraphNode> nodesByCode() {
    return nodesByCode;
  }

  public Map<String, GraphEdge> edgesByCode() {
    return edgesByCode;
  }

  public Map<String, List<GraphEdge>> outgoingEdges() {
    return outgoingEdges;
  }

  public Set<String> duplicateNodeCodes() {
    return duplicateNodeCodes;
  }

  public Set<String> duplicateEdgeCodes() {
    return duplicateEdgeCodes;
  }

  private static <T> BuildResult<T> indexByCode(
      List<T> values, java.util.function.Function<T, String> codeExtractor) {
    Map<String, T> index = new LinkedHashMap<>();
    Set<String> duplicates = new LinkedHashSet<>();
    for (T value : values) {
      String code = codeExtractor.apply(value);
      if (index.putIfAbsent(code, value) != null) {
        duplicates.add(code);
      }
    }
    return new BuildResult<>(
        Collections.unmodifiableMap(new LinkedHashMap<>(index)), Set.copyOf(duplicates));
  }

  private static Map<String, List<GraphEdge>> buildAdjacency(
      Set<String> nodeCodes, List<GraphEdge> edges) {
    Map<String, List<GraphEdge>> mutable = new LinkedHashMap<>();
    nodeCodes.forEach(code -> mutable.put(code, new ArrayList<>()));
    edges.forEach(
        edge ->
            mutable.computeIfAbsent(edge.fromNodeCode(), ignored -> new ArrayList<>()).add(edge));
    Map<String, List<GraphEdge>> immutable = new LinkedHashMap<>();
    mutable.forEach((code, outgoing) -> immutable.put(code, List.copyOf(outgoing)));
    return Collections.unmodifiableMap(immutable);
  }

  private record BuildResult<T>(Map<String, T> values, Set<String> duplicates) {}
}
