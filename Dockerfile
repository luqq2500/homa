# The Homa container for ECS/Fargate (docs/decisions/0001-runtime-ecs-fargate.md).
# Build the jar first, once, and ship the same image to every environment:
#   mvn -B verify && docker build -t homa .

# Split the Spring Boot jar into its layers, so a code change rebuilds only the small application layer.
FROM eclipse-temurin:21-jre AS layers
WORKDIR /layers
COPY target/homa-*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:21-jre
RUN groupadd --system --gid 10001 homa \
    && useradd --system --uid 10001 --gid homa --no-create-home --shell /usr/sbin/nologin homa
WORKDIR /app
COPY --from=layers /layers/extracted/dependencies/ ./
COPY --from=layers /layers/extracted/spring-boot-loader/ ./
COPY --from=layers /layers/extracted/snapshot-dependencies/ ./
COPY --from=layers /layers/extracted/application/ ./
USER 10001:10001

# Size the heap from the task's memory, not the host's.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
# Exec form, so the JVM is PID 1 and receives ECS's SIGTERM for a graceful shutdown.
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
