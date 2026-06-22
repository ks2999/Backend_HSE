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
    // JDBC-драйвер PostgreSQL
    implementation("org.postgresql:postgresql:42.7.7")

    // Hibernate ORM + JPA API
    implementation("org.hibernate.orm:hibernate-core:6.6.2.Final")
    // Интеграция Hibernate с пулом соединений HikariCP
    implementation("org.hibernate.orm:hibernate-hikaricp:6.6.2.Final")
    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")

    // HikariCP нужен напрямую для демо уровней изоляции (чистый JDBC)
    implementation("com.zaxxer:HikariCP:5.1.0")

    // Логирование через SLF4J (иначе Hibernate/Hikari ругаются на отсутствие биндинга)
    implementation("org.slf4j:slf4j-simple:2.0.13")
}

application {
    mainClass.set("org.example.Main")
    // Маршрутизируем логи Hibernate (JBoss Logging) через SLF4J,
    // чтобы их уровнем управлял simplelogger.properties (видны только демо).
    applicationDefaultJvmArgs = listOf("-Dorg.jboss.logging.provider=slf4j")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
