package co.cellano.edufeed.desktop.session;

import java.util.Set;

/**
 * Contexto simple en memoria para datos de sesión del Desktop.
 * Evita pasar el username/roles por todos los controladores.
 */
public final class SessionContext {
    private SessionContext() {
    }

    public static String baseUrl;
    public static String token;
    public static String username;
    public static Set<String> roles;
    public static Runnable onLogout;
}
