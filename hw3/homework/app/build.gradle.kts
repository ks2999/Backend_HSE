plugins {
    java
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.postgresql:postgresql:42.7.7")

    implementation("org.hibernate.orm:hibernate-core:6.6.2.Final")
    implementation("org.hibernate.orm:hibernate-hikaricp:6.6.2.Final")
    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")

    implementation("com.zaxxer:HikariCP:5.1.0")

    implementation("org.slf4j:slf4j-simple:2.0.13")
}

application {
    mainClass.set("org.example.Main")
    applicationDefaultJvmArgs = listOf("-Dorg.jboss.logging.provider=slf4j")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
