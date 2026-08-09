package com.kivo.backend.navigation.domain;

import com.kivo.backend.navigation.domain.model.GraphSnapshot;
import com.kivo.backend.navigation.domain.model.GraphValidationResult;

public interface GraphValidator {

  GraphValidationResult validate(GraphSnapshot snapshot);
}
