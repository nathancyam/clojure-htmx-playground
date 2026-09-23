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

RUN groupadd --system app && useradd --system --gid app --no-create-home app
COPY --from=build /app/target/app.jar ./app.jar
USER app

# Database settings: DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
ENV PORT=3000
EXPOSE 3000

# Arguments after the image name go to -main:
#   docker run <image>           start the web server
#   docker run <image> migrate   apply pending migrations and exit
# Exec form keeps java as PID 1, so `docker stop` (SIGTERM) runs the
# shutdown hook that stops Jetty and closes the connection pool.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
