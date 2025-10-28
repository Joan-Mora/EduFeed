package co.cellano.edufeed.backend.security;

/**
 * Constantes de roles de la aplicación.
 * Usar los nombres sin prefijo en anotaciones @PreAuthorize (Spring añade ROLE_ automáticamente).
 */
public final class Roles {
    private Roles() {}

    // Con prefijo (para authorities en SecurityContext)
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_OPERADOR_CAJA = "ROLE_OPERADOR_CAJA";
    public static final String ROLE_OPERADOR_ACCESO = "ROLE_OPERADOR_ACCESO";
    public static final String ROLE_AUDITOR = "ROLE_AUDITOR";
    public static final String ROLE_SUPERVISOR = "ROLE_SUPERVISOR";

    // Sin prefijo (para @PreAuthorize hasRole('XYZ'))
    public static final String ADMIN = "ADMIN";
    public static final String OPERADOR_CAJA = "OPERADOR_CAJA";
    public static final String OPERADOR_ACCESO = "OPERADOR_ACCESO";
    public static final String AUDITOR = "AUDITOR";
    public static final String SUPERVISOR = "SUPERVISOR";
}
