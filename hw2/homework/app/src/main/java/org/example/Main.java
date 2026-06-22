package org.example;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Приложение подключается СРАЗУ К ДВУМ независимым базам PostgreSQL
 * (каждая в своей docker-сети) через два отдельных пула HikariCP
 * и выполняет к каждой по паре запросов.
 *
 * Адреса БД можно переопределить переменными окружения, поэтому одно и то же
 * приложение работает и с хоста (localhost:5432 / localhost:5433),
 * и из контейнера (postgres1:5432 / postgres2:5432).
 */
public class Main {

    public static void main(String[] args) {
        String url1     = env("DB1_URL", "jdbc:postgresql://localhost:5432/users_db");
        String url2     = env("DB2_URL", "jdbc:postgresql://localhost:5433/products_db");
        String user     = env("DB_USER", "admin");
        String password = env("DB_PASSWORD", "password");

        // try-with-resources: оба пула гарантированно закроются в конце
        try (HikariDataSource ds1 = createPool("pool-users-db", url1, user, password);
             HikariDataSource ds2 = createPool("pool-products-db", url2, user, password)) {

            System.out.println("=== БД №1 (users_db) через Hikari ===");
            queryUsers(ds1);

            System.out.println();
            System.out.println("=== БД №2 (products_db) через Hikari ===");
            queryProducts(ds2);
        }
    }

    /** Отдельный пул соединений HikariCP под каждую БД. */
    private static HikariDataSource createPool(String name, String url, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setPoolName(name);
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(5);          // макс. число соединений в пуле
        config.setMinimumIdle(1);              // мин. число простаивающих соединений
        config.setConnectionTimeout(10_000);   // сколько ждать соединение из пула
        config.setConnectionTestQuery("SELECT 1");
        return new HikariDataSource(config);
    }

    /** Пара запросов к первой БД. */
    private static void queryUsers(HikariDataSource ds) {
        try (Connection conn = ds.getConnection()) {

            // запрос №1 — агрегат
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM users");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Всего пользователей: " + rs.getLong(1));
                }
            }

            // запрос №2 — выборка строк
            try (PreparedStatement ps = conn.prepareStatement(
                         "SELECT id, username, email FROM users ORDER BY id");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    System.out.printf("  #%d  %-12s %s%n",
                            rs.getLong("id"), rs.getString("username"), rs.getString("email"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Пара запросов ко второй БД. */
    private static void queryProducts(HikariDataSource ds) {
        try (Connection conn = ds.getConnection()) {

            // запрос №1 — агрегат
            try (PreparedStatement ps = conn.prepareStatement("SELECT AVG(price) FROM products");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.printf("Средняя цена товара: %.2f%n", rs.getDouble(1));
                }
            }

            // запрос №2 — выборка с параметром (PreparedStatement)
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, name, price FROM products WHERE price > ? ORDER BY price DESC")) {
                ps.setBigDecimal(1, new BigDecimal("20.00"));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        System.out.printf("  #%d  %-12s %s%n",
                                rs.getLong("id"), rs.getString("name"), rs.getBigDecimal("price"));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v;
    }
}
