package com.kivo.backend.catalog.persistence.entity;

import com.kivo.backend.common.persistence.AbstractUuidEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "mall")
public class Mall extends AbstractUuidEntity {

  @NotBlank
  @Size(max = 64)
  @Column(name = "code", nullable = false, length = 64)
  private String code;

  @NotBlank
  @Size(max = 255)
  @Column(name = "name", nullable = false, length = 255)
  private String name;

  @NotBlank
  @Pattern(regexp = "[a-z]{2,3}")
  @Column(name = "default_language", nullable = false, length = 8)
  private String defaultLanguage;

  @NotBlank
  @Size(max = 64)
  @Column(name = "timezone", nullable = false, length = 64)
  private String timezone;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @OneToMany(
      mappedBy = "mall",
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  private Set<MallLanguage> languages = new LinkedHashSet<>();

  public Set<MallLanguage> getLanguages() {
    return Collections.unmodifiableSet(languages);
  }

  public void addLanguage(MallLanguage language) {
    languages.add(language);
    language.attachTo(this);
  }

  public void removeLanguage(MallLanguage language) {
    languages.remove(language);
    language.detach();
  }

  @PrePersist
  void initializeCreatedAt() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }

  @Override
  public String toString() {
    return "Mall{" + "id=" + getId() + ", code='" + code + '\'' + ", name='" + name + '\'' + '}';
  }
}
