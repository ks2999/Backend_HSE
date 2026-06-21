package org.example.jpa;

import jakarta.persistence.*;
import lombok.*;

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

    @PrePersist
    void prePersist() { System.out.println("INSERT " + name); }

    @PreUpdate
    void preUpdate() { System.out.println("UPDATE " + name); }

    @PreRemove
    void preRemove() { System.out.println("DELETE " + name); }
}
