package co.cellano.edufeed.backend.service;

import co.cellano.edufeed.backend.dto.request.CompletarAutenticacionWebAuthnRequest;
import co.cellano.edufeed.backend.dto.request.CompletarRegistroWebAuthnRequest;
import co.cellano.edufeed.backend.dto.request.IniciarAutenticacionWebAuthnRequest;
import co.cellano.edufeed.backend.dto.request.IniciarRegistroWebAuthnRequest;
import co.cellano.edufeed.backend.dto.response.EstadoSesionWebAuthnResponse;
import co.cellano.edufeed.backend.dto.response.IniciarWebAuthnResponse;
import co.cellano.edufeed.backend.exception.InvalidBusinessRuleException;
import co.cellano.edufeed.backend.exception.ResourceNotFoundException;
import co.cellano.edufeed.backend.model.*;
import co.cellano.edufeed.backend.repository.*;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para gestión de autenticación biométrica WebAuthn (huella por teléfono).
 */
@Service
public class WebAuthnService {

    private static final Logger logger = LoggerFactory.getLogger(WebAuthnService.class);
    private static final int CHALLENGE_LENGTH = 32;
    private static final int SESSION_EXPIRY_MINUTES = 5;

    private final SesionWebAuthnRepository sesionRepository;
    private final CredencialWebAuthnRepository credencialRepository;
    private final DispositivoRepository dispositivoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public WebAuthnService(SesionWebAuthnRepository sesionRepository,
                          CredencialWebAuthnRepository credencialRepository,
                          DispositivoRepository dispositivoRepository,
                          UsuarioRepository usuarioRepository) {
        this.sesionRepository = sesionRepository;
        this.credencialRepository = credencialRepository;
        this.dispositivoRepository = dispositivoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Inicia el proceso de registro WebAuthn.
     */
    @Transactional
    public IniciarWebAuthnResponse iniciarRegistro(IniciarRegistroWebAuthnRequest request) {
        // Verificar que el usuario existe
        Usuario usuario = usuarioRepository.findByDocumento(request.getUsuarioDocumento())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "documento", request.getUsuarioDocumento()));

        // Generar challenge aleatorio
        String challenge = generarChallenge();

        // Crear sesión
        SesionWebAuthn sesion = new SesionWebAuthn();
        sesion.setChallenge(challenge);
        sesion.setUsuarioDocumento(request.getUsuarioDocumento());
        sesion.setTipo("REGISTRO");
        sesion.setEstado("PENDIENTE");
        sesion.setExpiraEn(OffsetDateTime.now().plusMinutes(SESSION_EXPIRY_MINUTES));

        sesion = sesionRepository.save(sesion);

        logger.info("Sesión de registro WebAuthn iniciada: {} para usuario: {}", sesion.getId(), request.getUsuarioDocumento());

        return new IniciarWebAuthnResponse(sesion.getId(), challenge, request.getUsuarioDocumento());
    }

    /**
     * Completa el proceso de registro WebAuthn.
     */
    @Transactional
    public EstadoSesionWebAuthnResponse completarRegistro(UUID sesionId, CompletarRegistroWebAuthnRequest request) {
        // Buscar sesión
        SesionWebAuthn sesion = sesionRepository.findById(sesionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sesión WebAuthn", sesionId));

        // Validar estado y expiración
        if (!"PENDIENTE".equals(sesion.getEstado())) {
            throw new InvalidBusinessRuleException("La sesión ya fue completada o expiró");
        }
        if (sesion.getExpiraEn().isBefore(OffsetDateTime.now())) {
            sesion.setEstado("EXPIRADA");
            sesionRepository.save(sesion);
            throw new InvalidBusinessRuleException("La sesión ha expirado");
        }

        // Buscar usuario
        Usuario usuario = usuarioRepository.findByDocumento(sesion.getUsuarioDocumento())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "documento", sesion.getUsuarioDocumento()));

        // Crear o actualizar dispositivo
        Dispositivo dispositivo = new Dispositivo();
        dispositivo.setUsuario(usuario);
        dispositivo.setPlataforma("WEB"); // Se puede mejorar detectando desde el cliente
        dispositivo.setModelo("WebAuthn");
        dispositivo.setActivo(true);
        dispositivo = dispositivoRepository.save(dispositivo);

        // Guardar credencial
        CredencialWebAuthn credencial = new CredencialWebAuthn();
        credencial.setUsuario(usuario);
        credencial.setDispositivo(dispositivo);
        credencial.setCredentialId(request.getCredentialId());
        credencial.setPublicKey(request.getPublicKey());
        credencial.setSignCount(0L);
        credencial.setActivo(true);
        credencialRepository.save(credencial);

        // Actualizar sesión
        sesion.setEstado("COMPLETADA");
        sesion.setCompletadoEn(OffsetDateTime.now());
        sesion.setResultado("{\"exito\": true, \"credentialId\": \"" + request.getCredentialId() + "\"}");
        sesionRepository.save(sesion);

        logger.info("Registro WebAuthn completado para sesión: {}, usuario: {}", sesionId, sesion.getUsuarioDocumento());

        EstadoSesionWebAuthnResponse response = new EstadoSesionWebAuthnResponse(sesionId, "COMPLETADA", "REGISTRO");
        response.setExito(true);
        response.setMensaje("Registro biométrico completado exitosamente");
        response.setCompletadoEn(sesion.getCompletadoEn());
        return response;
    }

    /**
     * Inicia el proceso de autenticación WebAuthn.
     */
    @Transactional
    public IniciarWebAuthnResponse iniciarAutenticacion(IniciarAutenticacionWebAuthnRequest request) {
        // Verificar que el usuario existe
        Usuario usuario = usuarioRepository.findByDocumento(request.getUsuarioDocumento())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "documento", request.getUsuarioDocumento()));

        // Obtener credenciales activas del usuario
        List<CredencialWebAuthn> credenciales = credencialRepository.findByUsuarioIdAndActivoTrue(usuario.getId());
        if (credenciales.isEmpty()) {
            throw new InvalidBusinessRuleException("El usuario no tiene credenciales WebAuthn registradas");
        }

        // Generar challenge
        String challenge = generarChallenge();

        // Crear sesión
        SesionWebAuthn sesion = new SesionWebAuthn();
        sesion.setChallenge(challenge);
        sesion.setUsuarioDocumento(request.getUsuarioDocumento());
        sesion.setTipo("AUTENTICACION");
        sesion.setEstado("PENDIENTE");
        sesion.setExpiraEn(OffsetDateTime.now().plusMinutes(SESSION_EXPIRY_MINUTES));
        sesion = sesionRepository.save(sesion);

        logger.info("Sesión de autenticación WebAuthn iniciada: {} para usuario: {}", sesion.getId(), request.getUsuarioDocumento());

        IniciarWebAuthnResponse response = new IniciarWebAuthnResponse(sesion.getId(), challenge, request.getUsuarioDocumento());
        
        // Agregar IDs de credenciales permitidas
        List<String> allowCredentials = credenciales.stream()
                .map(CredencialWebAuthn::getCredentialId)
                .collect(Collectors.toList());
        response.setAllowCredentials(allowCredentials);
        
        return response;
    }

    /**
     * Completa el proceso de autenticación WebAuthn.
     */
    @Transactional
    public EstadoSesionWebAuthnResponse completarAutenticacion(UUID sesionId, CompletarAutenticacionWebAuthnRequest request) {
        // Buscar sesión
        SesionWebAuthn sesion = sesionRepository.findById(sesionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sesión WebAuthn", sesionId));

        // Validar estado y expiración
        if (!"PENDIENTE".equals(sesion.getEstado())) {
            throw new InvalidBusinessRuleException("La sesión ya fue completada o expiró");
        }
        if (sesion.getExpiraEn().isBefore(OffsetDateTime.now())) {
            sesion.setEstado("EXPIRADA");
            sesionRepository.save(sesion);
            throw new InvalidBusinessRuleException("La sesión ha expirado");
        }

        // Buscar credencial
        CredencialWebAuthn credencial = credencialRepository.findByCredentialId(request.getCredentialId())
                .orElseThrow(() -> new ResourceNotFoundException("Credencial", "credentialId", request.getCredentialId()));

        // Validar que la credencial pertenece al usuario de la sesión
        if (!credencial.getUsuario().getDocumento().equals(sesion.getUsuarioDocumento())) {
            sesion.setEstado("FALLIDA");
            sesion.setResultado("{\"exito\": false, \"error\": \"Credencial no coincide con usuario\"}");
            sesionRepository.save(sesion);
            throw new InvalidBusinessRuleException("Credencial no pertenece al usuario");
        }

        // En una implementación real, aquí se verificaría la firma usando la publicKey
        // Por ahora, simulamos la verificación exitosa
        boolean verificacionExitosa = verificarFirma(credencial.getPublicKey(), sesion.getChallenge(), request.getSignature());

        if (!verificacionExitosa) {
            sesion.setEstado("FALLIDA");
            sesion.setResultado("{\"exito\": false, \"error\": \"Verificación de firma fallida\"}");
            sesionRepository.save(sesion);
            throw new InvalidBusinessRuleException("Verificación de firma fallida");
        }

        // Actualizar signCount
        credencial.setSignCount(credencial.getSignCount() + 1);
        credencialRepository.save(credencial);

        // Actualizar sesión
        sesion.setEstado("COMPLETADA");
        sesion.setCompletadoEn(OffsetDateTime.now());
        sesion.setResultado("{\"exito\": true, \"usuarioDocumento\": \"" + sesion.getUsuarioDocumento() + "\"}");
        sesionRepository.save(sesion);

        logger.info("Autenticación WebAuthn completada para sesión: {}, usuario: {}", sesionId, sesion.getUsuarioDocumento());

        EstadoSesionWebAuthnResponse response = new EstadoSesionWebAuthnResponse(sesionId, "COMPLETADA", "AUTENTICACION");
        response.setExito(true);
        response.setMensaje("Autenticación biométrica exitosa");
        response.setCompletadoEn(sesion.getCompletadoEn());
        return response;
    }

    /**
     * Obtiene el estado de una sesión WebAuthn (para polling desde el desktop).
     */
    @Transactional(readOnly = true)
    public EstadoSesionWebAuthnResponse obtenerEstadoSesion(UUID sesionId) {
        SesionWebAuthn sesion = sesionRepository.findById(sesionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sesión WebAuthn", sesionId));

        // Verificar expiración
        if ("PENDIENTE".equals(sesion.getEstado()) && sesion.getExpiraEn().isBefore(OffsetDateTime.now())) {
            sesion.setEstado("EXPIRADA");
            sesionRepository.save(sesion);
        }

        EstadoSesionWebAuthnResponse response = new EstadoSesionWebAuthnResponse(sesionId, sesion.getEstado(), sesion.getTipo());
        response.setCreadoEn(sesion.getCreadoEn());
        response.setExpiraEn(sesion.getExpiraEn());
        response.setCompletadoEn(sesion.getCompletadoEn());

        if ("COMPLETADA".equals(sesion.getEstado())) {
            response.setExito(true);
            response.setMensaje("Sesión completada exitosamente");
        } else if ("EXPIRADA".equals(sesion.getEstado())) {
            response.setExito(false);
            response.setMensaje("La sesión ha expirado");
        } else if ("FALLIDA".equals(sesion.getEstado())) {
            response.setExito(false);
            response.setMensaje("La sesión falló durante la verificación");
        } else {
            response.setMensaje("Esperando respuesta del dispositivo móvil");
        }

        return response;
    }

    /**
     * Genera un challenge aleatorio seguro.
     */
    private String generarChallenge() {
        byte[] randomBytes = new byte[CHALLENGE_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Verifica la firma digital (simulado - en producción usar librería de WebAuthn).
     */
    private boolean verificarFirma(String publicKey, String challenge, String signature) {
        // TODO: Implementar verificación real de firma ECDSA/RSA
        // Por ahora, simplemente verificar que los parámetros no estén vacíos
        return publicKey != null && !publicKey.isEmpty() 
            && challenge != null && !challenge.isEmpty()
            && signature != null && !signature.isEmpty();
    }
}
