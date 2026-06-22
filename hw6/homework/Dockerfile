# Stage 1: сборка исполняемого jar (Spring Boot bootJar)
FROM gradle:8.8-jdk21 AS build
WORKDIR /home/gradle/app

COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src

RUN gradle bootJar --no-daemon

# Stage 2: лёгкий рантайм-образ
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /home/gradle/app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
