package co.cellano.edufeed.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class AuditoriaDto {
    public UUID id;
    public String tipoEntidad;
    public UUID entidadId;
    public String accion;
    public String realizadoPor;
    public OffsetDateTime realizadoEn;
    public String valoresAnteriores;
    public String valoresNuevos;
}
