package org.example.spring;

import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;

@SpringBootApplication
public class SpringDataJpaDemo {

    public static void run() {
        System.out.println("=== 4. Spring + Spring Data JPA: таблица users ===");

        ConfigurableApplicationContext context = new SpringApplicationBuilder(SpringDataJpaDemo.class)
            .web(WebApplicationType.NONE)
            .bannerMode(Banner.Mode.OFF)
            .run();

        try {
            UserRepository users = context.getBean(UserRepository.class);
            seed(users);
            queries(users);
        } finally {
            context.close();
        }

        System.out.println();
    }

    private static void seed(UserRepository users) {
        users.deleteAll();
        users.saveAll(List.of(
            new User("alice", "alice@example.com"),
            new User("bob", "bob@example.org"),
            new User("carol", "carol@example.com")
        ));
        System.out.println("Сохранено пользователей: " + users.count());
    }

    private static void queries(UserRepository users) {
        System.out.println("findAll():");
        users.findAll().forEach(u -> System.out.println("  " + u));

        users.findByUsername("alice")
            .ifPresent(u -> System.out.println("findByUsername(\"alice\"): " + u));

        System.out.println("findByEmailContainingIgnoreCaseOrderByUsername(\"example.com\"):");
        users.findByEmailContainingIgnoreCaseOrderByUsername("example.com")
            .forEach(u -> System.out.println("  " + u));

        System.out.println("findByEmailDomain(\"example.org\"):");
        users.findByEmailDomain("example.org")
            .forEach(u -> System.out.println("  " + u));
    }

    public static void main(String[] args) {
        run();
    }
}
