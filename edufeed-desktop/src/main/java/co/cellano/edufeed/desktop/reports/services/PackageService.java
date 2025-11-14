package co.cellano.edufeed.desktop.reports.services;

import co.cellano.edufeed.desktop.service.PaymentApiClient;
import co.cellano.edufeed.desktop.service.PaymentApiClient.*;
import co.cellano.edufeed.desktop.reports.models.PaqueteAsignadoDto;
import co.cellano.edufeed.desktop.reports.models.PaqueteServicio;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar paquetes de servicios.
 * Permite listar, filtrar y asignar paquetes a usuarios.
 */
public class PackageService {
    private final PaymentApiClient paymentApiClient;

    public PackageService(PaymentApiClient paymentApiClient) {
        this.paymentApiClient = paymentApiClient;
    }

    /**
     * Asigna un paquete a un usuario, creando un pago PENDIENTE en el sistema.
     * 
     * @param usuarioId         ID del usuario
     * @param paquete           Tipo de paquete a asignar
     * @param metodoPago        Método de pago (EFECTIVO, TARJETA, TRANSFERENCIA,
     *                          POS)
     * @param referenciaExterna Referencia externa opcional
     * @param cajero            Nombre del cajero que asigna el paquete
     * @return DTO del pago creado
     * @throws IOException Si hay error de conexión
     */
    public PagoDto asignarPaquete(String usuarioId, PaqueteServicio paquete, String metodoPago,
            String referenciaExterna, String cajero) throws IOException {
        CreatePagoRequest request = new CreatePagoRequest();
        request.usuarioId = usuarioId;
        request.monto = paquete.getCosto();
        request.tipoPago = TipoPago.PAQUETE;
        request.metodoPago = metodoPago != null ? metodoPago : "EFECTIVO";
        request.referenciaExterna = referenciaExterna != null ? referenciaExterna : paquete.getMotivoPago();
        request.diasPaquete = paquete.getDuracionDias();
        request.cajero = cajero;
        request.metadatos = paquete.getNombre(); // Guardar el nombre del paquete en metadatos

        System.out.println("[PackageService] Asignando paquete: " + paquete.getNombre() +
                " a usuario: " + usuarioId + " con monto: $" + paquete.getCosto());

        return paymentApiClient.crearPago(request);
    }

    /**
     * Lista todos los paquetes asignados (pagos de tipo PAQUETE).
     * 
     * @return Lista de paquetes asignados con información del usuario
     * @throws IOException Si hay error de conexión
     */
    public List<PaqueteAsignadoDto> listarPaquetesAsignados() throws IOException {
        List<PagoEnriquecidoDto> pagos = paymentApiClient.listarPagos();

        // Filtrar solo pagos de tipo PAQUETE y convertir a PaqueteAsignadoDto
        return pagos.stream()
                .filter(p -> p.tipoPago == TipoPago.PAQUETE)
                .map(this::convertirAPaqueteAsignado)
                .collect(Collectors.toList());
    }

    /**
     * Filtra paquetes asignados por múltiples criterios.
     * 
     * @param paquetes         Lista original de paquetes
     * @param fechaDesde       Fecha desde (inclusive)
     * @param fechaHasta       Fecha hasta (inclusive)
     * @param documentoUsuario Documento del usuario (búsqueda parcial)
     * @param nombreUsuario    Nombre del usuario (búsqueda parcial)
     * @param idUsuario        ID del usuario (búsqueda exacta)
     * @param estados          Lista de estados a incluir (APROBADO, PENDIENTE,
     *                         RECHAZADO, REVERTIDO)
     * @return Lista filtrada de paquetes
     */
    public List<PaqueteAsignadoDto> filtrarPaquetes(List<PaqueteAsignadoDto> paquetes,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String documentoUsuario,
            String nombreUsuario,
            UUID idUsuario,
            List<EstadoPago> estados) {
        return paquetes.stream()
                .filter(p -> filtrarPorFecha(p, fechaDesde, fechaHasta))
                .filter(p -> filtrarPorDocumento(p, documentoUsuario))
                .filter(p -> filtrarPorNombre(p, nombreUsuario))
                .filter(p -> filtrarPorId(p, idUsuario))
                .filter(p -> filtrarPorEstado(p, estados))
                .collect(Collectors.toList());
    }

    private boolean filtrarPorFecha(PaqueteAsignadoDto paquete, LocalDate desde, LocalDate hasta) {
        if (desde == null && hasta == null)
            return true;
        if (paquete.getFechaCreacion() == null)
            return false;

        LocalDate fechaPaquete = paquete.getFechaCreacion()
                .atZoneSameInstant(ZoneId.systemDefault())
                .toLocalDate();

        if (desde != null && fechaPaquete.isBefore(desde))
            return false;
        if (hasta != null && fechaPaquete.isAfter(hasta))
            return false;

        return true;
    }

    private boolean filtrarPorDocumento(PaqueteAsignadoDto paquete, String documento) {
        if (documento == null || documento.trim().isEmpty())
            return true;
        if (paquete.getUsuarioDocumento() == null)
            return false;
        return paquete.getUsuarioDocumento().toLowerCase().contains(documento.toLowerCase());
    }

    private boolean filtrarPorNombre(PaqueteAsignadoDto paquete, String nombre) {
        if (nombre == null || nombre.trim().isEmpty())
            return true;
        if (paquete.getUsuarioNombre() == null)
            return false;
        return paquete.getUsuarioNombre().toLowerCase().contains(nombre.toLowerCase());
    }

    private boolean filtrarPorId(PaqueteAsignadoDto paquete, UUID id) {
        if (id == null)
            return true;
        return id.equals(paquete.getUsuarioId());
    }

    private boolean filtrarPorEstado(PaqueteAsignadoDto paquete, List<EstadoPago> estados) {
        if (estados == null || estados.isEmpty())
            return true;
        return estados.contains(paquete.getEstado());
    }

    /**
     * Convierte un PagoEnriquecidoDto a PaqueteAsignadoDto.
     */
    private PaqueteAsignadoDto convertirAPaqueteAsignado(PagoEnriquecidoDto pago) {
        PaqueteAsignadoDto dto = new PaqueteAsignadoDto();

        // Convertir String id a UUID
        try {
            dto.setPagoId(UUID.fromString(pago.id));
        } catch (IllegalArgumentException e) {
            System.err.println("Error al convertir ID de pago: " + pago.id);
        }

        // Convertir String usuarioId a UUID
        try {
            if (pago.usuarioId != null && !pago.usuarioId.isBlank()) {
                dto.setUsuarioId(UUID.fromString(pago.usuarioId));
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Error al convertir ID de usuario: " + pago.usuarioId);
        }

        dto.setUsuarioDocumento(pago.usuarioDocumento);
        dto.setUsuarioNombre(pago.usuarioNombre);
        dto.setMonto(pago.monto);
        dto.setEstado(pago.estadoPago); // Usar estadoPago en lugar de estado
        dto.setFechaCreacion(pago.creadoEn); // Usar creadoEn en lugar de createdAt
        dto.setFechaActualizacion(pago.vigenteHasta); // Usar vigenteHasta como fecha de actualización
        dto.setReferenciaExterna(pago.referenciaExterna);
        dto.setMetodoPago(pago.metodoPago);
        dto.setDiasPaquete(pago.diasPaquete);

        // Inferir el paquete desde metadatos o diasPaquete
        dto.setPaquete(inferirPaquete(pago.metadatos, pago.diasPaquete, pago.monto));

        return dto;
    }

    /**
     * Infiere el tipo de paquete desde los metadatos, días o monto.
     */
    private PaqueteServicio inferirPaquete(String metadatos, Integer diasPaquete, java.math.BigDecimal monto) {
        // Intentar desde metadatos
        if (metadatos != null) {
            if (metadatos.contains("Lite"))
                return PaqueteServicio.LITE;
            if (metadatos.contains("Estándar") || metadatos.contains("Estandar"))
                return PaqueteServicio.ESTANDAR;
            if (metadatos.contains("Premium"))
                return PaqueteServicio.PREMIUM;
        }

        // Intentar desde días
        if (diasPaquete != null) {
            if (diasPaquete == 7)
                return PaqueteServicio.LITE;
            if (diasPaquete == 15)
                return PaqueteServicio.ESTANDAR;
            if (diasPaquete >= 30)
                return PaqueteServicio.PREMIUM;
        }

        // Intentar desde monto (con margen de 1000 pesos)
        if (monto != null) {
            int montoInt = monto.intValue();
            if (Math.abs(montoInt - 55000) < 1000)
                return PaqueteServicio.LITE;
            if (Math.abs(montoInt - 250000) < 1000)
                return PaqueteServicio.ESTANDAR;
            if (Math.abs(montoInt - 600000) < 1000)
                return PaqueteServicio.PREMIUM;
        }

        // Por defecto, retornar LITE
        return PaqueteServicio.LITE;
    }
}
