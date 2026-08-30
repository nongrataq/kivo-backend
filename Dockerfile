# syntax=docker/dockerfile:1.7@sha256:a57df69d0ea827fb7266491f2813635de6f17269be881f696fbfdf2d83dda33e

ARG TEMURIN_BUILD_IMAGE=eclipse-temurin:25.0.3_9-jdk-jammy@sha256:f122992af75e61d87892f8a37c60f7cfa498b18748c1c9f8563da9a3b1893278
ARG TEMURIN_RUNTIME_IMAGE=eclipse-temurin:25.0.3_9-jre-jammy@sha256:5bd5dbe00f40ea149de434a75029713765a2912cfc1fd770cc7c7aff007384ea

FROM ${TEMURIN_BUILD_IMAGE} AS build

WORKDIR /workspace

COPY gradlew build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
RUN chmod 0755 gradlew

COPY src ./src
COPY deployment/docker/HealthCheck.java ./deployment/docker/HealthCheck.java

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon bootJar \
    && cp build/libs/kivo-backend-*.jar /workspace/app.jar \
    && javac -d /workspace/healthcheck deployment/docker/HealthCheck.java \
    && touch --date=@0 /workspace/app.jar /workspace/healthcheck/HealthCheck.class

FROM ${TEMURIN_RUNTIME_IMAGE} AS runtime

LABEL org.opencontainers.image.title="Kivo Backend" \
      org.opencontainers.image.description="Indoor navigation backend for large retail spaces" \
      org.opencontainers.image.source="https://github.com/nongrataq/kivo-backend"

WORKDIR /opt/kivo

COPY --from=build --chown=10001:10001 /workspace/app.jar ./app.jar
COPY --from=build --chown=10001:10001 /workspace/healthcheck ./healthcheck

USER 10001:10001
EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=12 \
  CMD ["java", "-cp", "/opt/kivo/healthcheck", "HealthCheck"]

ENTRYPOINT ["java", "-jar", "/opt/kivo/app.jar"]
