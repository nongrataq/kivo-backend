package com.kivo.backend.navigation.application;

import com.kivo.backend.navigation.domain.model.GraphSnapshot;
import java.util.UUID;

public interface GraphSnapshotLoader {

  GraphSnapshot load(UUID mallId, UUID versionId);
}
