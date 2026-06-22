package org.example;

/**
 * Простейший калькулятор с базовыми арифметическими операциями.
 *
 * <p>Класс демонстрирует оформление документации через <b>JavaDoc</b>:
 * теги {@code @param}, {@code @return}, {@code @throws}, {@code @see},
 * {@code @deprecated}, а также блоки кода {@code {@code ...}} и
 * примеры в {@code <pre>}.
 *
 * <p>Пример использования:
 * <pre>
 *     int sum = Calculator.add(2, 3);        // 5
 *     long f  = Calculator.factorial(5);     // 120
 * </pre>
 *
 * Все методы статические — объект создавать не нужно.
 *
 * @author Кирилл Еркаев
 * @version 1.0
 */
public final class Calculator {

    /** Утилитарный класс — экземпляры не создаются. */
    private Calculator() {
    }

    /**
     * Складывает два целых числа.
     *
     * @param a первое слагаемое
     * @param b второе слагаемое
     * @return сумма {@code a + b}
     */
    public static int add(int a, int b) {
        return a + b;
    }

    /**
     * Вычитает одно число из другого.
     *
     * @param a уменьшаемое
     * @param b вычитаемое
     * @return разность {@code a - b}
     */
    public static int subtract(int a, int b) {
        return a - b;
    }

    /**
     * Умножает два целых числа.
     *
     * @param a первый множитель
     * @param b второй множитель
     * @return произведение {@code a * b}
     */
    public static int multiply(int a, int b) {
        return a * b;
    }

    /**
     * Делит одно число на другое.
     *
     * @param a делимое
     * @param b делитель
     * @return частное {@code a / b} как число с плавающей точкой
     * @throws ArithmeticException если {@code b == 0}
     */
    public static double divide(int a, int b) {
        if (b == 0) {
            throw new ArithmeticException("Деление на ноль запрещено.");
        }
        return (double) a / b;
    }

    /**
     * Вычисляет факториал числа.
     *
     * <p>Пример: {@code factorial(5)} вернёт {@code 120}.
     *
     * @param n неотрицательное число
     * @return {@code n!}
     * @throws IllegalArgumentException если {@code n < 0}
     * @see #multiply(int, int)
     */
    public static long factorial(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Факториал отрицательного числа не определён.");
        }
        long result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    /**
     * Старый вариант сложения, оставлен для совместимости.
     *
     * @param a первое слагаемое
     * @param b второе слагаемое
     * @return сумма {@code a + b}
     * @deprecated используйте {@link #add(int, int)}.
     */
    @Deprecated
    public static int addOld(int a, int b) {
        return add(a, b);
    }
}
