package org.example;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Main {

    public static void main(String[] args) {
        String url1     = env("DB1_URL", "jdbc:postgresql://localhost:5432/users_db");
        String url2     = env("DB2_URL", "jdbc:postgresql://localhost:5433/products_db");
        String user     = env("DB_USER", "admin");
        String password = env("DB_PASSWORD", "password");

        try (HikariDataSource ds1 = createPool("pool-users-db", url1, user, password);
             HikariDataSource ds2 = createPool("pool-products-db", url2, user, password)) {

            System.out.println("=== БД №1 (users_db) через Hikari ===");
            queryUsers(ds1);

            System.out.println();
            System.out.println("=== БД №2 (products_db) через Hikari ===");
            queryProducts(ds2);
        }
    }

    private static HikariDataSource createPool(String name, String url, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setPoolName(name);
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10_000);
        config.setConnectionTestQuery("SELECT 1");
        return new HikariDataSource(config);
    }

    private static void queryUsers(HikariDataSource ds) {
        try (Connection conn = ds.getConnection()) {

            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM users");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Всего пользователей: " + rs.getLong(1));
                }
            }

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

    private static void queryProducts(HikariDataSource ds) {
        try (Connection conn = ds.getConnection()) {

            try (PreparedStatement ps = conn.prepareStatement("SELECT AVG(price) FROM products");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.printf("Средняя цена товара: %.2f%n", rs.getDouble(1));
                }
            }

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
