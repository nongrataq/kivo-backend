package com.kivo.backend.admin.api;

import java.util.UUID;

public record GraphPublicationResponse(
    String mallCode, UUID versionId, String versionCode, int nodeCount, int edgeCount) {}
