package com.kivo.backend.navigation.persistence.entity;

import com.kivo.backend.common.persistence.AbstractUuidEntity;
import com.kivo.backend.navigation.domain.model.MovementType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "navigation_edge")
public class NavigationEdge extends AbstractUuidEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "graph_version_id", nullable = false)
  @NotNull
  private NavigationGraphVersion graphVersion;

  @NotBlank
  @Size(max = 64)
  @Column(name = "external_code", nullable = false, length = 64)
  private String externalCode;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "from_node_id", nullable = false)
  @NotNull
  private NavigationNode fromNode;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "to_node_id", nullable = false)
  @NotNull
  private NavigationNode toNode;

  @NotNull
  @DecimalMin(value = "0.001")
  @Column(name = "length_meters", nullable = false, precision = 12, scale = 3)
  private BigDecimal lengthMeters;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "movement_type", nullable = false, length = 32)
  private MovementType movementType;

  @Column(name = "closed", nullable = false)
  private boolean closed;

  @Column(name = "wheelchair_accessible", nullable = false)
  private boolean wheelchairAccessible = true;

  @OneToMany(
      mappedBy = "edge",
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  @OrderBy("pointOrder ASC")
  private Set<NavigationEdgePoint> points = new LinkedHashSet<>();

  public static NavigationEdge create(
      NavigationGraphVersion graphVersion,
      String externalCode,
      NavigationNode fromNode,
      NavigationNode toNode,
      BigDecimal lengthMeters,
      MovementType movementType,
      boolean closed,
      boolean wheelchairAccessible) {
    NavigationEdge edge = new NavigationEdge();
    edge.graphVersion = graphVersion;
    edge.externalCode = externalCode;
    edge.fromNode = fromNode;
    edge.toNode = toNode;
    edge.lengthMeters = lengthMeters;
    edge.movementType = movementType;
    edge.closed = closed;
    edge.wheelchairAccessible = wheelchairAccessible;
    return edge;
  }

  public Set<NavigationEdgePoint> getPoints() {
    return Collections.unmodifiableSet(points);
  }

  public void addPoint(NavigationEdgePoint point) {
    points.add(point);
    point.attachTo(this);
  }

  public void removePoint(NavigationEdgePoint point) {
    points.remove(point);
    point.detach();
  }

  @Override
  public String toString() {
    return "NavigationEdge{"
        + "id="
        + getId()
        + ", externalCode='"
        + externalCode
        + '\''
        + ", lengthMeters="
        + lengthMeters
        + ", movementType="
        + movementType
        + ", closed="
        + closed
        + '}';
  }
}
