# Kivo Backend

Backend for indoor navigation in large retail spaces. The service will expose a JSON REST API for
kiosks built with Intuiface and provide route sharing for mobile devices.

## Technology baseline

- Java 25
- Spring Boot 4.1
- Gradle with Kotlin DSL
- PostgreSQL and Flyway
- Spring Data JPA
- JUnit 5; integration tests will use Testcontainers
- Spotless with google-java-format

The application is a modular monolith. Domain modules stay inside one deployable service while
keeping navigation, catalog, maps, events, sharing, feedback, and administration isolated in code.

## Local development

Prerequisites: JDK 25 and Docker Desktop.

```powershell
docker compose up -d
.\gradlew.bat bootRun
```

The local database defaults are defined in `compose.yaml`. Override them with `DB_URL`,
`DB_USERNAME`, and `DB_PASSWORD` outside local development.

Run the test suite:

```powershell
.\gradlew.bat test
```

## Project plan

The living implementation plan and architectural decisions are in
[`docs/plans/20260809-kivo-backend.md`](docs/plans/20260809-kivo-backend.md). It must be updated as
tasks are completed or the scope changes.
