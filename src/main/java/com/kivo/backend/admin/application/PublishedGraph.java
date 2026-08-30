package com.kivo.backend.admin.application;

import java.util.UUID;

public record PublishedGraph(
    String mallCode, UUID versionId, String versionCode, int nodeCount, int edgeCount) {}
