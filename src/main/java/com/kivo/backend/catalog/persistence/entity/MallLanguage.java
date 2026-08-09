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

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "mall_language")
public class MallLanguage extends AbstractUuidEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "mall_id", nullable = false)
  @NotNull
  private Mall mall;

  @NotBlank
  @Pattern(regexp = "[a-z]{2,3}")
  @Column(name = "language", nullable = false, length = 8)
  private String language;

  @NotBlank
  @Size(max = 64)
  @Column(name = "display_label", nullable = false, length = 64)
  private String displayLabel;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  public static MallLanguage create(String language, String displayLabel, int displayOrder) {
    MallLanguage mallLanguage = new MallLanguage();
    mallLanguage.language = language;
    mallLanguage.displayLabel = displayLabel;
    mallLanguage.displayOrder = displayOrder;
    return mallLanguage;
  }

  void attachTo(Mall mall) {
    this.mall = mall;
  }

  void detach() {
    mall = null;
  }

  @Override
  public String toString() {
    return "MallLanguage{"
        + "id="
        + getId()
        + ", language='"
        + language
        + '\''
        + ", displayLabel='"
        + displayLabel
        + '\''
        + '}';
  }
}
