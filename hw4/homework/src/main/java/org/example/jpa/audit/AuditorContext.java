package org.example.jpa.audit;

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
