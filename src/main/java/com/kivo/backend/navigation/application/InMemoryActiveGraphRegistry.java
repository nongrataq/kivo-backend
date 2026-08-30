package com.kivo.backend.navigation.application;

import com.kivo.backend.navigation.domain.model.GraphSnapshot;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class InMemoryActiveGraphRegistry implements ActiveGraphProvider {

  private final ConcurrentHashMap<UUID, AtomicReference<GraphSnapshot>> snapshots =
      new ConcurrentHashMap<>();

  public void replace(GraphSnapshot snapshot) {
    snapshots.computeIfAbsent(snapshot.mallId(), ignored -> new AtomicReference<>()).set(snapshot);
  }

  @Override
  public Optional<GraphSnapshot> find(UUID mallId) {
    AtomicReference<GraphSnapshot> reference = snapshots.get(mallId);
    return reference == null ? Optional.empty() : Optional.ofNullable(reference.get());
  }

  @Override
  public GraphSnapshot getRequired(UUID mallId) {
    return find(mallId).orElseThrow(() -> new ActiveGraphNotFoundException(mallId));
  }

  public int activeSnapshotCount() {
    return (int) snapshots.values().stream().filter(reference -> reference.get() != null).count();
  }

  void clear() {
    snapshots.clear();
  }
}
