package co.cellano.edufeed.backend.mapper;

import co.cellano.edufeed.backend.dto.AccesoDto;
import co.cellano.edufeed.backend.model.Acceso;
import co.cellano.edufeed.backend.model.DerechoUso;
import co.cellano.edufeed.backend.model.Usuario;
import java.util.UUID;

public final class AccesoMapper {
    private AccesoMapper() {
    }

    public static AccesoDto toDto(Acceso a) {
        if (a == null)
            return null;
        AccesoDto dto = new AccesoDto();
        dto.setId(a.getId() != null ? a.getId().toString() : null);
        dto.setUsuarioId(
                a.getUsuario() != null && a.getUsuario().getId() != null ? a.getUsuario().getId().toString() : null);
        dto.setDerechoId(
                a.getDerecho() != null && a.getDerecho().getId() != null ? a.getDerecho().getId().toString() : null);
        dto.setEstado(a.getEstado());
        dto.setModalidad(a.getModalidad());
        dto.setMotivo(a.getMotivo());
        dto.setFechaHora(a.getFechaHora());
        dto.setMetadatosCoincidenciaJson(a.getMetadatosCoincidencia());
        return dto;
    }

    public static Acceso toEntity(AccesoDto dto, Usuario usuario, DerechoUso derecho) {
        if (dto == null)
            return null;
        Acceso a = new Acceso();
        if (dto.getId() != null && !dto.getId().isBlank()) {
            a.setId(UUID.fromString(dto.getId()));
        }
        a.setUsuario(usuario);
        a.setDerecho(derecho);
        a.setEstado(dto.getEstado());
        a.setModalidad(dto.getModalidad());
        a.setMotivo(dto.getMotivo());
        if (dto.getFechaHora() != null)
            a.setFechaHora(dto.getFechaHora());
        a.setMetadatosCoincidencia(dto.getMetadatosCoincidenciaJson());
        return a;
    }
}
