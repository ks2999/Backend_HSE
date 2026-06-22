package org.example;

import jakarta.persistence.EntityManagerFactory;

/**
 * Точка входа. Запускает три демонстрации по темам семинара 3:
 * <ol>
 *   <li>Hibernate как ORM (сущности и связи) — {@link RelationshipsDemo};</li>
 *   <li>ACID (перевод денег в одной транзакции) — {@link AcidDemo};</li>
 *   <li>уровни изоляции транзакций — {@link IsolationDemo}.</li>
 * </ol>
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("Подключение к БД: " + Db.URL + " (пользователь " + Db.USER + ")");
        System.out.println();

        EntityManagerFactory emf = Db.buildEntityManagerFactory();
        try {
            Db.reset(emf);                 // чистим таблицы, чтобы запуск был воспроизводим
            RelationshipsDemo.run(emf);    // 1. Hibernate ORM
            AcidDemo.run(emf);             // 2. ACID
        } finally {
            emf.close();
        }

        IsolationDemo.run(Db.URL, Db.USER, Db.PASSWORD);  // 3. Уровни изоляции (чистый JDBC)

        System.out.println("Готово.");
    }
}
