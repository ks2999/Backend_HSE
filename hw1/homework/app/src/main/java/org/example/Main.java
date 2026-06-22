package org.example;

/**
 * Консольный калькулятор. Принимает выражение из аргументов командной строки.
 *
 * <p>Поддерживаемые формы:
 * <pre>
 *     &lt;a&gt; &lt;op&gt; &lt;b&gt;     бинарная операция, op = + - * /     (пример: 5 + 3)
 *     factorial &lt;n&gt;        факториал числа                      (пример: factorial 5)
 * </pre>
 */
public class Main {

    /** Точка входа — приложение запускается только через {@link #main(String[])}. */
    private Main() {
    }

    /**
     * Точка входа: вычисляет выражение из аргументов и печатает результат,
     * а при ошибке — сообщение и подсказку по использованию.
     *
     * @param args выражение, например {@code 5 + 3} или {@code factorial 5}
     */
    public static void main(String[] args) {
        try {
            System.out.println(evaluate(args));
        } catch (RuntimeException e) {
            System.out.println("Ошибка: " + e.getMessage());
            System.out.println(usage());
        }
    }

    /** Разбирает аргументы и возвращает строку с результатом. */
    static String evaluate(String[] args) {
        if (args.length == 2 && args[0].equalsIgnoreCase("factorial")) {
            int n = parse(args[1]);
            return n + "! = " + Calculator.factorial(n);
        }

        if (args.length == 3) {
            int a = parse(args[0]);
            int b = parse(args[2]);
            String op = args[1];
            return switch (op) {
                case "+" -> a + " + " + b + " = " + Calculator.add(a, b);
                case "-" -> a + " - " + b + " = " + Calculator.subtract(a, b);
                case "*", "x", "X" -> a + " * " + b + " = " + Calculator.multiply(a, b);
                case "/" -> a + " / " + b + " = " + Calculator.divide(a, b);
                default -> throw new IllegalArgumentException("неизвестная операция '" + op + "'");
            };
        }

        throw new IllegalArgumentException("неверное число аргументов");
    }

    private static int parse(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + s + "' — это не целое число");
        }
    }

    private static String usage() {
        return """
            Использование:
              <a> <op> <b>     где op = + - * /     (например: 5 + 3)
              factorial <n>    факториал числа       (например: factorial 5)""";
    }
}
