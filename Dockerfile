# ---- build stage -----------------------------------------------------------
# Java 25 (the current LTS) is required: the build targets release 25.
FROM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /build

# Copy the wrapper and POM first so the dependency layer is cached and only re-resolves
# when the POM itself changes - not on every source edit.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
# Integration tests need Docker (Testcontainers), which is not available inside the build.
# They run in CI against a real daemon; the image build only compiles and packages.
RUN ./mvnw -B -q clean package -DskipTests

# ---- runtime stage ---------------------------------------------------------
FROM eclipse-temurin:25-jre-alpine AS runtime

# Never run as root.
RUN addgroup -S payments && adduser -S payments -G payments

WORKDIR /app
COPY --from=build --chown=payments:payments /build/target/payments-api-*.jar app.jar

USER payments
EXPOSE 8080

# MaxRAMPercentage rather than a fixed -Xmx: the JVM then sizes the heap from the container's
# cgroup limit, so the same image behaves correctly at 512 MB or 4 GB.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseZGC -XX:+ExitOnOutOfMemoryError"

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
