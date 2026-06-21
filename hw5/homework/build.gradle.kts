plugins {
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.6"
    java
}

group = "org.example"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    runtimeOnly("org.postgresql:postgresql")
    implementation("com.clickhouse:clickhouse-jdbc:0.7.1:all")

    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")

    implementation("software.amazon.awssdk:s3:2.29.0")
    implementation("software.amazon.awssdk:apache-client:2.29.0")

    implementation("com.hazelcast:hazelcast:5.5.0")
}

springBoot {
    mainClass.set("org.example.storages.AllStoragesMain")
}
