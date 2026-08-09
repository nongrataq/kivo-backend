package com.kivo.backend.kiosks.persistence.entity;

import com.kivo.backend.catalog.persistence.entity.Mall;
import com.kivo.backend.common.persistence.AbstractUuidEntity;
import com.kivo.backend.maps.persistence.entity.Floor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "kiosk")
public class Kiosk extends AbstractUuidEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "mall_id", nullable = false)
  private Mall mall;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "floor_id", nullable = false)
  private Floor floor;

  @NotBlank
  @Size(max = 64)
  @Column(name = "code", nullable = false, length = 64)
  private String code;

  @Size(max = 64)
  @Column(name = "start_navigation_node_code", length = 64)
  private String startNavigationNodeCode;

  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

  protected Kiosk() {}

  @Override
  public String toString() {
    return "Kiosk{"
        + "id="
        + getId()
        + ", code='"
        + code
        + '\''
        + ", startNavigationNodeCode='"
        + startNavigationNodeCode
        + '\''
        + ", enabled="
        + enabled
        + '}';
  }
}
