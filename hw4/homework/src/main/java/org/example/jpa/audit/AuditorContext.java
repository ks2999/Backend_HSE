package org.example.jpa.audit;

/**
 * Простейший «источник текущего пользователя» для аудита.
 *
 * <p>В реальном приложении его роль играет Spring Security
 * ({@code SecurityContextHolder.getContext().getAuthentication().getName()}).
 * Здесь — статическое поле, чтобы в демо можно было переключать «пользователя».
 */
public final class AuditorContext {

    private static volatile String currentUser = "system";

    private AuditorContext() {
    }

    public static void set(String user) {
        currentUser = user;
    }

    public static String get() {
        return currentUser;
    }
}
