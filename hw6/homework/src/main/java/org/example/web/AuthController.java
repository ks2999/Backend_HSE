package org.example.web;

import org.example.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

/**
 * Вход и выход. Вместо создания серверной сессии (JSESSIONID) выдаём JWT.
 *
 * <ul>
 *   <li>{@code POST /login} — форма (browser): кладёт JWT в cookie и редиректит на /home.html;</li>
 *   <li>{@code POST /api/auth/login} — JSON (API): возвращает токен для заголовка Authorization;</li>
 *   <li>{@code GET /logout} — удаляет cookie с токеном (на сервере состояния нет).</li>
 * </ul>
 */
@RestController
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final String cookieName;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          @Value("${app.jwt.cookie-name}") String cookieName) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.cookieName = cookieName;
    }

    /** Обработка HTML-формы: успех -> cookie с JWT + редирект на главную; ошибка -> назад на форму. */
    @PostMapping(value = "/login", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<Void> formLogin(@RequestParam String username, @RequestParam String password) {
        try {
            String token = authenticateAndIssueToken(username, password);
            return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.SET_COOKIE, buildCookie(token, jwtService.getExpirationSeconds()))
                .location(URI.create("/home.html"))
                .build();
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/login.html?error"))
                .build();
        }
    }

    /** API-вход: возвращает токен в теле для использования в заголовке Authorization: Bearer. */
    @PostMapping(value = "/api/auth/login", consumes = "application/json")
    public ResponseEntity<?> apiLogin(@RequestBody LoginRequest request) {
        try {
            String token = authenticateAndIssueToken(request.username(), request.password());
            return ResponseEntity.ok(Map.of(
                "type", "Bearer",
                "token", token,
                "expiresInSeconds", jwtService.getExpirationSeconds()));
        } catch (AuthenticationException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Неверное имя пользователя или пароль"));
        }
    }

    /** Выход без сессии — просто стираем cookie с токеном (maxAge=0). */
    @GetMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.SET_COOKIE, buildCookie("", 0))
            .location(URI.create("/login.html?logout"))
            .build();
    }

    /** Проверяет логин/пароль через AuthenticationManager и выпускает JWT с ролями пользователя. */
    private String authenticateAndIssueToken(String username, String password) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(username, password));
        return jwtService.generateToken(authentication.getName(), authentication.getAuthorities());
    }

    private String buildCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(cookieName, value)
            .httpOnly(true)      // недоступна из JS -> защита от XSS-кражи токена
            .path("/")
            .sameSite("Lax")
            .maxAge(maxAgeSeconds)
            .build()
            .toString();
    }

    public record LoginRequest(String username, String password) {
    }
}
