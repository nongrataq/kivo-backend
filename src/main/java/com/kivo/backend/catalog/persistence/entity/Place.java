package com.kivo.backend.catalog.persistence.entity;

import com.kivo.backend.common.persistence.AbstractUuidEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "place")
public class Place extends AbstractUuidEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "mall_id", nullable = false)
  @NotNull
  private Mall mall;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "floor_id", nullable = false)
  @NotNull
  private Floor floor;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "category_id", nullable = false)
  @NotNull
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

  public Set<PlaceTranslation> getTranslations() {
    return Collections.unmodifiableSet(translations);
  }

  public void addTranslation(PlaceTranslation translation) {
    translations.add(translation);
    translation.attachTo(this);
  }

  public void removeTranslation(PlaceTranslation translation) {
    translations.remove(translation);
    translation.detach();
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
