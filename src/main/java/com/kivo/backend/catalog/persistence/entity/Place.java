package com.kivo.backend.catalog.persistence.entity;

import com.kivo.backend.common.persistence.AbstractUuidEntity;
import com.kivo.backend.maps.persistence.entity.Floor;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "place")
public class Place extends AbstractUuidEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "mall_id", nullable = false)
  private Mall mall;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "floor_id", nullable = false)
  private Floor floor;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "category_id", nullable = false)
  private Category category;

  @NotBlank
  @Size(max = 64)
  @Column(name = "code", nullable = false, length = 64)
  private String code;

  @Size(max = 64)
  @Column(name = "navigation_node_code", length = 64)
  private String navigationNodeCode;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  @OneToMany(
      mappedBy = "place",
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  private Set<PlaceTranslation> translations = new LinkedHashSet<>();

  protected Place() {}

  public Mall getMall() {
    return mall;
  }

  public void setMall(Mall mall) {
    this.mall = mall;
  }

  public Floor getFloor() {
    return floor;
  }

  public void setFloor(Floor floor) {
    this.floor = floor;
  }

  public Category getCategory() {
    return category;
  }

  public void setCategory(Category category) {
    this.category = category;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getNavigationNodeCode() {
    return navigationNodeCode;
  }

  public void setNavigationNodeCode(String navigationNodeCode) {
    this.navigationNodeCode = navigationNodeCode;
  }

  public int getDisplayOrder() {
    return displayOrder;
  }

  public void setDisplayOrder(int displayOrder) {
    this.displayOrder = displayOrder;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public Set<PlaceTranslation> getTranslations() {
    return Collections.unmodifiableSet(translations);
  }

  public void addTranslation(PlaceTranslation translation) {
    translations.add(translation);
    translation.setPlace(this);
  }

  public void removeTranslation(PlaceTranslation translation) {
    translations.remove(translation);
    translation.setPlace(null);
  }

  @Override
  public String toString() {
    return "Place{"
        + "id="
        + getId()
        + ", code='"
        + code
        + '\''
        + ", navigationNodeCode='"
        + navigationNodeCode
        + '\''
        + ", active="
        + active
        + '}';
  }
}
