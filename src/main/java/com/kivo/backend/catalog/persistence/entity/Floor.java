package com.kivo.backend.catalog.persistence.entity;

import com.kivo.backend.common.persistence.AbstractUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "floor")
public class Floor extends AbstractUuidEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "mall_id", nullable = false)
  private Mall mall;

  @NotBlank
  @Size(max = 32)
  @Column(name = "code", nullable = false, length = 32)
  private String code;

  @NotBlank
  @Size(max = 128)
  @Column(name = "name", nullable = false, length = 128)
  private String name;

  @Column(name = "level_number", nullable = false)
  private int levelNumber;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  @Size(max = 2048)
  @Column(name = "map_url", length = 2048)
  private String mapUrl;

  @Positive
  @Column(name = "coordinate_width", nullable = false)
  private int coordinateWidth = 10000;

  @Positive
  @Column(name = "coordinate_height", nullable = false)
  private int coordinateHeight = 10000;

  @DecimalMin(value = "0.00000001")
  @Column(name = "meters_per_unit", precision = 12, scale = 8)
  private BigDecimal metersPerUnit;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  protected Floor() {}

  @Override
  public String toString() {
    return "Floor{"
        + "id="
        + getId()
        + ", code='"
        + code
        + '\''
        + ", name='"
        + name
        + '\''
        + ", levelNumber="
        + levelNumber
        + '}';
  }
}
