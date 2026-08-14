package com.kivo.backend.testsupport;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class MutableClock extends Clock {

  private final AtomicReference<Instant> instant;
  private final ZoneId zone;

  public MutableClock(Instant instant, ZoneId zone) {
    this(new AtomicReference<>(Objects.requireNonNull(instant)), zone);
  }

  private MutableClock(AtomicReference<Instant> instant, ZoneId zone) {
    this.instant = instant;
    this.zone = Objects.requireNonNull(zone);
  }

  public void set(Instant value) {
    instant.set(Objects.requireNonNull(value));
  }

  public void advance(Duration duration) {
    instant.updateAndGet(value -> value.plus(duration));
  }

  @Override
  public ZoneId getZone() {
    return zone;
  }

  @Override
  public Clock withZone(ZoneId zone) {
    return new MutableClock(instant, zone);
  }

  @Override
  public Instant instant() {
    return instant.get();
  }
}
