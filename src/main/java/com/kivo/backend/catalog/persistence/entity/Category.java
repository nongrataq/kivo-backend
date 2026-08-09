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
@Table(name = "category")
public class Category extends AbstractUuidEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "mall_id", nullable = false)
  @NotNull
  private Mall mall;

  @NotBlank
  @Size(max = 64)
  @Column(name = "code", nullable = false, length = 64)
  private String code;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  @OneToMany(
      mappedBy = "category",
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  private Set<CategoryTranslation> translations = new LinkedHashSet<>();

  public Set<CategoryTranslation> getTranslations() {
    return Collections.unmodifiableSet(translations);
  }

  public void addTranslation(CategoryTranslation translation) {
    translations.add(translation);
    translation.attachTo(this);
  }

  public void removeTranslation(CategoryTranslation translation) {
    translations.remove(translation);
    translation.detach();
  }

  @Override
  public String toString() {
    return "Category{"
        + "id="
        + getId()
        + ", code='"
        + code
        + '\''
        + ", displayOrder="
        + displayOrder
        + ", active="
        + active
        + '}';
  }
}
