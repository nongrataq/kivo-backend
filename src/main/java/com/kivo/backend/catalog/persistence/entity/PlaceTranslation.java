package com.kivo.backend.catalog.persistence.entity;

import com.kivo.backend.common.persistence.AbstractUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "place_translation")
public class PlaceTranslation extends AbstractUuidEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "place_id", nullable = false)
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

  protected PlaceTranslation() {}

  public Place getPlace() {
    return place;
  }

  public void setPlace(Place place) {
    this.place = place;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
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
