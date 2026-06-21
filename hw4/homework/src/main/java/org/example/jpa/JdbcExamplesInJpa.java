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

@SpringBootApplication
public class JdbcExamplesInJpa {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(JdbcExamplesInJpa.class, args);

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
                default -> {  }
            }
        }

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

        insertUser(em, tx, users.get(0));

        printUsers(em);
        clearUsers(em, tx);

        context.close();
    }

    static void clearUsers(EntityManager em, TransactionTemplate tx) {
        tx.execute(status -> em.createQuery("DELETE FROM User").executeUpdate());
    }

    static void printUsers(EntityManager em) {
        em.createQuery("SELECT u FROM User u ORDER BY u.id", User.class)
            .getResultList()
            .forEach(System.out::println);
    }

    static void countUsers(EntityManager em) {
        Long count = em.createQuery("SELECT COUNT(u) FROM User u", Long.class).getSingleResult();
        System.out.println(count);
    }

    static void concatUsersName(EntityManager em) {
        Object result = em.createNativeQuery("SELECT string_agg(name, ', ') FROM users").getSingleResult();
        System.out.println(result);
    }

    static void insertUser(EntityManager em, TransactionTemplate tx, User user) {
        tx.execute(status -> {
            em.persist(user);
            return null;
        });
    }

    static void insertMultipleUsers(EntityManager em, TransactionTemplate tx, List<User> users) {
        tx.execute(status -> {
            users.forEach(em::persist);
            return null;
        });
    }

    static void insertWithTransaction(EntityManager em, TransactionTemplate tx, List<User> users) {
        tx.execute(status -> {
            users.forEach(user -> {
                em.persist(user);
                printUsers(em);
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
                status.setRollbackOnly();
            });
            return null;
        });
    }

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
                em.clear();
                printUsers(em);
                return null;
            })
        );
        join(thread1, thread2);
    }

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
                em.clear();
                printUsers(em);
                return null;
            })
        );
        join(thread1, thread2);
    }

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

    static void propagationRequired(EntityManager em, TransactionTemplate tx) {
        Thread threadInsert = Thread.startVirtualThread(() ->
            tx.execute(status -> {
                em.persist(new User("Luci", "Test"));
                sleep(2_000);
                tx.execute(inner -> {
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

    static void propagationSupport(EntityManager em, TransactionTemplate base, PlatformTransactionManager txm) {
        TransactionTemplate supports = new TransactionTemplate(txm);
        supports.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

        Thread threadInsert = Thread.startVirtualThread(() ->
            base.execute(status -> {
                supports.execute(inner -> {
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

    static void propagationRequiredNew(EntityManager em, PlatformTransactionManager txm) {
        TransactionTemplate tx = new TransactionTemplate(txm);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        Thread threadInsert = Thread.startVirtualThread(() ->
            tx.execute(status -> {
                em.persist(new User("Luci", "Test"));
                sleep(2_000);
                tx.execute(inner -> {
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
