package org.example;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.math.BigDecimal;

public final class AcidDemo {

    private AcidDemo() {
    }

    public static void run(EntityManagerFactory emf) {
        System.out.println("=== 2. ACID: перевод денег в одной транзакции ===");

        long alice = createAccount(emf, "Alice", new BigDecimal("1000"));
        long bob = createAccount(emf, "Bob", new BigDecimal("500"));
        System.out.printf("Старт:   Alice=%s, Bob=%s%n",
            money(balance(emf, alice)), money(balance(emf, bob)));

        transfer(emf, alice, bob, new BigDecimal("300"));
        System.out.println("Перевод 300 (Alice → Bob): успех.");
        System.out.printf("         Alice=%s, Bob=%s%n",
            money(balance(emf, alice)), money(balance(emf, bob)));

        try {
            transfer(emf, alice, bob, new BigDecimal("100000"));
        } catch (IllegalStateException e) {
            System.out.println("Перевод 100000 (Alice → Bob): " + e.getMessage() + " → ROLLBACK.");
        }
        System.out.printf("         Alice=%s, Bob=%s  (баланс не изменился — атомарность)%n",
            money(balance(emf, alice)), money(balance(emf, bob)));

        BigDecimal total = balance(emf, alice).add(balance(emf, bob));
        System.out.println("Сумма балансов = " + money(total) + " (сохранена — согласованность)");

        System.out.println();
    }

    private static void transfer(EntityManagerFactory emf, long fromId, long toId, BigDecimal amount) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            Account from = em.find(Account.class, fromId);
            Account to = em.find(Account.class, toId);

            if (from.getBalance().compareTo(amount) < 0) {
                throw new IllegalStateException("недостаточно средств");
            }

            from.setBalance(from.getBalance().subtract(amount));
            to.setBalance(to.getBalance().add(amount));

            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    private static long createAccount(EntityManagerFactory emf, String name, BigDecimal balance) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            Account account = new Account(name, balance);
            em.persist(account);
            em.getTransaction().commit();
            return account.getId();
        } finally {
            em.close();
        }
    }

    private static BigDecimal balance(EntityManagerFactory emf, long id) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.find(Account.class, id).getBalance();
        } finally {
            em.close();
        }
    }

    private static String money(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
