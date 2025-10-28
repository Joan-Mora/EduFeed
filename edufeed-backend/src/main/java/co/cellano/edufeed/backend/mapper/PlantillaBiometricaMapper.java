package co.cellano.edufeed.backend.mapper;

import co.cellano.edufeed.backend.dto.PlantillaBiometricaDto;
import co.cellano.edufeed.backend.model.PlantillaBiometrica;

public class PlantillaBiometricaMapper {
    public static PlantillaBiometricaDto toDto(PlantillaBiometrica p) {
        PlantillaBiometricaDto dto = new PlantillaBiometricaDto();
        dto.setId(p.getId() != null ? p.getId().toString() : null);
        dto.setModalidad(p.getModalidad());
        dto.setProveedor(p.getProveedor());
        dto.setCreadoEn(p.getCreadoEn());
        dto.setActivo(p.isActivo());
        return dto;
    }
}
