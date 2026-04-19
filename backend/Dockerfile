# syntax=docker/dockerfile:1.6

# ---- Build stage ----
FROM gradle:8.10-jdk17 AS build
WORKDIR /workspace

# Copy Gradle files first for better layer caching
COPY settings.gradle build.gradle ./
COPY gradle ./gradle
# Warm the cache (no sources yet)
RUN gradle --no-daemon dependencies || true

# Now copy sources and build
COPY src ./src
RUN gradle --no-daemon bootJar -x test

# ---- Run stage ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Non-root user for safety
RUN useradd -r -u 1001 -g root appuser

COPY --from=build /workspace/build/libs/app.jar /app/app.jar
RUN chown -R appuser:root /app

USER appuser
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "/app/app.jar"]
