package co.cellano.edufeed.backend.exception;

import java.util.UUID;

/**
 * Excepción lanzada cuando un usuario no tiene un derecho de uso vigente
 * para acceder al sistema.
 * 
 * Esta excepción se usa en el proceso de verificación de acceso (RF-03)
 * y desencadena la orientación a caja (RF-04).
 */
public class NoDerechoVigenteException extends RuntimeException {

    private final UUID usuarioId;
    private final String documento;
    private final String motivoDenegacion;

    /**
     * Constructor con información completa del usuario
     * 
     * @param usuarioId        ID del usuario sin derecho
     * @param documento        Documento del usuario
     * @param motivoDenegacion Código del motivo de denegación
     */
    public NoDerechoVigenteException(UUID usuarioId, String documento, String motivoDenegacion) {
        super(String.format("Usuario con documento %s no tiene derecho vigente. Motivo: %s",
                documento, motivoDenegacion));
        this.usuarioId = usuarioId;
        this.documento = documento;
        this.motivoDenegacion = motivoDenegacion;
    }

    /**
     * Constructor simplificado con solo el documento
     * 
     * @param documento Documento del usuario
     */
    public NoDerechoVigenteException(String documento) {
        this(null, documento, "SIN_DERECHO_VIGENTE");
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getDocumento() {
        return documento;
    }

    public String getMotivoDenegacion() {
        return motivoDenegacion;
    }
}
