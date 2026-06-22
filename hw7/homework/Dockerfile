FROM gradle:8.5-jdk21 AS build
WORKDIR /home/gradle/app

COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src

RUN gradle build

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /home/gradle/app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
