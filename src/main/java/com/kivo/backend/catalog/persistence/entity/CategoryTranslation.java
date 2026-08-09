package com.kivo.backend.catalog.persistence.entity;

import com.kivo.backend.common.persistence.AbstractUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "category_translation")
public class CategoryTranslation extends AbstractUuidEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "category_id", nullable = false)
  @NotNull
  private Category category;

  @NotBlank
  @Pattern(regexp = "[a-z]{2,3}")
  @Column(name = "language", nullable = false, length = 8)
  private String language;

  @NotBlank
  @Size(max = 255)
  @Column(name = "name", nullable = false, length = 255)
  private String name;

  public static CategoryTranslation create(String language, String name) {
    CategoryTranslation translation = new CategoryTranslation();
    translation.language = language;
    translation.name = name;
    return translation;
  }

  void attachTo(Category category) {
    this.category = category;
  }

  void detach() {
    category = null;
  }

  @Override
  public String toString() {
    return "CategoryTranslation{"
        + "id="
        + getId()
        + ", language='"
        + language
        + '\''
        + ", name='"
        + name
        + '\''
        + '}';
  }
}
