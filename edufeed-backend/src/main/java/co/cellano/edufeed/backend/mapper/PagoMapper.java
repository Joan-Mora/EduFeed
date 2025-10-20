package co.cellano.edufeed.backend.mapper;

import co.cellano.edufeed.backend.dto.PagoDto;
import co.cellano.edufeed.backend.model.Pago;
import co.cellano.edufeed.backend.model.Usuario;

import java.util.UUID;

/**
 * Mapper para conversión entre Pago y PagoDto.
 * 
 * @since FASE 1, extendido en FASE 2.2
 */
public final class PagoMapper {
    private PagoMapper() {
    }

    public static PagoDto toDto(Pago p) {
        if (p == null)
            return null;
        PagoDto dto = new PagoDto();
        dto.setId(p.getId() != null ? p.getId().toString() : null);
        dto.setUsuarioId(
                p.getUsuario() != null && p.getUsuario().getId() != null ? p.getUsuario().getId().toString() : null);
        dto.setMonto(p.getMonto());
        dto.setTipoPago(p.getTipoPago());
        dto.setEstadoPago(p.getEstadoPago());
        dto.setCreadoEn(p.getCreadoEn());
        dto.setVigenteDesde(p.getVigenteDesde());
        dto.setVigenteHasta(p.getVigenteHasta());
        dto.setMetodoPago(p.getMetodoPago());
        dto.setReferenciaExterna(p.getReferenciaExterna());
        dto.setCajero(p.getCajero());
        dto.setMetadatos(p.getMetadatos());
        // Note: diasPaquete no se mapea desde entity (se gestiona en PaquetePago)
        return dto;
    }

    public static Pago toEntity(PagoDto dto, Usuario usuario) {
        if (dto == null)
            return null;
        Pago p = new Pago();
        if (dto.getId() != null && !dto.getId().isBlank()) {
            p.setId(UUID.fromString(dto.getId()));
        }
        p.setUsuario(usuario);
        p.setMonto(dto.getMonto());
        p.setTipoPago(dto.getTipoPago());
        p.setEstadoPago(dto.getEstadoPago());
        p.setMetodoPago(dto.getMetodoPago());
        p.setReferenciaExterna(dto.getReferenciaExterna());
        p.setCajero(dto.getCajero());
        p.setMetadatos(dto.getMetadatos());
        // Note: vigencias se calculan automáticamente en PagoService
        return p;
    }
}
