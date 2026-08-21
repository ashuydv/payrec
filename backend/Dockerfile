# --- Build stage ---
# Alpine variants of these tags are amd64-only; the non-alpine tags support
# arm64 (Apple Silicon) too, at the cost of a somewhat larger image.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Cache dependencies separately from source so `docker build` skips the
# (slow) dependency download step when only application code changes.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -q dependency:go-offline

COPY src src
RUN ./mvnw -q package -DskipTests

# --- Run stage ---
FROM eclipse-temurin:17-jre AS run
WORKDIR /app

RUN groupadd -r payrecon && useradd -r -g payrecon payrecon
COPY --from=build /app/target/*.jar app.jar
RUN chown payrecon:payrecon app.jar
USER payrecon

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
