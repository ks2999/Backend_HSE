package org.example.jpa;

import jakarta.persistence.*;
import lombok.*;

/**
 * Вспомогательная сущность для примеров аномалий сериализации
 * (anomalyExample / notAnomalyExample). В JDBC-версии это была таблица
 * calculator(class, value), создаваемая руками; здесь её создаёт Hibernate.
 * Поле названо clazz/amount, чтобы не конфликтовать с зарезервированными словами.
 */
@Entity
@Table(name = "calculator")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Calculator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clazz")
    private Integer clazz;

    @Column(name = "amount")
    private Integer amount;

    public Calculator(Integer clazz, Integer amount) {
        this.clazz = clazz;
        this.amount = amount;
    }
}
