package org.example;

import jakarta.persistence.EntityManagerFactory;
import org.example.spring.SpringDataJpaDemo;

public class Main {

    public static void main(String[] args) {
        System.out.println("Подключение к БД: " + Db.URL + " (пользователь " + Db.USER + ")");
        System.out.println();

        EntityManagerFactory emf = Db.buildEntityManagerFactory();
        try {
            Db.reset(emf);
            RelationshipsDemo.run(emf);
            AcidDemo.run(emf);
        } finally {
            emf.close();
        }

        IsolationDemo.run(Db.URL, Db.USER, Db.PASSWORD);

        SpringDataJpaDemo.run();

        System.out.println("Готово.");
    }
}
