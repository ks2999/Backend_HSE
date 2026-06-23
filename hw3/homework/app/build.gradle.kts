plugins {
    java
    application

    id("io.spring.dependency-management") version "1.1.6"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.0")
    }
}

dependencies {
    implementation("org.postgresql:postgresql")

    implementation("org.hibernate.orm:hibernate-core")
    implementation("org.hibernate.orm:hibernate-hikaricp")
    implementation("jakarta.persistence:jakarta.persistence-api")
    implementation("com.zaxxer:HikariCP")
    implementation("org.slf4j:slf4j-simple")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa") {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
    }
}

application {
    mainClass.set("org.example.Main")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
