package org.example.jpa.nplus1;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.function.Supplier;

public final class NPlusOneDemo {

    private NPlusOneDemo() {
    }

    public static void run(ConfigurableApplicationContext ctx) {
        AuthorRepository authors = ctx.getBean(AuthorRepository.class);
        TransactionTemplate tx = ctx.getBean(TransactionTemplate.class);
        Statistics stats = ctx.getBean(EntityManagerFactory.class)
            .unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);

        seed(authors, tx);

        System.out.println("=== Проблема N+1 (3 автора по 3 книги) ===");

        measure("N+1 — ленивая загрузка в цикле", stats, () ->
            tx.execute(s -> countBooks(authors.findAll())));

        measure("Решение №1 — JOIN FETCH", stats, () ->
            tx.execute(s -> countBooks(authors.findAllWithBooksFetch())));

        measure("Решение №2 — @EntityGraph", stats, () ->
            tx.execute(s -> countBooks(authors.findAllWithBooksGraph())));

        System.out.println(
            "Видно: при N+1 запросов = 1 + N (по одному на каждого автора), "
                + "а с JOIN FETCH/@EntityGraph — один запрос.");
    }

    private static int countBooks(List<Author> authors) {
        int total = 0;
        for (Author a : authors) {
            total += a.getBooks().size();
        }
        System.out.println("  авторов=" + authors.size() + ", книг суммарно=" + total);
        return total;
    }

    private static void measure(String name, Statistics stats, Supplier<?> action) {
        System.out.println("--- " + name + " ---");
        long before = stats.getPrepareStatementCount();
        action.get();
        long queries = stats.getPrepareStatementCount() - before;
        System.out.println("  SQL-запросов выполнено: " + queries);
    }

    private static void seed(AuthorRepository authors, TransactionTemplate tx) {
        tx.execute(s -> {
            authors.deleteAll();
            for (int i = 1; i <= 3; i++) {
                Author author = new Author("Автор " + i);
                for (int j = 1; j <= 3; j++) {
                    author.addBook(new Book("Книга " + i + "." + j));
                }
                authors.save(author);
            }
            return null;
        });
    }
}
