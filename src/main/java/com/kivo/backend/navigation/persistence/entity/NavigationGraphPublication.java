package com.kivo.backend.navigation.persistence.entity;

import com.kivo.backend.catalog.persistence.entity.Mall;
import com.kivo.backend.common.persistence.AbstractUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "navigation_graph_publication")
public class NavigationGraphPublication extends AbstractUuidEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "mall_id", nullable = false)
  @NotNull
  private Mall mall;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "active_graph_version_id", nullable = false)
  @NotNull
  private NavigationGraphVersion activeGraphVersion;

  @Column(name = "published_at", nullable = false)
  private Instant publishedAt;

  public static NavigationGraphPublication create(
      Mall mall, NavigationGraphVersion activeGraphVersion, Instant publishedAt) {
    NavigationGraphPublication publication = new NavigationGraphPublication();
    publication.mall = mall;
    publication.activeGraphVersion = activeGraphVersion;
    publication.publishedAt = publishedAt;
    return publication;
  }

  public void replaceWith(NavigationGraphVersion graphVersion, Instant publishedAt) {
    this.activeGraphVersion = graphVersion;
    this.publishedAt = publishedAt;
  }

  @Override
  public String toString() {
    return "NavigationGraphPublication{" + "id=" + getId() + ", publishedAt=" + publishedAt + '}';
  }
}
