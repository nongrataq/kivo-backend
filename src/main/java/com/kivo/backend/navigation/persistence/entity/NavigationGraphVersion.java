package com.kivo.backend.navigation.persistence.entity;

import com.kivo.backend.catalog.persistence.entity.Mall;
import com.kivo.backend.common.persistence.AbstractUuidEntity;
import com.kivo.backend.navigation.domain.model.GraphVersionState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "navigation_graph_version")
public class NavigationGraphVersion extends AbstractUuidEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "mall_id", nullable = false)
  @NotNull
  private Mall mall;

  @NotBlank
  @Size(max = 64)
  @Column(name = "version_code", nullable = false, length = 64)
  private String versionCode;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "state", nullable = false, length = 16)
  private GraphVersionState state = GraphVersionState.DRAFT;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "validated_at")
  private Instant validatedAt;

  public static NavigationGraphVersion create(Mall mall, String versionCode) {
    NavigationGraphVersion version = new NavigationGraphVersion();
    version.mall = mall;
    version.versionCode = versionCode;
    return version;
  }

  public void markValidated(Instant validatedAt) {
    this.state = GraphVersionState.VALIDATED;
    this.validatedAt = validatedAt;
  }

  @PrePersist
  void initializeCreatedAt() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }

  @Override
  public String toString() {
    return "NavigationGraphVersion{"
        + "id="
        + getId()
        + ", versionCode='"
        + versionCode
        + '\''
        + ", state="
        + state
        + '}';
  }
}
