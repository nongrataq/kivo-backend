# Kivo Backend — Full Implementation Plan

## Overview

Kivo is a backend for indoor navigation in large shopping centres and other retail spaces. A visitor
uses a fixed kiosk, searches for a destination, receives the fastest valid walking route, and may
transfer that route to a phone through a QR code.

The first delivery target is a complete, tested backend with a stable JSON API. Integration with
Ariadna's Figma/Intuiface frontend follows after the backend contract and functionality are ready.

## Agreed Decisions

- Java 25 and Spring Boot 4.1.
- Gradle with Kotlin DSL and Gradle Wrapper.
- Gradle toolchains automatically provision Java 25 when it is not installed locally.
- Spotless with google-java-format enforces Java formatting in local builds and CI.
- Modular monolith; no microservices for the initial product.
- PostgreSQL with versioned Flyway migrations.
- Spring Data JPA for persistence; JPA entities never leave the persistence boundary.
- Contract-first REST API documented with OpenAPI.
- Ordinary development flow: implementation and its tests are completed in the same task.
- Every production code change requires success and error/edge-case tests.
- The repository remains deployable after every completed task; failing tests block further work.
- `main` is the stable branch. Feature work uses short-lived `codex/*` branches and pull requests
  once regular development begins.

## System Context

```text
Intuiface kiosk ──HTTPS/JSON──> Kivo Spring Boot API ──> PostgreSQL
                                      │
                                      ├──> in-memory navigation graph
                                      ├──> route overlay renderer
                                      └──> expiring mobile route page <── QR/phone
```

The visual floor plan and the navigation topology are separate:

- Intuiface owns presentation and displays floor artwork.
- The backend owns routable nodes, edges, restrictions, distances, floor transitions, and POI links.
- Coordinates use a resolution-independent `0..10000` system with origin at the top-left.
- The backend returns route metadata and per-floor overlay URLs, so Intuiface does not need to draw
  arbitrary polylines itself.

## Target Modules

All modules live below `com.kivo.backend` and expose dependencies through application services:

- `catalog`: malls, categories, places, localized content, and search.
- `maps`: floors, map metadata, coordinate conversion, and route overlays.
- `kiosks`: kiosk identity, configuration, and fixed starting positions.
- `navigation`: graph model, graph loading, route policies, A*/Dijkstra, and instructions.
- `events`: current and upcoming events linked to places.
- `sharing`: opaque expiring share tokens, QR images, and mobile route retrieval.
- `feedback`: anonymous usefulness feedback and operational metadata.
- `admin`: protected data import and maintenance capabilities added after the public API is stable.
- `common`: API errors, validation, localization, time, identifiers, and configuration.

Modules must not access another module's repositories directly. Cross-module behaviour goes through
an application service or an explicitly defined read port.

## Domain and Persistence

Planned persisted concepts:

- `Mall`: stable code, name, timezone, active graph version.
- `Floor`: mall, floor code, display order, map URL, coordinate width/height, real-world scale.
- `Kiosk`: public code, mall, floor, entry navigation node, enabled flag.
- `Category` and translations: localized searchable category names.
- `Place` and translations: POI metadata, category, floor, nearest navigation node.
- `NavigationNode`: floor and normalized coordinates; may represent a turn, entrance, or connector.
- `NavigationEdge`: directed endpoints, length, geometry, traversal type, accessibility, state.
- `Event`: localized content, time interval, place, publication state.
- `SharedRoute`: opaque token hash, route snapshot/reference, expiration.
- `Feedback`: route/kiosk context, useful flag, timestamp; no personal data.

The routing graph is loaded from PostgreSQL into an immutable in-memory snapshot per mall and graph
version. PostgreSQL remains the source of truth. A publish/reload operation atomically replaces the
active snapshot so route requests never see a half-updated graph.

> **JPA skill required (`spring-data-jpa`).** Before writing entity or repository code, activate the
> skill, inspect established conventions, and keep schema changes in the same task as their Flyway
> migration. Any deviation requires developer confirmation.

## Routing Engine

The routing engine is pure Java and does not depend on Spring or JPA. This keeps its behaviour fast,
deterministic, and exhaustively testable.

Route cost is time-oriented rather than straight-line distance:

```text
edge cost = distance / walking speed + traversal penalty + dynamic restriction
```

- Corridor edges use walking time.
- Stairs, escalators, and elevators apply configurable penalties.
- Closed edges are excluded.
- Accessible routing excludes stairs and other incompatible edges.
- A* is the default algorithm with an admissible geometric heuristic.
- Dijkstra remains a reference implementation for correctness tests and cases with no safe heuristic.
- A route across floors contains explicit transition steps and a separate overlay per floor.
- An unreachable destination returns a typed `ROUTE_NOT_FOUND` response, never an empty success.

Graph validation must reject missing endpoints, negative lengths, invalid floor connectors,
unreachable public places, duplicate external IDs, and coordinates outside floor bounds.

## Initial REST Contract

Public kiosk API under `/api/v1`:

```text
GET  /kiosks/{kioskCode}/bootstrap
GET  /malls/{mallCode}/categories
GET  /malls/{mallCode}/places?q=&categoryId=&lang=
GET  /places/{placeId}?lang=
POST /routes
GET  /routes/{routeId}
GET  /routes/{routeId}/floors/{floorCode}/overlay.svg
POST /routes/{routeId}/share
GET  /public/routes/{shareToken}
GET  /malls/{mallCode}/events/today?lang=
POST /feedback
```

API rules:

- camelCase JSON and ISO-8601 timestamps in UTC;
- UUID/ULID-style opaque public identifiers rather than database sequence IDs;
- explicit `lang` for simple Intuiface binding, with a documented fallback language;
- Bean Validation at the HTTP boundary;
- RFC 9457 Problem Details plus stable Kivo error codes;
- bounded search result sizes and server-side filtering;
- no nested arrays in Intuiface-facing responses;
- no persistence entities in controllers, DTOs, or OpenAPI schemas;
- additive changes inside `v1`; breaking changes require a new API version.

## Security and Privacy

- Public read data contains no secrets or personal information.
- Administrative endpoints are isolated from kiosk endpoints and require authentication.
- Kiosk credentials, if introduced, are revocable and supplied through deployment configuration.
- QR links contain a high-entropy opaque token, expire automatically, and reveal no database IDs.
- Feedback is anonymous; IP addresses are not domain data.
- Secrets never enter Git. Local defaults are development-only and production uses environment or
  platform-managed secrets.
- Apply rate limits to route creation, search, sharing, and public share retrieval before deployment.

## Testing Strategy

- Unit tests: graph construction, A*/Dijkstra, cost policies, validation, mapping, and application
  services. These must be fast and avoid Spring.
- Slice tests: MVC validation/error contracts and JPA repository behaviour where appropriate.
- Integration tests: Spring Boot + PostgreSQL Testcontainers + Flyway.
- Architecture tests: enforce module boundaries and prevent controllers from exposing entities.
- Contract tests: validate response examples against OpenAPI and Intuiface-friendly JSON constraints.
- End-to-end backend scenarios: bootstrap → search → route → share → public retrieval → feedback.
- Quality gates: compilation, all tests, migration validation, and static formatting on every push.

No task is complete while relevant tests are failing. Test coverage is used as a diagnostic, not as a
substitute for meaningful assertions; the routing engine receives the highest branch/edge-case focus.

## Progress Tracking

- Mark completed checklist items with `[x]` immediately.
- Add newly discovered work with `[+]` and blockers with `[!]`.
- Update decisions and file lists when the implementation changes.
- Complete each task, including tests, before beginning the next one.
- Move this file to `docs/plans/completed/` only when the complete backend scope is delivered.

## Implementation Steps

### Task 1: Bootstrap the repository and build

**Files:**

- Create: `build.gradle.kts`, `settings.gradle.kts`, `gradle/wrapper/*`
- Create: `src/main/java/com/kivo/backend/KivoBackendApplication.java`
- Create: `src/main/resources/application.properties`, `compose.yaml`
- Create: `src/test/**`, `README.md`, `.editorconfig`

- [x] generate a Java 25 / Spring Boot 4.1 Gradle project
- [x] configure PostgreSQL, Flyway, validation, JPA, Actuator, and MVC dependencies
- [x] externalize database credentials and add a local PostgreSQL Compose service
- [x] add an isolated H2 test profile for the initial context smoke test
- [x] run the complete initial test suite
- [x] create the initial Git commit and push synchronized `main` to GitHub

### Task 2: Establish API and module foundations

**Files:**

- Create: `src/main/java/com/kivo/backend/common/**`
- Create: `src/main/java/com/kivo/backend/*/package-info.java`
- Create: `src/test/java/com/kivo/backend/architecture/**`
- Create: `docs/api/openapi.yaml`

- [ ] define package/module boundaries and dependency rules
- [ ] introduce stable error codes and RFC 9457 error mapping
- [ ] define API versioning, pagination, language, and identifier conventions in OpenAPI
- [ ] add architecture tests for module and controller boundaries
- [ ] add MVC tests for validation and representative error responses
- [ ] run all tests before Task 3

### Task 3: Create the mall catalog and map persistence model

**Files:**

- Create: `src/main/java/com/kivo/backend/catalog/**`
- Create: `src/main/java/com/kivo/backend/maps/**`
- Create: `src/main/java/com/kivo/backend/kiosks/**`
- Create: `src/main/resources/db/migration/V1__create_core_catalog.sql`
- Create: `src/test/java/com/kivo/backend/{catalog,maps,kiosks}/**`

> **JPA skill required (`spring-data-jpa`).** Activate it before entity/repository implementation.

- [ ] model malls, floors, kiosks, categories, translations, and places
- [ ] create explicit DTOs and application services; keep entities internal
- [ ] add Flyway constraints and indexes with external IDs unique in their proper scope
- [ ] implement seed/import fixtures for a small demo mall
- [ ] test persistence constraints, mappings, localization fallback, and invalid data
- [ ] run all tests before Task 4

### Task 4: Implement catalog, search, and bootstrap APIs

**Files:**

- Create: `src/main/java/com/kivo/backend/catalog/api/**`
- Create: `src/main/java/com/kivo/backend/kiosks/api/**`
- Create: `src/test/java/com/kivo/backend/catalog/api/**`
- Modify: `docs/api/openapi.yaml`

- [ ] implement kiosk bootstrap and place/category read models
- [ ] implement normalized, bounded server-side search and category filtering
- [ ] return localized flat DTOs suitable for Intuiface collections
- [ ] test successful reads, empty results, invalid language, and missing resources
- [ ] verify OpenAPI examples against serialized responses
- [ ] run all tests before Task 5

### Task 5: Persist and validate navigation graphs

**Files:**

- Create: `src/main/java/com/kivo/backend/navigation/graph/**`
- Create: `src/main/java/com/kivo/backend/navigation/persistence/**`
- Create: `src/main/resources/db/migration/V2__create_navigation_graph.sql`
- Create: `src/test/java/com/kivo/backend/navigation/graph/**`

> **JPA skill required (`spring-data-jpa`).** Activate it before entity/repository implementation.

- [ ] create node, edge, connector, geometry, restriction, and graph-version models
- [ ] load immutable graph snapshots per mall/version
- [ ] validate graph connectivity, bounds, lengths, endpoints, and public POI reachability
- [ ] make snapshot publication atomic and observable
- [ ] test valid graphs and every validation failure class
- [ ] run all tests before Task 6

### Task 6: Build the routing engine

**Files:**

- Create: `src/main/java/com/kivo/backend/navigation/routing/**`
- Create: `src/test/java/com/kivo/backend/navigation/routing/**`

- [ ] implement pure-Java Dijkstra as the correctness reference
- [ ] implement A* with an admissible heuristic and deterministic tie-breaking
- [ ] add standard and accessible cost profiles with traversal penalties
- [ ] produce ordered route points, floor transitions, distance, ETA, and instructions
- [ ] test single/multi-floor routes, disconnected graphs, closures, one-way edges, accessibility,
  deterministic ties, and A*/Dijkstra cost equivalence
- [ ] add performance tests for a representative shopping-centre graph
- [ ] run all tests before Task 7

### Task 7: Expose routes and render map overlays

**Files:**

- Create: `src/main/java/com/kivo/backend/navigation/api/**`
- Create: `src/main/java/com/kivo/backend/maps/rendering/**`
- Create: `src/test/java/com/kivo/backend/{navigation/api,maps/rendering}/**`
- Modify: `docs/api/openapi.yaml`

- [ ] implement route creation/retrieval APIs from kiosk to destination
- [ ] render safe transparent SVG overlays in the floor's normalized coordinate system
- [ ] keep Intuiface-facing route collections flat and provide one overlay record per floor
- [ ] return typed errors for invalid kiosk/destination/profile and unreachable routes
- [ ] test JSON contracts, SVG escaping/content type, route lifecycle, and errors
- [ ] run all tests before Task 8

### Task 8: Implement route sharing and QR delivery

**Files:**

- Create: `src/main/java/com/kivo/backend/sharing/**`
- Create: `src/main/resources/db/migration/V3__create_route_shares.sql`
- Create: `src/test/java/com/kivo/backend/sharing/**`
- Modify: `docs/api/openapi.yaml`

> **JPA skill required (`spring-data-jpa`).** Activate it before entity/repository implementation.

- [ ] generate high-entropy share tokens and store only a safe token representation
- [ ] implement expiration and route snapshot/version consistency
- [ ] return `routeUrl` and `qrImageUrl` for direct Intuiface binding
- [ ] expose a mobile JSON representation and minimal responsive route page
- [ ] test token entropy assumptions, expiry, unknown/revoked tokens, and concurrent access
- [ ] run all tests before Task 9

### Task 9: Add events and feedback

**Files:**

- Create: `src/main/java/com/kivo/backend/events/**`
- Create: `src/main/java/com/kivo/backend/feedback/**`
- Create: `src/main/resources/db/migration/V4__create_events_and_feedback.sql`
- Create: `src/test/java/com/kivo/backend/{events,feedback}/**`
- Modify: `docs/api/openapi.yaml`

> **JPA skill required (`spring-data-jpa`).** Activate it before entity/repository implementation.

- [ ] implement localized events sorted in the mall timezone
- [ ] link events to routable places where applicable
- [ ] accept anonymous positive/negative feedback with kiosk/route context
- [ ] validate publication windows and reject malformed or oversized input
- [ ] test timezone boundaries, ordering, localization, feedback validation, and persistence
- [ ] run all tests before Task 10

### Task 10: Harden operations, security, and observability

**Files:**

- Create: `src/main/java/com/kivo/backend/admin/**`
- Create: `src/main/java/com/kivo/backend/common/security/**`
- Create: `src/test/java/com/kivo/backend/{admin,common/security}/**`
- Modify: `src/main/resources/application.properties`, deployment documentation

- [ ] protect administration and graph publication operations
- [ ] configure CORS for known web clients and production HTTPS assumptions
- [ ] add rate limiting strategy for expensive/public endpoints
- [ ] add health/readiness, structured logging, correlation IDs, and route metrics
- [ ] add backup, restore, migration rollback, and graph recovery runbooks
- [ ] test authorization rules, CORS, actuator exposure, and operational failure modes
- [ ] run all tests before Task 11

### Task 11: Validate Intuiface integration

**Files:**

- Create: `docs/integration/intuiface.md`
- Create: `docs/integration/examples/**`
- Modify: `docs/api/openapi.yaml`

- [ ] create an Intuiface spike for categories, route creation, and remote overlay image binding
- [ ] verify API Explorer behaviour for GET/POST, headers, HTTPS, arrays, and error triggers
- [ ] document every screen-to-endpoint binding for Ariadna
- [ ] test idle/reset behaviour without storing backend session state
- [ ] execute the full kiosk scenario against a deployed test environment
- [ ] record and test any contract adjustments before Task 12

### Task 12: Deployment and release readiness

**Files:**

- Create: `Dockerfile`
- Create: `.github/workflows/ci.yml`
- Create: deployment and release documentation

- [ ] create a non-root, reproducible application image
- [ ] configure CI for build, unit/integration tests, formatting, and migration validation
- [ ] configure staging from environment variables and managed secrets
- [ ] run load tests for search/route endpoints and tune measured bottlenecks
- [ ] verify acceptance scenarios and restore procedure on staging
- [ ] tag the first backend release only after all quality gates pass

### Task 13: Final documentation and handoff

**Files:**

- Modify: `README.md`, `docs/api/openapi.yaml`, integration and operations guides

- [ ] verify all functional requirements and edge cases from this plan
- [ ] run the complete unit, integration, architecture, and contract test suites
- [ ] confirm committed migrations build a database from zero
- [ ] document local development, data import, deployment, and troubleshooting
- [ ] help Ariadna complete the final Intuiface integration walkthrough
- [ ] move this plan to `docs/plans/completed/`

## Post-Completion and External Inputs

The following require external information but do not block initial backend development:

- Final floor-plan artwork and stable dimensions from Ariadna.
- Known real-world scale or reference distances for ETA calibration.
- Location of kiosks, destinations, walkable paths, stairs, lifts, and restricted areas.
- Target languages and final translated content.
- Hosting provider, domain, TLS, monitoring destination, and production PostgreSQL service.
- Intuiface Player platform/version and license capabilities.

Before the real demo, run an on-site or plan-based route audit: every public destination must be
reachable from every enabled kiosk under the standard profile, and accessibility claims must be
verified against the real building rather than inferred from artwork.
