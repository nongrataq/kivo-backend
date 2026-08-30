package com.kivo.backend.navigation.configuration;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("kivo.readiness.navigation")
public record NavigationReadinessProperties(@Min(0) int minimumActiveGraphs) {}
