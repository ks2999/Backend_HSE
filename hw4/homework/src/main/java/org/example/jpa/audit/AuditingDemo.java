package org.example.jpa.audit;

import org.springframework.context.ConfigurableApplicationContext;

public final class AuditingDemo {

    private AuditingDemo() {
    }

    public static void run(ConfigurableApplicationContext ctx) {
        ArticleRepository repo = ctx.getBean(ArticleRepository.class);
        repo.deleteAll();

        System.out.println("=== Spring Data JPA Auditing ===");

        AuditorContext.set("alice");
        Long id = repo.save(new Article("Аудит в Spring Data JPA")).getId();
        print("после создания (пользователь alice)", repo.findById(id).orElseThrow());

        sleep(100);

        AuditorContext.set("bob");
        Article article = repo.findById(id).orElseThrow();
        article.setTitle("Аудит в Spring Data JPA (отредактировано)");
        repo.save(article);
        print("после изменения (пользователь bob)", repo.findById(id).orElseThrow());

        System.out.println(
            "Итог: created* проставлены при вставке и не меняются; "
                + "lastModified* обновляются при каждом изменении.");
    }

    private static void print(String when, Article a) {
        System.out.printf(
            "%s:%n  title=%s%n  createdBy=%s, createdDate=%s%n  lastModifiedBy=%s, lastModifiedDate=%s%n",
            when, a.getTitle(),
            a.getCreatedBy(), a.getCreatedDate(),
            a.getLastModifiedBy(), a.getLastModifiedDate());
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
