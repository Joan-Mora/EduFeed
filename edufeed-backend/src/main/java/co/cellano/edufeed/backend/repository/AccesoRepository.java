package co.cellano.edufeed.backend.repository;

import co.cellano.edufeed.backend.model.Acceso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

/**
 * Repositorio para gestión de accesos.
 * Soporta consultas dinámicas con Specification para filtros complejos.
 * 
 * @since FASE 1, extendido en FASE 2.3
 */
public interface AccesoRepository extends JpaRepository<Acceso, UUID>, JpaSpecificationExecutor<Acceso> {
}
