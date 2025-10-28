package co.cellano.edufeed.backend.service;

import co.cellano.edufeed.backend.exception.InsufficientPackageException;
import co.cellano.edufeed.backend.exception.ResourceNotFoundException;
import co.cellano.edufeed.backend.model.PaquetePago;
import co.cellano.edufeed.backend.repository.PaquetePagoRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para gestión de paquetes de pago.
 * 
 * <p>
 * Responsabilidades:
 * <ul>
 * <li>Validar disponibilidad de días en paquetes</li>
 * <li>Consumir días de paquetes al generar derechos de uso</li>
 * <li>Consultar días restantes</li>
 * </ul>
 * </p>
 * 
 * @since FASE 2.2
 */
@Service
@Transactional
public class PaquetePagoService {
    private static final Logger log = LoggerFactory.getLogger(PaquetePagoService.class);

    private final PaquetePagoRepository paquetePagoRepository;

    public PaquetePagoService(PaquetePagoRepository paquetePagoRepository) {
        this.paquetePagoRepository = paquetePagoRepository;
    }

    /**
     * Obtiene el paquete asociado a un pago.
     * 
     * @param pagoId ID del pago
     * @return PaquetePago asociado
     * @throws ResourceNotFoundException si no existe paquete para el pago
     */
    @Transactional(readOnly = true)
    public PaquetePago obtenerPorPago(UUID pagoId) {
        return paquetePagoRepository.findByPagoId(pagoId)
                .orElseThrow(() -> new ResourceNotFoundException("PaquetePago para pago", pagoId));
    }

    /**
     * Verifica si un paquete tiene días disponibles.
     * 
     * @param pagoId ID del pago
     * @return true si tiene días disponibles, false si está agotado
     */
    @Transactional(readOnly = true)
    public boolean tieneDiasDisponibles(UUID pagoId) {
        PaquetePago paquete = obtenerPorPago(pagoId);
        return paquete.getDiasRestantes() > 0;
    }

    /**
     * Consume un día del paquete.
     * 
     * @param pagoId ID del pago
     * @throws InsufficientPackageException si el paquete está agotado
     */
    public void consumirDia(UUID pagoId) {
        log.debug("Consumiendo día de paquete para pago {}", pagoId);

        PaquetePago paquete = obtenerPorPago(pagoId);

        if (paquete.getDiasRestantes() <= 0) {
            throw new InsufficientPackageException(
                    "El paquete no tiene días disponibles",
                    paquete.getDiasRestantes());
        }

        paquete.setDiasRestantes(paquete.getDiasRestantes() - 1);
        paquetePagoRepository.save(paquete);

        log.info("Día consumido de paquete: pago={}, días restantes={}",
                pagoId, paquete.getDiasRestantes());
    }

    /**
     * Restaura un día al paquete (por ejemplo, al cancelar un derecho).
     * 
     * @param pagoId ID del pago
     */
    public void restaurarDia(UUID pagoId) {
        log.debug("Restaurando día de paquete para pago {}", pagoId);

        PaquetePago paquete = obtenerPorPago(pagoId);

        // No permitir restaurar más allá de la cantidad original
        if (paquete.getDiasRestantes() >= paquete.getDias()) {
            log.warn("Intento de restaurar día excediendo cantidad original: pago={}", pagoId);
            return;
        }

        paquete.setDiasRestantes(paquete.getDiasRestantes() + 1);
        paquetePagoRepository.save(paquete);

        log.info("Día restaurado en paquete: pago={}, días restantes={}",
                pagoId, paquete.getDiasRestantes());
    }

    /**
     * Obtiene la cantidad de días restantes en un paquete.
     * 
     * @param pagoId ID del pago
     * @return Días restantes
     */
    @Transactional(readOnly = true)
    public int obtenerDiasRestantes(UUID pagoId) {
        PaquetePago paquete = obtenerPorPago(pagoId);
        return paquete.getDiasRestantes();
    }
}
