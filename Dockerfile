### ---------- Stage 1: Build ----------
FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace

# Copy Maven wrapper and pom first so dependency layers are cached
# separately from source code changes (faster rebuilds).
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Now copy the actual source and build the application.
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

### ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:21-jre-jammy AS runtime

# Run as a non-root user for security.
RUN groupadd -r quarkus && useradd -r -g quarkus quarkus

WORKDIR /deployments

# Quarkus JVM-mode build produces this exact directory structure.
COPY --from=build /workspace/target/quarkus-app/lib/ ./lib/
COPY --from=build /workspace/target/quarkus-app/*.jar ./
COPY --from=build /workspace/target/quarkus-app/app/ ./app/
COPY --from=build /workspace/target/quarkus-app/quarkus/ ./quarkus/

RUN chown -R quarkus:quarkus /deployments
USER quarkus

EXPOSE 8080

ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0"

ENTRYPOINT ["java", "-jar", "quarkus-run.jar"]
