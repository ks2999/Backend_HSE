package org.example.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Защищаемые эндпоинты. Аутентификация берётся из JWT (cookie или Bearer),
 * роли проверяются через {@link PreAuthorize}.
 */
@RestController
public class ApiController {

    @GetMapping("/public")
    public String publicEndpoint() {
        return "Публичный метод — доступен всем, без токена.";
    }

    /** Текущий пользователь и его роли — данные взяты из токена. */
    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority).toList();
        return Map.of("username", authentication.getName(), "roles", roles);
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('USER')")
    public String userEndpoint() {
        return "Вы вошли как USER.";
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminEndpoint() {
        return "Вы вошли как ADMIN.";
    }

    @GetMapping("/user-or-admin")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public String userOrAdminEndpoint() {
        return "Доступно для USER или ADMIN.";
    }
}
