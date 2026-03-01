# ===================================
# Stage 1: Build Stage
# ===================================
FROM gradle:8.5-jdk17-alpine AS build

WORKDIR /build

# Copy Gradle wrapper and dependency files first for layer caching
COPY gradle gradle
COPY gradlew .
COPY gradle.properties .
COPY settings.gradle.kts .
COPY build.gradle.kts .

# Download dependencies (cached layer)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY src src

# Build application (skip tests for faster builds, run tests in CI/CD)
RUN ./gradlew clean build -x test --no-daemon --info

# Verify JAR was created
RUN ls -lh /build/build/libs/ && \
    test -f /build/build/libs/*.jar || (echo "ERROR: JAR file not found" && exit 1)

# ===================================
# Stage 2: Runtime Stage
# ===================================
FROM eclipse-temurin:17-jre-alpine

# Install dumb-init for proper signal handling
RUN apk add --no-cache dumb-init

# Create non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy JAR from build stage
COPY --from=build --chown=appuser:appgroup /build/build/libs/*.jar app.jar

# Switch to non-root user
USER appuser:appgroup

# Expose application port
EXPOSE 8080

# Health check using Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Use dumb-init to handle signals properly (graceful shutdown)
ENTRYPOINT ["dumb-init", "--"]

# Run application with optimized JVM settings
CMD ["java", \
     "-XX:+UseContainerSupport", \
     "-XX:MaxRAMPercentage=75.0", \
     "-XX:InitialRAMPercentage=50.0", \
     "-XX:+UseG1GC", \
     "-XX:+UseStringDeduplication", \
     "-Djava.security.egd=file:/dev/./urandom", \
     "-jar", \
     "app.jar"]
