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
@Table(name = "place_translation")
public class PlaceTranslation extends AbstractUuidEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "place_id", nullable = false)
  @NotNull
  private Place place;

  @NotBlank
  @Pattern(regexp = "[a-z]{2,3}")
  @Column(name = "language", nullable = false, length = 8)
  private String language;

  @NotBlank
  @Size(max = 255)
  @Column(name = "name", nullable = false, length = 255)
  private String name;

  @Size(max = 2000)
  @Column(name = "description", length = 2000)
  private String description;

  public static PlaceTranslation create(String language, String name, String description) {
    PlaceTranslation translation = new PlaceTranslation();
    translation.language = language;
    translation.name = name;
    translation.description = description;
    return translation;
  }

  void attachTo(Place place) {
    this.place = place;
  }

  void detach() {
    place = null;
  }

  @Override
  public String toString() {
    return "PlaceTranslation{"
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
