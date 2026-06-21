package org.example;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public final class RelationshipsDemo {

    private RelationshipsDemo() {
    }

    public static void run(EntityManagerFactory emf) {
        System.out.println("=== 1. Hibernate ORM: сущности и связи ===");

        seed(emf);
        readGraph(emf);
        queries(emf);

        System.out.println();
    }

    private static void seed(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();

            Category electronics = new Category("Электроника");
            Category books = new Category("Книги");

            Product phone = new Product("Смартфон", new BigDecimal("30000"));
            Product novel = new Product("Роман", new BigDecimal("500"));
            electronics.addProduct(phone);
            books.addProduct(novel);

            Customer customer = new Customer("Алексей");
            Order order = new Order(LocalDate.now());
            order.addProduct(phone);
            order.addProduct(novel);
            customer.addOrder(order);

            em.persist(electronics);
            em.persist(books);
            em.persist(customer);

            em.getTransaction().commit();
            System.out.println("Сохранён граф: 2 категории, 2 товара, покупатель и его заказ.");
        } finally {
            em.close();
        }
    }

    private static void readGraph(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        try {
            List<Customer> customers = em.createQuery(
                    "SELECT DISTINCT c FROM Customer c JOIN FETCH c.orders", Customer.class)
                .getResultList();

            for (Customer c : customers) {
                System.out.println("Покупатель: " + c.getName());
                for (Order o : c.getOrders()) {
                    System.out.println("  Заказ от " + o.getOrderDate() + ":");
                    o.getProducts().stream()
                        .sorted(Comparator.comparing(Product::getName))
                        .forEach(p -> System.out.printf("    - %s (%s) [%s]%n",
                            p.getName(),
                            p.getPrice().stripTrailingZeros().toPlainString(),
                            p.getCategory().getName()));
                }
            }
        } finally {
            em.close();
        }
    }

    private static void queries(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        try {
            Double avg = em.createQuery(
                    "SELECT AVG(p.price) FROM Product p", Double.class)
                .getSingleResult();
            BigDecimal avgRounded = BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
            System.out.println("JPQL — средняя цена товара: " + avgRounded.toPlainString());

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Product> cq = cb.createQuery(Product.class);
            Root<Product> root = cq.from(Product.class);
            cq.select(root).where(cb.equal(root.get("name"), "Смартфон"));

            Product found = em.createQuery(cq).getSingleResult();
            System.out.printf("Criteria — найден товар: %s, цена %s%n",
                found.getName(), found.getPrice().stripTrailingZeros().toPlainString());
        } finally {
            em.close();
        }
    }
}
