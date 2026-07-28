# syntax=docker/dockerfile:1

# ============================================================================
# Stage 1: Build — compile & package with Maven, then explode the fat jar
# into a thin application jar + external lib/ folder (Spring Boot 4 "tools"
# jarmode) so dependency layers cache separately from the app layer.
# ============================================================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Leverage Docker layer caching for dependencies
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q clean package -DskipTests \
    && mkdir -p target/extracted \
    && java -Djarmode=tools -jar target/*.jar extract --destination target/extracted

# ============================================================================
# Stage 2: Runtime — minimal JRE, non-root.
# ============================================================================
FROM eclipse-temurin:17-jre-jammy AS runtime

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system spring && useradd --system --gid spring --no-create-home spring \
    && mkdir -p /app/rasp-logs \
    && chown -R spring:spring /app

WORKDIR /app

COPY --from=build --chown=spring:spring /build/target/extracted/lib/ ./lib/
COPY --from=build --chown=spring:spring /build/target/extracted/*.jar ./app.jar

USER spring:spring

ENV JAVA_OPTS="" \
    RASP_ENABLED=true \
    RASP_MODE=block \
    RASP_LOG_DIR=/app/rasp-logs \
    SERVER_PORT=8080

EXPOSE 8080
VOLUME ["/app/rasp-logs"]

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD curl -fs http://localhost:${SERVER_PORT}/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
