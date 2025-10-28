package co.cellano.edufeed.backend.dto.response;

import java.time.LocalDate;

/**
 * DTO para reporte de asistencias diarias.
 * Agrupa accesos exitosos por día.
 * 
 * @since FASE 3.3
 */
public class AsistenciasDiariasItem {
    private LocalDate dia;
    private Long totalAccesos;
    private Long usuariosUnicos;

    public LocalDate getDia() {
        return dia;
    }

    public void setDia(LocalDate dia) {
        this.dia = dia;
    }

    public Long getTotalAccesos() {
        return totalAccesos;
    }

    public void setTotalAccesos(Long totalAccesos) {
        this.totalAccesos = totalAccesos;
    }

    public Long getUsuariosUnicos() {
        return usuariosUnicos;
    }

    public void setUsuariosUnicos(Long usuariosUnicos) {
        this.usuariosUnicos = usuariosUnicos;
    }
}
