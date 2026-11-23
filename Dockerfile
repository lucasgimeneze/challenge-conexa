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

# 60 % de heap deja margen para metaspace, threads y code cache en contenedores chicos (512 MB).
# Como variable de entorno, se ajusta al desplegar sin reconstruir la imagen.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60"

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD curl -fs http://localhost:${SERVER_PORT:-8080}/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
