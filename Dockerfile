# Build: dependencies first so code-only changes reuse the cached layer.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 1001 app && mkdir -p /data/uploads && chown app /data/uploads
WORKDIR /app
COPY --from=build /src/target/*.jar app.jar
USER app
# Heap follows the container's memory limit (mem_limit in docker-compose.yml).
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
