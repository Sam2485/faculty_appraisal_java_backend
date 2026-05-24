# =============================================================================
# Stage 1 — Build the Spring Boot fat jar
# =============================================================================
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY mvnw .
RUN chmod +x mvnw

COPY .mvn/ .mvn/
COPY pom.xml .

RUN ./mvnw dependency:go-offline -q --no-transfer-progress

COPY src ./src

RUN ./mvnw package -DskipTests -q --no-transfer-progress

# =============================================================================
# Stage 2 — Lean JRE runtime image
# =============================================================================
FROM eclipse-temurin:21-jre-alpine

# Run as a non-root user (required for local server deployment)
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Pre-create uploads dir for USE_LOCAL_STORAGE=true mode
RUN mkdir -p /app/uploads && chown spring:spring /app/uploads

COPY --from=build /app/target/*.jar app.jar

USER spring:spring

EXPOSE 8080

# Container-aware JVM: reads cgroup limits, caps heap at 75% of container RAM.
# Prevents OOM kills on Cloud Run's default 512 MB / 1 GB instances.
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:InitialRAMPercentage=50.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.jmx.enabled=false", \
    "-Dfile.encoding=UTF-8", \
    "-jar", "app.jar"]
