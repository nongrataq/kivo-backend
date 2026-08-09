package com.kivo.backend.catalog.persistence.entity;

import com.kivo.backend.common.persistence.AbstractUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
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

  protected Mall() {}

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
