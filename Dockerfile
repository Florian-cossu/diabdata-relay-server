# ---- Build stage ----
FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle :relay:installDist --no-daemon

# ---- Run stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/relay/build/install/relay/ ./

EXPOSE 8080

ENTRYPOINT ["./bin/relay"]