package co.cellano.edufeed.backend.mapper;

import co.cellano.edufeed.backend.dto.AuditoriaDto;
import co.cellano.edufeed.backend.model.Auditoria;

public class AuditoriaMapper {
    public static AuditoriaDto toDto(Auditoria a) {
        AuditoriaDto dto = new AuditoriaDto();
        dto.id = a.getId();
        dto.tipoEntidad = a.getTipoEntidad();
        dto.entidadId = a.getEntidadId();
        dto.accion = a.getAccion();
        dto.realizadoPor = a.getRealizadoPor();
        dto.realizadoEn = a.getRealizadoEn();
        dto.valoresAnteriores = a.getValoresAnteriores();
        dto.valoresNuevos = a.getValoresNuevos();
        return dto;
    }
}
