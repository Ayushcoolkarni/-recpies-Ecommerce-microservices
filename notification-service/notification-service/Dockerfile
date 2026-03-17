# ─────────────────────────────────────────────────────────────────────────────
#  Multi-stage Dockerfile for all Spring Boot microservices.
#
#  Place one copy of this file in each service root:
#    service-registry/Dockerfile
#    api-gateway/Dockerfile
#    user-service/Dockerfile
#    recipe-service/Dockerfile
#    inventory-service/inventory-service/Dockerfile
#    order-service/order-service/Dockerfile
#    payment-service/Dockerfile
#    notification-service/notification-service/Dockerfile
#    admin-service/Dockerfile
#
#  Build (Docker does this automatically via docker-compose):
#    docker build -t ecom-user-service .
# ─────────────────────────────────────────────────────────────────────────────

# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app

# Copy Maven wrapper and pom first — Docker caches this layer
# so dependencies are only re-downloaded when pom.xml changes
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -q

# Copy source and build
COPY src ./src
RUN ./mvnw package -DskipTests -q

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

# Copy the fat jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Spring Boot listens on the port set by SERVER_PORT env var
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
