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
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
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
 * Aquí manejamos todo lo de autenticación biométrica con WebAuthn.
 * Básicamente es para que la gente use la huella del teléfono.
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
    private final BiometricAuthService biometricAuthService;
    private final SecureRandom secureRandom = new SecureRandom();

    public WebAuthnService(SesionWebAuthnRepository sesionRepository,
            CredencialWebAuthnRepository credencialRepository,
            DispositivoRepository dispositivoRepository,
            UsuarioRepository usuarioRepository,
            BiometricAuthService biometricAuthService) {
        this.sesionRepository = sesionRepository;
        this.credencialRepository = credencialRepository;
        this.dispositivoRepository = dispositivoRepository;
        this.usuarioRepository = usuarioRepository;
        this.biometricAuthService = biometricAuthService;
    }

    /**
     * Arranca el proceso para registrar una huella con WebAuthn.
     */
    @Transactional
    public IniciarWebAuthnResponse iniciarRegistro(IniciarRegistroWebAuthnRequest request) {
        // Primero nos aseguramos de que el usuario exista
        usuarioRepository.findByDocumento(request.getUsuarioDocumento())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Usuario", "documento", request.getUsuarioDocumento()));

        // Generamos un challenge aleatorio para esta sesión
        String challenge = generarChallenge();

        // Crear sesión
        SesionWebAuthn sesion = new SesionWebAuthn();
        sesion.setChallenge(challenge);
        sesion.setUsuarioDocumento(request.getUsuarioDocumento());
        sesion.setTipo("REGISTRO");
        sesion.setEstado("PENDIENTE");
        sesion.setExpiraEn(OffsetDateTime.now().plusMinutes(SESSION_EXPIRY_MINUTES));

        sesion = sesionRepository.save(sesion);

        logger.info("Sesión de registro WebAuthn iniciada: {} para usuario: {}", sesion.getId(),
                request.getUsuarioDocumento());

        return new IniciarWebAuthnResponse(sesion.getId(), challenge, request.getUsuarioDocumento());
    }

    /**
     * Termina el proceso de registro una vez que el cliente envía los datos.
     */
    @Transactional
    public EstadoSesionWebAuthnResponse completarRegistro(UUID sesionId, CompletarRegistroWebAuthnRequest request) {
        // Buscamos la sesión que se inició antes
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

        // Traemos el usuario de la base de datos
        Usuario usuario = usuarioRepository.findByDocumento(sesion.getUsuarioDocumento())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "documento", sesion.getUsuarioDocumento()));

        // Creamos o actualizamos el registro del dispositivo
        Dispositivo dispositivo = new Dispositivo();
        dispositivo.setUsuario(usuario);
        dispositivo.setPlataforma("WEB"); // Más adelante esto se puede detectar mejor desde el cliente
        dispositivo.setModelo("WebAuthn");
        dispositivo.setActivo(true);
        dispositivo = dispositivoRepository.save(dispositivo);

        // Ahora guardamos la credencial biométrica
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

        logger.info("Registro WebAuthn completado para sesión: {}, usuario: {}", sesionId,
                sesion.getUsuarioDocumento());

        EstadoSesionWebAuthnResponse response = new EstadoSesionWebAuthnResponse(sesionId, "COMPLETADA", "REGISTRO");
        response.setExito(true);
        response.setMensaje("Registro biométrico completado exitosamente");
        response.setCompletadoEn(sesion.getCompletadoEn());
        return response;
    }

    /**
     * Comienza el proceso para que alguien se autentique con su huella.
     */
    @Transactional
    public IniciarWebAuthnResponse iniciarAutenticacion(IniciarAutenticacionWebAuthnRequest request) {
        // Nos aseguramos de que el usuario exista
        Usuario usuario = usuarioRepository.findByDocumento(request.getUsuarioDocumento())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Usuario", "documento", request.getUsuarioDocumento()));

        // Traemos todas las credenciales activas que tenga registradas
        List<CredencialWebAuthn> credenciales = credencialRepository.findByUsuarioIdAndActivoTrue(usuario.getId());
        if (credenciales.isEmpty()) {
            throw new InvalidBusinessRuleException("El usuario no tiene credenciales WebAuthn registradas");
        }

        // Generamos un nuevo challenge
        String challenge = generarChallenge();

        // Crear sesión
        SesionWebAuthn sesion = new SesionWebAuthn();
        sesion.setChallenge(challenge);
        sesion.setUsuarioDocumento(request.getUsuarioDocumento());
        sesion.setTipo("AUTENTICACION");
        sesion.setEstado("PENDIENTE");
        sesion.setExpiraEn(OffsetDateTime.now().plusMinutes(SESSION_EXPIRY_MINUTES));
        sesion = sesionRepository.save(sesion);

        logger.info("Sesión de autenticación WebAuthn iniciada: {} para usuario: {}", sesion.getId(),
                request.getUsuarioDocumento());

        IniciarWebAuthnResponse response = new IniciarWebAuthnResponse(sesion.getId(), challenge,
                request.getUsuarioDocumento());

        // Le pasamos la lista de credenciales que puede usar
        List<String> allowCredentials = credenciales.stream()
                .map(CredencialWebAuthn::getCredentialId)
                .collect(Collectors.toList());
        response.setAllowCredentials(allowCredentials);

        return response;
    }

    /**
     * Termina el proceso de autenticación una vez que el cliente envía la
     * respuesta.
     */
    @Transactional
    public EstadoSesionWebAuthnResponse completarAutenticacion(UUID sesionId,
            CompletarAutenticacionWebAuthnRequest request) {
        // Buscamos la sesión que se creó al iniciar
        SesionWebAuthn sesion = sesionRepository.findById(sesionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sesión WebAuthn", sesionId));

        // Verificamos que todo esté bien con la sesión
        if (!"PENDIENTE".equals(sesion.getEstado())) {
            throw new InvalidBusinessRuleException("La sesión ya fue completada o expiró");
        }
        if (sesion.getExpiraEn().isBefore(OffsetDateTime.now())) {
            sesion.setEstado("EXPIRADA");
            sesionRepository.save(sesion);
            throw new InvalidBusinessRuleException("La sesión ha expirado");
        }

        // Buscamos la credencial, probando diferentes encodings porque a veces vienen
        // en base64 o base64url
        CredencialWebAuthn credencial = null;
        String incomingId = request.getCredentialId();
        try {
            byte[] raw = base64UrlDecode(incomingId); // acepta base64 y base64url
            String b64std = java.util.Base64.getEncoder().encodeToString(raw);
            String b64url = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
            java.util.List<String> candidates = java.util.List.of(incomingId, b64std, b64url);
            for (String c : candidates) {
                var opt = credencialRepository.findByCredentialId(c);
                if (opt.isPresent()) {
                    credencial = opt.get();
                    break;
                }
            }
        } catch (Exception ignore) {
        }
        if (credencial == null) {
            throw new ResourceNotFoundException("Credencial", "credentialId", incomingId);
        }

        // Nos aseguramos de que la credencial realmente sea del usuario que inició
        // sesión
        if (!credencial.getUsuario().getDocumento().equals(sesion.getUsuarioDocumento())) {
            sesion.setEstado("FALLIDA");
            sesion.setResultado("{\"exito\": false, \"error\": \"Credencial no coincide con usuario\"}");
            sesionRepository.save(sesion);
            throw new InvalidBusinessRuleException("Credencial no pertenece al usuario");
        }

        // En producción real acá verificaríamos la firma criptográfica usando la
        // publicKey
        // Por ahora lo simulamos para que funcione
        // Decodificamos los datos que vienen del cliente
        byte[] authenticatorData = base64UrlDecode(request.getAuthenticatorData());
        byte[] clientDataJSON = base64UrlDecode(request.getClientDataJSON());
        byte[] signatureBytes = base64UrlDecode(request.getSignature());

        // Chequeamos que el challenge coincida con el que enviamos
        if (!validarChallengeEnClientData(clientDataJSON, sesion.getChallenge())) {
            sesion.setEstado("FALLIDA");
            sesion.setResultado("{\"exito\": false, \"error\": \"Challenge no coincide\"}");
            sesionRepository.save(sesion);
            throw new InvalidBusinessRuleException("Challenge no coincide");
        }

        // Sacamos el signCount de los bytes del authenticatorData
        if (authenticatorData.length < 37) {
            sesion.setEstado("FALLIDA");
            sesion.setResultado("{\"exito\": false, \"error\": \"authenticatorData inválido\"}");
            sesionRepository.save(sesion);
            throw new InvalidBusinessRuleException("authenticatorData inválido");
        }
        long newSignCount = ((long) (authenticatorData[33] & 0xFF) << 24) |
                ((long) (authenticatorData[34] & 0xFF) << 16) |
                ((long) (authenticatorData[35] & 0xFF) << 8) |
                ((long) (authenticatorData[36] & 0xFF));

        // Verificamos que el contador haya aumentado, aunque no somos muy estrictos por
        // si el dispositivo no lo incrementa
        if (newSignCount > 0 && newSignCount <= credencial.getSignCount()) {
            logger.warn("signCount no aumentó: anterior={}, nuevo={}", credencial.getSignCount(), newSignCount);
        }

        boolean verificacionExitosa = verificarFirmaCompleta(credencial.getPublicKey(), authenticatorData,
                clientDataJSON,
                signatureBytes);

        if (!verificacionExitosa) {
            sesion.setEstado("FALLIDA");
            sesion.setResultado("{\"exito\": false, \"error\": \"Verificación de firma fallida\"}");
            sesionRepository.save(sesion);
            throw new InvalidBusinessRuleException("Verificación de firma fallida");
        }

        // Actualizamos el contador de usos si aumentó
        if (newSignCount > credencial.getSignCount()) {
            credencial.setSignCount(newSignCount);
        } else {
            credencial.setSignCount(credencial.getSignCount() + 1); // Por si acaso el dispositivo no lo actualiza
        }
        credencialRepository.save(credencial);

        // Actualizar sesión
        sesion.setEstado("COMPLETADA");
        sesion.setCompletadoEn(OffsetDateTime.now());
        sesion.setResultado("{\"exito\": true, \"usuarioDocumento\": \"" + sesion.getUsuarioDocumento() + "\"}");
        sesionRepository.save(sesion);

        logger.info("Autenticación WebAuthn completada para sesión: {}, usuario: {}", sesionId,
                sesion.getUsuarioDocumento());

        // Le avisamos a la app de escritorio que todo salió bien
        try {
            biometricAuthService.notifyDesktop(sesion.getUsuarioDocumento(), credencial.getUsuario());
        } catch (Exception e) {
            logger.warn("No se pudo notificar al desktop el éxito de WebAuthn: {}", e.getMessage());
        }

        EstadoSesionWebAuthnResponse response = new EstadoSesionWebAuthnResponse(sesionId, "COMPLETADA",
                "AUTENTICACION");
        response.setExito(true);
        response.setMensaje("Autenticación biométrica exitosa");
        response.setCompletadoEn(sesion.getCompletadoEn());
        return response;
    }

    /**
     * Consulta cómo va una sesión WebAuthn. Esto lo usa el desktop para ir
     * chequeando el estado.
     */
    @Transactional(readOnly = true)
    public EstadoSesionWebAuthnResponse obtenerEstadoSesion(UUID sesionId) {
        SesionWebAuthn sesion = sesionRepository.findById(sesionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sesión WebAuthn", sesionId));

        // Si ya expiró, la marcamos como tal
        if ("PENDIENTE".equals(sesion.getEstado()) && sesion.getExpiraEn().isBefore(OffsetDateTime.now())) {
            sesion.setEstado("EXPIRADA");
            sesionRepository.save(sesion);
        }

        EstadoSesionWebAuthnResponse response = new EstadoSesionWebAuthnResponse(sesionId, sesion.getEstado(),
                sesion.getTipo());
        response.setCreadoEn(sesion.getCreadoEn());
        response.setExpiraEn(sesion.getExpiraEn());
        response.setCompletadoEn(sesion.getCompletadoEn());
        // Agregamos los datos que la PWA va a necesitar
        response.setChallenge(sesion.getChallenge());
        response.setUsuarioDocumento(sesion.getUsuarioDocumento());

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

        // Si es autenticación, incluimos las credenciales permitidas
        if ("AUTENTICACION".equals(sesion.getTipo())) {
            usuarioRepository.findByDocumento(sesion.getUsuarioDocumento()).ifPresent(usuario -> {
                List<CredencialWebAuthn> creds = credencialRepository.findByUsuarioIdAndActivoTrue(usuario.getId());
                List<String> allow = creds.stream().map(CredencialWebAuthn::getCredentialId)
                        .collect(java.util.stream.Collectors.toList());
                response.setAllowCredentials(allow);
            });
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
     * Verifica la firma digital (simulado - en producción usar librería de
     * WebAuthn).
     */
    private boolean verificarFirmaCompleta(String publicKeyPem, byte[] authenticatorData, byte[] clientDataJSON,
            byte[] signatureBytes) {
        try {
            if (publicKeyPem == null || publicKeyPem.isBlank() || authenticatorData == null || clientDataJSON == null
                    || signatureBytes == null) {
                return false;
            }
            if (signatureBytes.length < 32) {
                logger.warn("Firma demasiado corta: {} bytes", signatureBytes.length);
                return false;
            }
            String cleaned = publicKeyPem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\r?\n", "")
                    .trim();
            byte[] keyBytes = Base64.getDecoder().decode(cleaned);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            PublicKey pk;
            try {
                pk = KeyFactory.getInstance("EC").generatePublic(spec);
            } catch (Exception e) {
                pk = KeyFactory.getInstance("RSA").generatePublic(spec);
            }
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] clientDataHash = sha256.digest(clientDataJSON);
            // Mensaje = authenticatorData || clientDataHash
            byte[] message = new byte[authenticatorData.length + clientDataHash.length];
            System.arraycopy(authenticatorData, 0, message, 0, authenticatorData.length);
            System.arraycopy(clientDataHash, 0, message, authenticatorData.length, clientDataHash.length);
            Signature verifier = Signature.getInstance(pk.getAlgorithm().equalsIgnoreCase("EC") ? "SHA256withECDSA"
                    : "SHA256withRSA");
            verifier.initVerify(pk);
            verifier.update(message);
            boolean ok = verifier.verify(signatureBytes);
            if (!ok) {
                logger.warn("Verificación de firma fallida sobre authenticatorData||hash(clientDataJSON)");
            }
            return ok;
        } catch (Exception ex) {
            logger.error("Error en verificación completa WebAuthn: {}", ex.getMessage(), ex);
            return false;
        }
    }

    private byte[] base64UrlDecode(String v) {
        if (v == null)
            return new byte[0];
        String padded = v.replace('-', '+').replace('_', '/');
        int mod = padded.length() % 4;
        if (mod > 0)
            padded += "====".substring(mod);
        return Base64.getDecoder().decode(padded);
    }

    private boolean validarChallengeEnClientData(byte[] clientDataJSON, String expectedChallenge) {
        try {
            String json = new String(clientDataJSON, java.nio.charset.StandardCharsets.UTF_8);
            // Parse simple sin dependencia externa adicional (ya está Jackson en el
            // proyecto)
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(json);
            String challengeEncoded = node.get("challenge").asText();
            // challenge en clientDataJSON viene Base64URL del challenge original
            byte[] decodedClientChallenge = base64UrlDecode(challengeEncoded);
            byte[] decodedExpected = base64UrlDecode(expectedChallenge);
            return java.util.Arrays.equals(decodedClientChallenge, decodedExpected);
        } catch (Exception e) {
            logger.error("Error validando challenge en clientDataJSON: {}", e.getMessage());
            return false;
        }
    }
}
