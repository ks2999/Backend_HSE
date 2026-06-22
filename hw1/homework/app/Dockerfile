# Stage 1: сборка дистрибутива приложения через Gradle
FROM gradle:8.8-jdk21 AS build
WORKDIR /home/gradle/app

COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src

# installDist собирает приложение со всеми зависимостями в build/install/app
# (скрипт запуска — bin/app)
RUN gradle installDist --no-daemon

# Stage 2: лёгкий рантайм-образ
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /home/gradle/app/build/install/app ./

ENTRYPOINT ["./bin/app"]
CMD ["5", "+", "3"]
