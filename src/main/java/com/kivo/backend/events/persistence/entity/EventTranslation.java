package com.kivo.backend.events.persistence.entity;

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
@Table(name = "event_translation")
public class EventTranslation extends AbstractUuidEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "event_id", nullable = false)
  @NotNull
  private Event event;

  @NotBlank
  @Pattern(regexp = "[a-z]{2,3}")
  @Column(name = "language", nullable = false, length = 8)
  private String language;

  @NotBlank
  @Size(max = 255)
  @Column(name = "title", nullable = false, length = 255)
  private String title;

  @Size(max = 2000)
  @Column(name = "description", length = 2000)
  private String description;

  public static EventTranslation create(String language, String title, String description) {
    EventTranslation translation = new EventTranslation();
    translation.language = language;
    translation.title = title;
    translation.description = description;
    return translation;
  }

  void attachTo(Event event) {
    this.event = event;
  }

  void detach() {
    event = null;
  }

  @Override
  public String toString() {
    return "EventTranslation{"
        + "id="
        + getId()
        + ", language='"
        + language
        + '\''
        + ", title='"
        + title
        + '\''
        + '}';
  }
}
