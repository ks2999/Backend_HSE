package org.example;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;

public final class Db {

    public static final String URL =
        env("DB_URL", "jdbc:postgresql://localhost:5433/mydatabase");
    public static final String USER = env("DB_USER", "admin");
    public static final String PASSWORD = env("DB_PASSWORD", "password");

    private Db() {
    }

    public static EntityManagerFactory buildEntityManagerFactory() {
        Map<String, Object> overrides = new HashMap<>();
        overrides.put("hibernate.connection.url", URL);
        overrides.put("hibernate.connection.username", USER);
        overrides.put("hibernate.connection.password", PASSWORD);
        return Persistence.createEntityManagerFactory("homework-pu", overrides);
    }

    public static void reset(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.createNativeQuery(
                "TRUNCATE TABLE order_product, orders, products, customers, categories, accounts "
                    + "RESTART IDENTITY CASCADE").executeUpdate();
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v;
    }
}
