# syntax=docker/dockerfile:1

# ---- Build: compile an uberjar ----
FROM clojure:temurin-25-tools-deps-noble AS build
WORKDIR /app

# Fetch dependencies in their own layer, so it's reused until deps.edn changes.
COPY deps.edn build.clj ./
RUN clojure -P && clojure -A:build -P

COPY src ./src
COPY resources ./resources
RUN clojure -T:build uber

# ---- Runtime: JRE and the jar only ----
FROM eclipse-temurin:25-jre-noble
WORKDIR /app

# curl is only here for the HEALTHCHECK below.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app && useradd --system --gid app --no-create-home app
COPY --from=build /app/target/app.jar ./app.jar
USER app

# Database settings: DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
ENV PORT=3000
EXPOSE 3000

HEALTHCHECK --start-period=30s --interval=30s --timeout=3s \
    CMD curl -fsS "http://localhost:${PORT}/healthz" || exit 1

# Arguments after the image name go to -main:
#   docker run <image>           start the web server
#   docker run <image> migrate   apply pending migrations and exit
# Exec form keeps java as PID 1, so `docker stop` (SIGTERM) runs the
# shutdown hook that stops Jetty and closes the connection pool.
#   CrashOnOutOfMemoryError: exit instead of limping on, so it gets restarted
# Extra JVM flags can be passed at run time with JDK_JAVA_OPTIONS.
ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=75", \
    "-XX:+CrashOnOutOfMemoryError", \
    "-jar", "/app/app.jar"]
