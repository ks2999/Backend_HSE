package org.example.jpa;

import jakarta.persistence.*;
import lombok.*;

/**
 * Сущность для таблицы users(id, name, about) — та же модель, что и в JDBC-примерах.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "about")
    private String about;

    public User(String name, String about) {
        this.name = name;
        this.about = about;
    }

    // JPA lifecycle-колбэки — аналог "побочных эффектов", наглядно видно момент INSERT/UPDATE/DELETE
    @PrePersist
    void prePersist() { System.out.println("INSERT " + name); }

    @PreUpdate
    void preUpdate() { System.out.println("UPDATE " + name); }

    @PreRemove
    void preRemove() { System.out.println("DELETE " + name); }
}
