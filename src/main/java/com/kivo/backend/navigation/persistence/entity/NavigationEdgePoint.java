package com.kivo.backend.navigation.persistence.entity;

import com.kivo.backend.common.persistence.AbstractUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "navigation_edge_point")
public class NavigationEdgePoint extends AbstractUuidEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "edge_id", nullable = false)
  @NotNull
  private NavigationEdge edge;

  @Min(0)
  @Column(name = "point_order", nullable = false)
  private int pointOrder;

  @Min(0)
  @Max(10000)
  @Column(name = "x", nullable = false)
  private int x;

  @Min(0)
  @Max(10000)
  @Column(name = "y", nullable = false)
  private int y;

  public static NavigationEdgePoint create(int pointOrder, int x, int y) {
    NavigationEdgePoint point = new NavigationEdgePoint();
    point.pointOrder = pointOrder;
    point.x = x;
    point.y = y;
    return point;
  }

  void attachTo(NavigationEdge edge) {
    this.edge = edge;
  }

  void detach() {
    edge = null;
  }

  @Override
  public String toString() {
    return "NavigationEdgePoint{"
        + "id="
        + getId()
        + ", pointOrder="
        + pointOrder
        + ", x="
        + x
        + ", y="
        + y
        + '}';
  }
}
