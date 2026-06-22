package org.example;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Модульные тесты {@link Calculator} на JUnit 5.
 */
class CalculatorTest {

    @Test
    @DisplayName("Сложение")
    void add() {
        assertEquals(5, Calculator.add(2, 3));
        assertEquals(-1, Calculator.add(2, -3));
    }

    @Test
    @DisplayName("Вычитание")
    void subtract() {
        assertEquals(-1, Calculator.subtract(2, 3));
    }

    @Test
    @DisplayName("Умножение")
    void multiply() {
        assertEquals(6, Calculator.multiply(2, 3));
        assertEquals(0, Calculator.multiply(0, 99));
    }

    @Test
    @DisplayName("Деление")
    void divide() {
        assertEquals(2.5, Calculator.divide(5, 2));
    }

    @Test
    @DisplayName("Деление на ноль бросает ArithmeticException")
    void divideByZero() {
        assertThrows(ArithmeticException.class, () -> Calculator.divide(1, 0));
    }

    @ParameterizedTest(name = "{0}! = {1}")
    @CsvSource({"0, 1", "1, 1", "5, 120", "10, 3628800"})
    @DisplayName("Факториал")
    void factorial(int n, long expected) {
        assertEquals(expected, Calculator.factorial(n));
    }

    @Test
    @DisplayName("Факториал отрицательного числа бросает IllegalArgumentException")
    void factorialNegative() {
        assertThrows(IllegalArgumentException.class, () -> Calculator.factorial(-1));
    }

    @Test
    @DisplayName("Main.evaluate разбирает выражения")
    void evaluateExpressions() {
        assertEquals("5 + 3 = 8", Main.evaluate(new String[]{"5", "+", "3"}));
        assertEquals("10 / 4 = 2.5", Main.evaluate(new String[]{"10", "/", "4"}));
        assertEquals("5! = 120", Main.evaluate(new String[]{"factorial", "5"}));
    }

    @Test
    @DisplayName("Main.evaluate отвергает неизвестную операцию")
    void evaluateBadOperator() {
        assertThrows(IllegalArgumentException.class,
            () -> Main.evaluate(new String[]{"5", "%", "3"}));
    }
}
