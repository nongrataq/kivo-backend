package com.kivo.backend.navigation.persistence.entity;

import com.kivo.backend.catalog.persistence.entity.Floor;
import com.kivo.backend.common.persistence.AbstractUuidEntity;
import com.kivo.backend.navigation.domain.model.NodeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "navigation_node")
public class NavigationNode extends AbstractUuidEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "graph_version_id", nullable = false)
  @NotNull
  private NavigationGraphVersion graphVersion;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "floor_id", nullable = false)
  @NotNull
  private Floor floor;

  @NotBlank
  @Size(max = 64)
  @Column(name = "external_code", nullable = false, length = 64)
  private String externalCode;

  @Min(0)
  @Max(10000)
  @Column(name = "x", nullable = false)
  private int x;

  @Min(0)
  @Max(10000)
  @Column(name = "y", nullable = false)
  private int y;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "node_type", nullable = false, length = 32)
  private NodeType nodeType;

  public static NavigationNode create(
      NavigationGraphVersion graphVersion,
      Floor floor,
      String externalCode,
      int x,
      int y,
      NodeType nodeType) {
    NavigationNode node = new NavigationNode();
    node.graphVersion = graphVersion;
    node.floor = floor;
    node.externalCode = externalCode;
    node.x = x;
    node.y = y;
    node.nodeType = nodeType;
    return node;
  }

  @Override
  public String toString() {
    return "NavigationNode{"
        + "id="
        + getId()
        + ", externalCode='"
        + externalCode
        + '\''
        + ", x="
        + x
        + ", y="
        + y
        + ", nodeType="
        + nodeType
        + '}';
  }
}
