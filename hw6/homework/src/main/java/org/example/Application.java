package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Своя форма авторизации, которая вместо JSESSIONID выдаёт JWT.
 *
 * <p>Идея: пользователь вводит логин/пароль в обычной HTML-форме; сервер проверяет
 * их и в ответ кладёт подписанный JWT в cookie (вместо серверной сессии и JSESSIONID).
 * Приложение работает в режиме STATELESS — каждый запрос аутентифицируется заново
 * по токену (из cookie в браузере или из заголовка {@code Authorization: Bearer} для API).
 */
@SpringBootApplication
@EnableMethodSecurity   // включает @PreAuthorize на эндпоинтах
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
