package com.kivo.backend.testsupport;

import org.springframework.context.annotation.Import;

@Import(PostgreSqlTestConfiguration.class)
public abstract class PostgreSqlIntegrationTest {}
