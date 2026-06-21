package org.example.jpa;

import jakarta.persistence.*;
import lombok.*;

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
