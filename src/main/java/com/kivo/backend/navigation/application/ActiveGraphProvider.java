package com.kivo.backend.navigation.application;

import com.kivo.backend.navigation.domain.model.GraphSnapshot;
import java.util.Optional;
import java.util.UUID;

public interface ActiveGraphProvider {

  Optional<GraphSnapshot> find(UUID mallId);

  GraphSnapshot getRequired(UUID mallId);
}
