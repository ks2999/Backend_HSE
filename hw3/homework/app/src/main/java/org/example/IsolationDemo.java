package org.example;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Демонстрация уровней изоляции транзакций на примере «неповторяемого чтения»
 * (non-repeatable read).
 *
 * <p>Транзакция T1 дважды читает один и тот же баланс. Между этими чтениями
 * транзакция T2 меняет баланс и делает COMMIT.
 *
 * <ul>
 *   <li><b>READ COMMITTED</b> — второе чтение видит новое значение
 *       (неповторяемое чтение допускается);</li>
 *   <li><b>REPEATABLE READ</b> — T1 работает со снимком данных на момент старта,
 *       поэтому оба чтения совпадают (неповторяемое чтение исключено).</li>
 * </ul>
 *
 * <p>Используется чистый JDBC поверх пула HikariCP — так нагляднее виден
 * контроль над уровнем изоляции и границами транзакций. (В PostgreSQL уровень
 * READ UNCOMMITTED работает как READ COMMITTED, «грязное чтение» невозможно.)
 */
public final class IsolationDemo {

    private IsolationDemo() {
    }

    public static void run(String url, String user, String password) {
        System.out.println("=== 3. Уровни изоляции: неповторяемое чтение ===");

        try (HikariDataSource ds = pool(url, user, password)) {
            long accId = createIsoAccount(ds);
            scenario(ds, accId, Connection.TRANSACTION_READ_COMMITTED, "READ COMMITTED");
            scenario(ds, accId, Connection.TRANSACTION_REPEATABLE_READ, "REPEATABLE READ");
        }

        System.out.println();
    }

    private static void scenario(HikariDataSource ds, long accId, int level, String levelName) {
        resetBalance(ds, accId, new BigDecimal("1000"));
        System.out.println("--- " + levelName + " ---");

        // T2: спустя время меняет баланс и коммитит — это происходит «между»
        // двумя чтениями транзакции T1.
        Thread writer = new Thread(() -> {
            try {
                Thread.sleep(700);
                try (Connection w = ds.getConnection()) {
                    w.setAutoCommit(true);
                    try (PreparedStatement ps =
                             w.prepareStatement("UPDATE accounts SET balance = ? WHERE id = ?")) {
                        ps.setBigDecimal(1, new BigDecimal("2000"));
                        ps.setLong(2, accId);
                        ps.executeUpdate();
                    }
                }
                System.out.println("  T2: обновил баланс -> 2000 и сделал COMMIT");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        try (Connection r = ds.getConnection()) {
            r.setAutoCommit(false);
            r.setTransactionIsolation(level);

            writer.start();

            BigDecimal r1 = readBalance(r, accId);   // первый SELECT задаёт снимок при REPEATABLE READ
            System.out.println("  T1: первое чтение      balance=" + plain(r1));

            Thread.sleep(1500);                       // ждём, пока T2 успеет закоммитить

            BigDecimal r2 = readBalance(r, accId);
            System.out.println("  T1: повторное чтение   balance=" + plain(r2));
            r.commit();

            if (r1.compareTo(r2) != 0) {
                System.out.println("  => неповторяемое чтение: значение изменилось внутри транзакции ("
                    + plain(r1) + " -> " + plain(r2) + ")");
            } else {
                System.out.println("  => чтение повторяемо: оба раза " + plain(r1)
                    + " (T1 видит снимок на момент старта)");
            }
        } catch (SQLException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                writer.join();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static HikariDataSource pool(String url, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setPoolName("isolation-demo");
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(4);
        return new HikariDataSource(config);
    }

    private static long createIsoAccount(HikariDataSource ds) {
        String sql = "INSERT INTO accounts(name, balance) VALUES ('iso-account', 1000) RETURNING id";
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void resetBalance(HikariDataSource ds, long id, BigDecimal value) {
        try (Connection conn = ds.getConnection();
             PreparedStatement ps =
                 conn.prepareStatement("UPDATE accounts SET balance = ? WHERE id = ?")) {
            ps.setBigDecimal(1, value);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static BigDecimal readBalance(Connection conn, long id) throws SQLException {
        try (PreparedStatement ps =
                 conn.prepareStatement("SELECT balance FROM accounts WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1);
            }
        }
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
