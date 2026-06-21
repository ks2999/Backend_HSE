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

    @GetMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.SET_COOKIE, buildCookie("", 0))
            .location(URI.create("/login.html?logout"))
            .build();
    }

    private String authenticateAndIssueToken(String username, String password) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(username, password));
        return jwtService.generateToken(authentication.getName(), authentication.getAuthorities());
    }

    private String buildCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(cookieName, value)
            .httpOnly(true)
            .path("/")
            .sameSite("Lax")
            .maxAge(maxAgeSeconds)
            .build()
            .toString();
    }

    public record LoginRequest(String username, String password) {
    }
}
