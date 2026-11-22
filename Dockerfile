# syntax=docker/dockerfile:1

# Build: compila con el Maven wrapper del repo. Los tests corren en el build local o en CI, no acá.
FROM eclipse-temurin:21.0.13.1_2-jdk AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline
COPY src src
RUN ./mvnw -q package -DskipTests

# Runtime: solo el JRE y el jar, con un usuario sin privilegios.
FROM eclipse-temurin:21.0.13.1_2-jre
RUN groupadd --system app && useradd --system --gid app app
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
USER app

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD curl -fs http://localhost:${SERVER_PORT:-8080}/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
