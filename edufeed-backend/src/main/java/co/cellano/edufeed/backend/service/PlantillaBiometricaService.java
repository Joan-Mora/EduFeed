package co.cellano.edufeed.backend.service;

import co.cellano.edufeed.backend.exception.BiometricEnrollmentException;
import co.cellano.edufeed.backend.exception.ResourceNotFoundException;
import co.cellano.edufeed.backend.model.PlantillaBiometrica;
import co.cellano.edufeed.backend.repository.PlantillaBiometricaRepository;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para gestión de plantillas biométricas con cifrado AES-256-GCM.
 * FASE 2.1: Almacenamiento seguro de plantillas biométricas.
 */
@Service
@Transactional
public class PlantillaBiometricaService {
    private static final Logger log = LoggerFactory.getLogger(PlantillaBiometricaService.class);

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int GCM_IV_LENGTH = 12; // bytes (96 bits recomendado para GCM)

    private final PlantillaBiometricaRepository plantillaBiometricaRepository;
    private final SecretKey secretKey;

    public PlantillaBiometricaService(
            PlantillaBiometricaRepository plantillaBiometricaRepository,
            @Value("${biometric.encryption.key:}") String base64Key) {
        this.plantillaBiometricaRepository = plantillaBiometricaRepository;

        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException(
                    "Clave de cifrado biométrico no configurada. " +
                            "Debe establecer 'biometric.encryption.key' en application.yml con una clave base64 de 256 bits.");
        }

        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            if (keyBytes.length != 32) { // 256 bits = 32 bytes
                // Derivar una clave de 32 bytes usando SHA-256 (útil para desarrollo si se pasa
                // una cadena base64 no estándar)
                // Nota: Para producción, se recomienda proporcionar una clave AES-256 exacta en
                // Base64.
                log.warn(
                        "Clave biométrica decodificada no es de 32 bytes ({}). Derivando a 32 bytes con SHA-256 para entorno de desarrollo.",
                        keyBytes.length);
                MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                keyBytes = sha256.digest(keyBytes); // 32 bytes
            }
            this.secretKey = new SecretKeySpec(keyBytes, "AES");
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Error al decodificar clave de cifrado biométrico: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalStateException("Error al inicializar clave de cifrado biométrico: " + e.getMessage(), e);
        }
    }

    /**
     * Almacena una plantilla biométrica cifrada.
     * La plantilla se cifra con AES-256-GCM antes de persistir.
     * 
     * @param plantilla Entidad PlantillaBiometrica con plantilla en claro
     * @return PlantillaBiometrica guardada con plantilla cifrada
     * @throws BiometricEnrollmentException si el cifrado falla
     */
    public PlantillaBiometrica almacenarCifrada(PlantillaBiometrica plantilla) {
        try {
            byte[] plantillaClara = plantilla.getPlantilla();
            if (plantillaClara == null || plantillaClara.length == 0) {
                throw new BiometricEnrollmentException(
                        plantilla.getUsuario().getId().toString(),
                        plantilla.getModalidad().name(),
                        "La plantilla biométrica está vacía");
            }

            // Cifrar plantilla
            byte[] plantillaCifrada = cifrar(plantillaClara);
            plantilla.setPlantilla(plantillaCifrada);

            // Persistir
            return plantillaBiometricaRepository.save(plantilla);

        } catch (Exception e) {
            throw new BiometricEnrollmentException(
                    plantilla.getUsuario().getId().toString(),
                    plantilla.getModalidad().name(),
                    "Error al cifrar plantilla: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Recupera una plantilla biométrica descifrada.
     * 
     * @param plantillaId ID de la plantilla
     * @return PlantillaBiometrica con plantilla descifrada
     * @throws ResourceNotFoundException    si la plantilla no existe
     * @throws BiometricEnrollmentException si el descifrado falla
     */
    @Transactional(readOnly = true)
    public PlantillaBiometrica recuperarDescifrada(UUID plantillaId) {
        PlantillaBiometrica plantilla = plantillaBiometricaRepository.findById(plantillaId)
                .orElseThrow(() -> new ResourceNotFoundException("PlantillaBiometrica", plantillaId));

        try {
            byte[] plantillaCifrada = plantilla.getPlantilla();
            byte[] plantillaDescifrada = descifrar(plantillaCifrada);

            // Crear nueva instancia para no modificar la entidad gestionada
            PlantillaBiometrica resultado = new PlantillaBiometrica();
            resultado.setId(plantilla.getId());
            resultado.setUsuario(plantilla.getUsuario());
            resultado.setProveedor(plantilla.getProveedor());
            resultado.setModalidad(plantilla.getModalidad());
            resultado.setPlantilla(plantillaDescifrada);
            resultado.setCreadoEn(plantilla.getCreadoEn());
            resultado.setActivo(plantilla.isActivo());

            return resultado;

        } catch (Exception e) {
            throw new BiometricEnrollmentException(
                    plantilla.getUsuario().getId().toString(),
                    plantilla.getModalidad().name(),
                    "Error al descifrar plantilla: " + e.getMessage(),
                    e);
        }
    }

    /**
     * Desactiva una plantilla biométrica.
     * 
     * @param plantillaId ID de la plantilla
     */
    public void desactivar(UUID plantillaId) {
        PlantillaBiometrica plantilla = plantillaBiometricaRepository.findById(plantillaId)
                .orElseThrow(() -> new ResourceNotFoundException("PlantillaBiometrica", plantillaId));

        plantilla.setActivo(false);
        plantillaBiometricaRepository.save(plantilla);
    }

    /**
     * Cifra datos con AES-256-GCM.
     * Formato: [IV (12 bytes)][Datos cifrados + Auth Tag]
     * 
     * @param plaintext Datos en claro
     * @return Datos cifrados con IV prepuesto
     */
    private byte[] cifrar(byte[] plaintext) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);

        // Generar IV aleatorio
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

        byte[] ciphertext = cipher.doFinal(plaintext);

        // Concatenar IV + ciphertext
        byte[] resultado = new byte[GCM_IV_LENGTH + ciphertext.length];
        System.arraycopy(iv, 0, resultado, 0, GCM_IV_LENGTH);
        System.arraycopy(ciphertext, 0, resultado, GCM_IV_LENGTH, ciphertext.length);

        return resultado;
    }

    /**
     * Descifra datos con AES-256-GCM.
     * 
     * @param ciphertext Datos cifrados con IV prepuesto
     * @return Datos en claro
     */
    private byte[] descifrar(byte[] ciphertext) throws Exception {
        if (ciphertext.length < GCM_IV_LENGTH) {
            throw new IllegalArgumentException("Datos cifrados inválidos (demasiado cortos)");
        }

        Cipher cipher = Cipher.getInstance(ALGORITHM);

        // Extraer IV
        byte[] iv = Arrays.copyOfRange(ciphertext, 0, GCM_IV_LENGTH);
        byte[] encryptedData = Arrays.copyOfRange(ciphertext, GCM_IV_LENGTH, ciphertext.length);

        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

        return cipher.doFinal(encryptedData);
    }

    /**
     * Genera una clave AES-256 aleatoria en Base64 para configuración.
     * Este método es útil para generar la clave inicial.
     * 
     * @return Clave en formato Base64
     */
    public static String generarClave() {
        SecureRandom random = new SecureRandom();
        byte[] key = new byte[32]; // 256 bits
        random.nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}
