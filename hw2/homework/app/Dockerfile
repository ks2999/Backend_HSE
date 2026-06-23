FROM gradle:8.8-jdk21 AS build
WORKDIR /home/gradle/app

COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src

RUN gradle installDist --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /home/gradle/app/build/install/app ./

ENTRYPOINT ["./bin/app"]
