package org.example.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.example.jpa.audit.AuditingDemo;
import org.example.jpa.nplus1.NPlusOneDemo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Random;

/**
 * ДЗ: воспроизвести ВСЕ примеры из jdbc/JdbcTemplateMain, но на JPA.
 *
 * Соответствие:
 *   JdbcTemplate.update(...)              -> EntityManager.persist(...) / JPQL executeUpdate
 *   JdbcTemplate.query(...)               -> EntityManager.createQuery("SELECT ...")
 *   JdbcTemplate.queryForObject(COUNT)    -> JPQL "SELECT COUNT(u) FROM User u"
 *   string_agg                            -> native query через EntityManager
 *   TransactionTemplate (+ isolation)     -> то же самое, только менеджер транзакций JPA
 *
 * Важные отличия JPA от «голого» JDBC, которые видны в примерах:
 *   1) Запись (persist) требует активной транзакции (нет авто-commit как у JdbcTemplate).
 *   2) Кэш первого уровня (persistence context): повторное чтение управляемой сущности
 *      в одной транзакции вернёт её из кэша. Чтобы увидеть феномены изоляции (повторное
 *      чтение), делаем em.clear() перед повторным запросом.
 *
 * Имена методов оставлены как в оригинале — для удобства сверки.
 * Раскомментируй нужный пример в main().
 */
@SpringBootApplication
public class JdbcExamplesInJpa {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(JdbcExamplesInJpa.class, args);

        // ДЗ «на дом»: отдельные демонстрации, выбираются аргументом запуска
        //   ./gradlew bootRun --args="audit"    -> Spring Data JPA Auditing
        //   ./gradlew bootRun --args="nplus1"   -> проблема N+1 и её решения
        // без аргумента выполняются примеры JDBC -> JPA ниже.
        if (args.length > 0) {
            switch (args[0]) {
                case "audit" -> {
                    AuditingDemo.run(context);
                    context.close();
                    return;
                }
                case "nplus1" -> {
                    NPlusOneDemo.run(context);
                    context.close();
                    return;
                }
                default -> { /* падаем в примеры JDBC -> JPA */ }
            }
        }

        // Общий (thread-safe) EntityManager-proxy: внутри транзакции делегирует к транзакционному EM
        // текущего потока — поэтому один и тот же em можно использовать в разных виртуальных потоках.
        // Берём после старта контекста (а не через @Bean), чтобы не инициализировать EMF раньше DataSource.
        EntityManager em = SharedEntityManagerCreator.createSharedEntityManager(
            context.getBean(EntityManagerFactory.class));
        TransactionTemplate tx = context.getBean(TransactionTemplate.class);
        PlatformTransactionManager txManager = context.getBean(PlatformTransactionManager.class);

        clearUsers(em, tx);

        List<User> users = List.of(
            new User("John Doe " + new Random().nextInt(1000), "Test"),
            new User("Jane Doe " + new Random().nextInt(1000), "Test"),
            new User("Jane Doe " + new Random().nextInt(1000), "Test")
        );

        // --- выбери пример (как закомментированный список в JdbcTemplateMain) ---
        insertUser(em, tx, users.get(0));
//        insertMultipleUsers(em, tx, users);
//        insertWithTransaction(em, tx, users);
//        insertWithTransactionRollback(em, tx, users);
//        testReadNotCommited(em, tx, users);
//        notDirtyRead(em, tx, txManager);
//        dirtyRead(em, tx, txManager);
//        repeatableRead(em, tx, txManager);
//        notRepeatableRead(em, tx, txManager);
//        phantomRead(em, tx, txManager);
//        notPhantomRead(em, tx, txManager);
//        anomalyExample(em, tx, txManager);
//        notAnomalyExample(em, tx, txManager);
//        propagationRequired(em, tx);
//        propagationSupport(em, tx, txManager);
//        propagationRequiredNew(em, txManager);

//        countUsers(em);
        printUsers(em);
        clearUsers(em, tx);

        context.close();
    }

    // ===================== базовые операции =====================

    /** Аналог: jdbcTemplate.execute("DELETE FROM users"). JPQL bulk-delete (нужна транзакция). */
    static void clearUsers(EntityManager em, TransactionTemplate tx) {
        tx.execute(status -> em.createQuery("DELETE FROM User").executeUpdate());
    }

    /** Аналог: jdbcTemplate.query("SELECT ...", rowMapper). */
    static void printUsers(EntityManager em) {
        em.createQuery("SELECT u FROM User u ORDER BY u.id", User.class)
            .getResultList()
            .forEach(System.out::println);
    }

    /** Аналог: jdbcTemplate.queryForObject("SELECT COUNT(*) ...", Integer.class). */
    static void countUsers(EntityManager em) {
        Long count = em.createQuery("SELECT COUNT(u) FROM User u", Long.class).getSingleResult();
        System.out.println(count);
    }

    /** Аналог: jdbcTemplate.queryForObject("SELECT string_agg(name, ', ') ...").
     *  В JPQL переносимого string_agg нет — используем native query через EntityManager. */
    static void concatUsersName(EntityManager em) {
        Object result = em.createNativeQuery("SELECT string_agg(name, ', ') FROM users").getSingleResult();
        System.out.println(result);
    }

    /** Аналог: jdbcTemplate.update("INSERT ...", name, about). */
    static void insertUser(EntityManager em, TransactionTemplate tx, User user) {
        tx.execute(status -> {
            em.persist(user);
            return null;
        });
    }

    /** Аналог: jdbcTemplate.batchUpdate(...). Батчинг включается hibernate.jdbc.batch_size. */
    static void insertMultipleUsers(EntityManager em, TransactionTemplate tx, List<User> users) {
        tx.execute(status -> {
            users.forEach(em::persist);
            return null;
        });
    }

    // ===================== транзакции =====================

    static void insertWithTransaction(EntityManager em, TransactionTemplate tx, List<User> users) {
        tx.execute(status -> {
            users.forEach(user -> {
                em.persist(user);
                printUsers(em);   // SELECT инициирует авто-flush — вставки уже видны внутри транзакции
                System.out.println("-----------");
            });
            return null;
        });
    }

    static void insertWithTransactionRollback(EntityManager em, TransactionTemplate tx, List<User> users) {
        tx.execute(status -> {
            users.forEach(user -> {
                em.persist(user);
                printUsers(em);
                System.out.println("-----------");
                status.setRollbackOnly();   // в конце всё откатится
            });
            return null;
        });
    }

    /** Наблюдатель считает строки, пока другой поток вставляет их в одной транзакции:
     *  до commit-а insert не виден (читаем committed). */
    static void testReadNotCommited(EntityManager em, TransactionTemplate tx, List<User> users) {
        Thread threadInsert = Thread.startVirtualThread(() ->
            tx.execute(status -> {
                users.forEach(user -> {
                    em.persist(user);
                    sleep(1_000);
                });
                return null;
            })
        );
        Thread.startVirtualThread(() -> {
            while (threadInsert.isAlive()) {
                countUsers(em);
                sleep(100);
            }
        });
        join(threadInsert);
    }

    // ===================== уровни изоляции =====================

    /** Нет грязного чтения: транзакция 1 меняет и откатывает, транзакция 2 видит старое значение. */
    static void notDirtyRead(EntityManager em, TransactionTemplate tx, PlatformTransactionManager txm) {
        insertUser(em, tx, new User("Luci", "Test"));

        Thread thread1 = Thread.startVirtualThread(() ->
            tx.execute(status -> {
                em.createQuery("UPDATE User u SET u.about = concat(u.about, ' Test') WHERE u.name = 'Luci'")
                    .executeUpdate();
                sleep(3_000);
                status.setRollbackOnly();
                return null;
            })
        );
        Thread thread2 = Thread.startVirtualThread(() ->
            tx.execute(status -> {
                sleep(1_000);
                printUsers(em);
                return null;
            })
        );
        join(thread1, thread2);
    }

    /** Попытка грязного чтения (READ UNCOMMITTED). В PostgreSQL не сработает —
     *  он трактует READ UNCOMMITTED как READ COMMITTED. */
    static void dirtyRead(EntityManager em, TransactionTemplate base, PlatformTransactionManager txm) {
        TransactionTemplate tx = withIsolation(txm, TransactionDefinition.ISOLATION_READ_UNCOMMITTED);
        insertUser(em, base, new User("Luci", "Test"));

        Thread thread1 = Thread.startVirtualThread(() ->
            tx.execute(status -> {
                em.createQuery("UPDATE User u SET u.about = concat(u.about, ' Test') WHERE u.name = 'Luci'")
                    .executeUpdate();
                sleep(3_000);
                status.setRollbackOnly();
                return null;
            })
        );
        Thread thread2 = Thread.startVirtualThread(() ->
            tx.execute(status -> {
                sleep(1_000);
                printUsers(em);
                return null;
            })
        );
        join(thread1, thread2);
    }

    /** Неповторяющееся чтение (default = READ COMMITTED): второе чтение видит чужой commit.
     *  В JPA перед повторным чтением чистим persistence context, иначе вернётся кэш. */
    static void repeatableRead(EntityManager em, TransactionTemplate tx, PlatformTransactionManager txm) {
        insertUser(em, tx, new User("Luci", "Test"));

        Thread thread1 = Thread.startVirtualThread(() -> {
            sleep(1_000);
            tx.execute(status -> {
                System.out.println("Start thread 1");
                em.createQuery("UPDATE User u SET u.about = concat(u.about, ' Test') WHERE u.name = 'Luci'")
                    .executeUpdate();
                return null;
            });
            System.out.println("Finish thread 1");
        });
        Thread thread2 = Thread.startVirtualThread(() ->
            tx.execute(status -> {
                System.out.println("Start thread 2");
                printUsers(em);
                sleep(3_000);
                em.clear();          // сброс L1-кэша, иначе повторное чтение вернёт ту же сущность
                printUsers(em);
                return null;
            })
        );
        join(thread1, thread2);
    }

    /** Повторяющееся чтение (REPEATABLE READ): второе чтение видит тот же снимок данных. */
    static void notRepeatableRead(EntityManager em, TransactionTemplate base, PlatformTransactionManager txm) {
        TransactionTemplate tx = withIsolation(txm, TransactionDefinition.ISOLATION_REPEATABLE_READ);
        insertUser(em, base, new User("Luci", "Test"));

        Thread thread1 = Thread.startVirtualThread(() -> {
            sleep(1_000);
            tx.execute(status -> {
                System.out.println("Start thread 1");
                em.createQuery("UPDATE User u SET u.about = concat(u.about, ' Test') WHERE u.name = 'Luci'")
                    .executeUpdate();
                return null;
            });
            System.out.println("Finish thread 1");
        });
        Thread thread2 = Thread.startVirtualThread(() ->
            tx.execute(status -> {
                System.out.println("Start thread 2");
                printUsers(em);
                sleep(3_000);
                em.clear();          // даже после сброса кэша снимок REPEATABLE READ вернёт прежние данные
                printUsers(em);
                return null;
            })
        );
        join(thread1, thread2);
    }

    /** Фантомное чтение (default): между двумя агрегатами появляется новая строка. */
    static void phantomRead(EntityManager em, TransactionTemplate tx, PlatformTransactionManager txm) {
        insertUser(em, tx, new User("Luci", "Test"));

        Thread thread1 = Thread.startVirtualThread(() -> {
            sleep(1_000);
            tx.execute(status -> {
                System.out.println("Start thread 1");
                em.persist(new User("Mark", "Test"));
                return null;
            });
            System.out.println("Finish thread 1");
        });
        Thread thread2 = Thread.startVirtualThread(() ->
            tx.execute(status -> {
                System.out.println("Start thread 2");
                concatUsersName(em);
                sleep(3_000);
                concatUsersName(em);
                return null;
            })
        );
        join(thread1, thread2);
    }

    /** Нет фантомов (REPEATABLE READ в PostgreSQL = снимок, фантомы не появляются). */
    static void notPhantomRead(EntityManager em, TransactionTemplate base, PlatformTransactionManager txm) {
        TransactionTemplate tx = withIsolation(txm, TransactionDefinition.ISOLATION_REPEATABLE_READ);
        insertUser(em, base, new User("Luci", "Test"));

        Thread thread1 = Thread.startVirtualThread(() -> {
            sleep(1_000);
            tx.execute(status -> {
                System.out.println("Start thread 1");
                em.persist(new User("Mark", "Test"));
                return null;
            });
            System.out.println("Finish thread 1");
        });
        Thread thread2 = Thread.startVirtualThread(() ->
            tx.execute(status -> {
                System.out.println("Start thread 2");
                concatUsersName(em);
                sleep(3_000);
                concatUsersName(em);
                return null;
            })
        );
        join(thread1, thread2);
    }

    /** Аномалия записи (write skew) при REPEATABLE READ: обе транзакции читают свой класс
     *  и пишут в чужой — обе коммитятся, инвариант нарушается. */
    static void anomalyExample(EntityManager em, TransactionTemplate base, PlatformTransactionManager txm) {
        seedCalculator(em, base);
        TransactionTemplate tx = withIsolation(txm, TransactionDefinition.ISOLATION_REPEATABLE_READ);

        Thread thread1 = Thread.startVirtualThread(() -> {
            try {
                tx.execute(status -> {
                    System.out.println("Start thread 1");
                    sumOfClass(em, 1);
                    sleep(2_000);
                    em.persist(new Calculator(2, 30));
                    return null;
                });
            } catch (Exception e) {
                System.out.println("thread 1 failed: " + e.getClass().getSimpleName());
            }
            System.out.println("Finish thread 1");
        });
        Thread thread2 = Thread.startVirtualThread(() -> {
            try {
                tx.execute(status -> {
                    System.out.println("Start thread 2");
                    sumOfClass(em, 2);
                    sleep(2_000);
                    em.persist(new Calculator(1, 300));
                    return null;
                });
            } catch (Exception e) {
                System.out.println("thread 2 failed: " + e.getClass().getSimpleName());
            }
            System.out.println("Finish thread 2");
        });
        join(thread1, thread2);
        printCalculator(em);
        clearCalculator(em, base);
    }

    /** Нет аномалии при SERIALIZABLE: PostgreSQL обнаружит конфликт сериализации и откатит одну транзакцию. */
    static void notAnomalyExample(EntityManager em, TransactionTemplate base, PlatformTransactionManager txm) {
        seedCalculator(em, base);
        TransactionTemplate tx = withIsolation(txm, TransactionDefinition.ISOLATION_SERIALIZABLE);

        Thread thread1 = Thread.startVirtualThread(() -> {
            sleep(1_000);
            try {
                tx.execute(status -> {
                    System.out.println("Start thread 1");
                    sumOfClass(em, 1);
                    sleep(2_000);
                    em.persist(new Calculator(2, 30));
                    return null;
                });
            } catch (Exception e) {
                System.out.println("thread 1 failed: " + e.getClass().getSimpleName());
            }
            System.out.println("Finish thread 1");
        });
        Thread thread2 = Thread.startVirtualThread(() -> {
            sleep(1_000);
            try {
                tx.execute(status -> {
                    System.out.println("Start thread 2");
                    sumOfClass(em, 2);
                    sleep(2_000);
                    em.persist(new Calculator(1, 300));
                    return null;
                });
            } catch (Exception e) {
                System.out.println("thread 2 failed: " + e.getClass().getSimpleName());
            }
            System.out.println("Finish thread 2");
        });
        join(thread1, thread2);
        printCalculator(em);
        clearCalculator(em, base);
    }

    // ===================== распространение транзакций (propagation) =====================

    /** REQUIRED: вложенная транзакция присоединяется к внешней — оба insert-а становятся
     *  видимыми одновременно, в момент commit-а внешней транзакции. */
    static void propagationRequired(EntityManager em, TransactionTemplate tx) {
        Thread threadInsert = Thread.startVirtualThread(() ->
            tx.execute(status -> {
                em.persist(new User("Luci", "Test"));
                sleep(2_000);
                tx.execute(inner -> {        // PROPAGATION_REQUIRED (по умолчанию) -> та же транзакция
                    em.persist(new User("Mark", "Test"));
                    return null;
                });
                sleep(2_000);
                return null;
            })
        );
        Thread.startVirtualThread(() -> {
            while (threadInsert.isAlive()) {
                countUsers(em);
                sleep(100);
            }
        });
        join(threadInsert);
    }

    /** SUPPORTS: присоединяется к существующей транзакции, а без неё выполняется вне транзакции.
     *  ВАЖНОЕ отличие от JDBC: в JdbcTemplate каждый update вне транзакции авто-коммитился и был
     *  виден сразу; в JPA запись без транзакции невозможна. Поэтому здесь SUPPORTS вложен во
     *  внешнюю транзакцию и присоединяется к ней (ведёт себя как REQUIRED). */
    static void propagationSupport(EntityManager em, TransactionTemplate base, PlatformTransactionManager txm) {
        TransactionTemplate supports = new TransactionTemplate(txm);
        supports.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

        Thread threadInsert = Thread.startVirtualThread(() ->
            base.execute(status -> {                 // внешняя транзакция (REQUIRED)
                supports.execute(inner -> {          // SUPPORTS -> присоединяется к внешней
                    em.persist(new User("Luci", "Test"));
                    sleep(2_000);
                    em.persist(new User("Mark", "Test"));
                    sleep(2_000);
                    return null;
                });
                return null;
            })
        );
        Thread.startVirtualThread(() -> {
            while (threadInsert.isAlive()) {
                countUsers(em);
                sleep(100);
            }
        });
        join(threadInsert);
    }

    /** REQUIRES_NEW: внутренняя транзакция приостанавливает внешнюю и коммитится сама —
     *  "Mark" становится виден раньше, чем закоммитится внешняя транзакция с "Luci". */
    static void propagationRequiredNew(EntityManager em, PlatformTransactionManager txm) {
        TransactionTemplate tx = new TransactionTemplate(txm);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        Thread threadInsert = Thread.startVirtualThread(() ->
            tx.execute(status -> {
                em.persist(new User("Luci", "Test"));
                sleep(2_000);
                tx.execute(inner -> {            // REQUIRES_NEW -> новая транзакция, отдельный commit
                    em.persist(new User("Mark", "Test"));
                    return null;
                });
                sleep(2_000);
                return null;
            })
        );
        Thread.startVirtualThread(() -> {
            while (threadInsert.isAlive()) {
                countUsers(em);
                sleep(100);
            }
        });
        join(threadInsert);
    }

    // ===================== вспомогательное =====================

    static void seedCalculator(EntityManager em, TransactionTemplate tx) {
        tx.execute(status -> {
            em.createQuery("DELETE FROM Calculator").executeUpdate();
            em.persist(new Calculator(1, 10));
            em.persist(new Calculator(1, 20));
            em.persist(new Calculator(2, 100));
            em.persist(new Calculator(2, 200));
            return null;
        });
    }

    static void sumOfClass(EntityManager em, int clazz) {
        Long sum = em.createQuery(
                "SELECT COALESCE(SUM(c.amount), 0) FROM Calculator c WHERE c.clazz = :c", Long.class)
            .setParameter("c", clazz)
            .getSingleResult();
        System.out.println("sum(class=" + clazz + ") = " + sum);
    }

    static void printCalculator(EntityManager em) {
        em.createQuery("SELECT c FROM Calculator c ORDER BY c.id", Calculator.class)
            .getResultList()
            .forEach(c -> System.out.println(c.getClazz() + " " + c.getAmount()));
    }

    static void clearCalculator(EntityManager em, TransactionTemplate tx) {
        tx.execute(status -> em.createQuery("DELETE FROM Calculator").executeUpdate());
    }

    static TransactionTemplate withIsolation(PlatformTransactionManager txm, int isolation) {
        TransactionTemplate t = new TransactionTemplate(txm);
        t.setIsolationLevel(isolation);
        return t;
    }

    static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static void join(Thread... threads) {
        try {
            for (Thread t : threads) t.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
