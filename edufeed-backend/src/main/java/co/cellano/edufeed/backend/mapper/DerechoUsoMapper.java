package co.cellano.edufeed.backend.mapper;

import co.cellano.edufeed.backend.dto.DerechoUsoDto;
import co.cellano.edufeed.backend.model.DerechoUso;
import co.cellano.edufeed.backend.model.Pago;
import co.cellano.edufeed.backend.model.Usuario;
import java.util.UUID;

public final class DerechoUsoMapper {
    private DerechoUsoMapper() {
    }

    public static DerechoUsoDto toDto(DerechoUso d) {
        if (d == null)
            return null;
        DerechoUsoDto dto = new DerechoUsoDto();
        dto.setId(d.getId() != null ? d.getId().toString() : null);
        dto.setUsuarioId(
                d.getUsuario() != null && d.getUsuario().getId() != null ? d.getUsuario().getId().toString() : null);
        dto.setTipoDerecho(d.getTipoDerecho());
        dto.setPagoOrigenId(
                d.getPagoOrigen() != null && d.getPagoOrigen().getId() != null ? d.getPagoOrigen().getId().toString()
                        : null);
        dto.setVigenteDesde(d.getVigenteDesde());
        dto.setVigenteHasta(d.getVigenteHasta());
        dto.setActivo(d.isActivo());
        return dto;
    }

    public static DerechoUso toEntity(DerechoUsoDto dto, Usuario usuario, Pago origen) {
        if (dto == null)
            return null;
        DerechoUso d = new DerechoUso();
        if (dto.getId() != null && !dto.getId().isBlank()) {
            d.setId(UUID.fromString(dto.getId()));
        }
        d.setUsuario(usuario);
        d.setTipoDerecho(dto.getTipoDerecho());
        d.setPagoOrigen(origen);
        d.setVigenteDesde(dto.getVigenteDesde());
        d.setVigenteHasta(dto.getVigenteHasta());
        if (dto.getActivo() != null)
            d.setActivo(dto.getActivo());
        return d;
    }
}
