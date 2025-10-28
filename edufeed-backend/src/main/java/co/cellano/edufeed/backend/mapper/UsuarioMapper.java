package co.cellano.edufeed.backend.mapper;

import co.cellano.edufeed.backend.dto.UsuarioDto;
import co.cellano.edufeed.backend.model.Usuario;
import java.util.UUID;

public final class UsuarioMapper {
    private UsuarioMapper() {
    }

    public static UsuarioDto toDto(Usuario u) {
        if (u == null)
            return null;
        UsuarioDto dto = new UsuarioDto();
        dto.setId(u.getId() != null ? u.getId().toString() : null);
        dto.setDocumento(u.getDocumento());
        dto.setNombreCompleto(u.getNombreCompleto());
        dto.setTipoUsuario(u.getTipoUsuario());
        dto.setEmail(u.getEmail());
        dto.setTelefono(u.getTelefono());
        dto.setActivo(u.isActivo());
        return dto;
    }

    public static Usuario toEntity(UsuarioDto dto) {
        if (dto == null)
            return null;
        Usuario u = new Usuario();
        if (dto.getId() != null && !dto.getId().isBlank()) {
            u.setId(UUID.fromString(dto.getId()));
        }
        u.setDocumento(dto.getDocumento());
        u.setNombreCompleto(dto.getNombreCompleto());
        u.setTipoUsuario(dto.getTipoUsuario());
        u.setEmail(dto.getEmail());
        u.setTelefono(dto.getTelefono());
        if (dto.getActivo() != null)
            u.setActivo(dto.getActivo());
        return u;
    }
}
