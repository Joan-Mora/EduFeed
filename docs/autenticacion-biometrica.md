# 🔐 Sistema de Autenticación Biométrica

## 📋 Resumen

Sistema completo de autenticación biométrica con QR Code para EduFeed, que permite a los usuarios autenticarse usando:
- 🔐 **Huella digital** (WebAuthn)
- 👤 **Reconocimiento facial** (MediaDevices Camera)
- 🎤 **Reconocimiento de voz** (MediaRecorder)

## 🏗️ Arquitectura

```
┌─────────────┐      QR Code       ┌─────────────┐
│   Desktop   │ ───────────────────>│   Móvil     │
│   (JavaFX)  │                     │  (Browser)  │
└──────┬──────┘                     └──────┬──────┘
       │                                   │
       │ Polling (2s)                      │ POST verify
       │                                   │
       └───────────> ┌─────────────┐ <────┘
                     │   Backend   │
                     │ (Spring)    │
                     └─────────────┘
```

## 🎯 Flujo de Autenticación

1. **Usuario ingresa documento** en módulo Access del desktop
2. **Desktop genera QR** con URL: `http://localhost:8080/auth/biometric?userId=XXX&token=YYY`
3. **Usuario escanea QR** con su móvil
4. **Móvil carga página HTML** que muestra métodos biométricos disponibles
5. **Usuario selecciona método** (Huella/FaceID/Voz) y autentica
6. **Backend valida** datos biométricos
7. **Desktop detecta** autenticación exitosa (polling cada 2 segundos)
8. **Desktop muestra** información del usuario y pagos

## 📁 Archivos Implementados

### Backend (`edufeed-backend`)

#### Controller
**`BiometricAuthController.java`**
```java
@Controller
@RequestMapping("/auth/biometric")
public class BiometricAuthController {
    
    // GET /auth/biometric?userId=XXX&token=YYY
    // Retorna página HTML móvil con métodos disponibles
    
    // POST /auth/biometric/verify
    // Recibe datos biométricos y valida
    
    // GET /auth/biometric/status/{userId}
    // Polling desde desktop para saber si autenticó
}
```

#### Service
**`BiometricAuthService.java`**
```java
@Service
public class BiometricAuthService {
    
    // Verificación ML-based
    boolean verifyFingerprint(Usuario user, String data);  // WebAuthn signature
    boolean verifyFaceId(Usuario user, String faceData);   // L2(descriptor) < 0.6
    boolean verifyVoice(Usuario user, String voiceData);   // cosine(mfcc) > 0.85
    
    // Distancia euclidiana para face-api.js descriptors
    private static double l2(double[] a, double[] b) {
        double s=0; 
        for (int i=0;i<a.length;i++) { 
            double d=a[i]-b[i]; s+=d*d; 
        } 
        return Math.sqrt(s);
    }
    
    // Similaridad coseno para MFCC
    private static double cosine(double[] a, double[] b) {
        double dot=0, na=0, nb=0; 
        for (int i=0;i<a.length;i++) { 
            dot+=a[i]*b[i]; na+=a[i]*a[i]; nb+=b[i]*b[i]; 
        }
        return dot/(Math.sqrt(na)*Math.sqrt(nb));
    }
    
    // Gestión de sesión
    void notifyDesktop(String userId, Usuario user);
    Map<String, Object> getAuthStatus(String userId);
    Map<String, Object> getUserPaymentData(Usuario user);
}
```

#### DTOs
**`BiometricAuthRequest.java`**
```java
public class BiometricAuthRequest {
    private String userId;      // Documento del usuario
    private String token;       // Token de seguridad
    private String method;      // "fingerprint", "faceid", "voice"
    private String data;        // Datos biométricos en base64
    private boolean secondAttempt;  // Si es segundo intento
}
```

**`BiometricAuthResponse.java`**
```java
public class BiometricAuthResponse {
    private boolean success;
    private String message;
    private Map<String, Object> userData;
    private boolean requiresSecondFactor;
}
```

#### Vista
**`templates/biometric-auth.html`**
- Página móvil responsive con gradiente moderno
- Tres botones para métodos biométricos
- **Huella**: WebAuthn navigator.credentials.get() con mediation:silent
- **FaceID**: Preview en vivo con face-api.js detection loop + captura explícita
- **Voz**: Grabación con timer (mín 3s), botón start/stop, extracción MFCC con Meyda
- Fetch API para comunicación con backend

**Tecnologías Frontend**
```javascript
// face-api.js (vladmandic) - Models CDN
await faceapi.nets.ssdMobilenetv1.loadFromUri('/webjars/face-api/models');
await faceapi.nets.faceLandmark68Net.loadFromUri('/webjars/face-api/models');
await faceapi.nets.faceRecognitionNet.loadFromUri('/webjars/face-api/models');

// Meyda para análisis de voz
Meyda.extract('mfcc', audioFrame, {
    sampleRate: 44100,
    bufferSize: 1024,
    melBands: 26,
    numberOfMFCCCoefficients: 13
});
```

### Desktop (`edufeed-desktop`)

**`AccessCheckModuleV2.java`**
```java
public class AccessCheckModuleV2 extends VBox {
    
    // Genera QR con ZXing
    private void generateQRCode(String userId);
    
    // Polling cada 2 segundos (max 120s)
    private void simulateAuthenticationFlow(String userId);
    
    // Muestra datos del usuario al autenticar
    private void showUserInfo(String jsonResponse);
    
    // Parser JSON manual (sin Jackson)
    private String extractJsonValue(String json, String key);
}
```

## 🔧 Dependencias

### Backend (`pom.xml`)
```xml
<!-- Ya incluidas en Spring Boot -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

### Desktop (`pom.xml`)
```xml
<!-- ZXing para QR Code -->
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>core</artifactId>
    <version>3.5.3</version>
</dependency>
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>javase</artifactId>
    <version>3.5.3</version>
</dependency>
```

## 🧪 Cómo Probar

### 1. Iniciar Base de Datos
```powershell
# En VS Code, ejecutar tarea:
DB: up
```

### 2. Iniciar Backend
```powershell
# En VS Code, ejecutar tarea:
Backend: run

# O manualmente:
cd edufeed-backend
mvn spring-boot:run
```

### 3. Iniciar Desktop
```powershell
# En VS Code, ejecutar tarea:
Desktop: run

# O manualmente:
cd edufeed-desktop
mvn javafx:run
```

### 4. Navegar al Módulo Access
- En el desktop, hacer clic en "Access" en el menú lateral
- Ingresar documento de usuario (ej: `123456`)
- Hacer clic en "Generar QR"

### 5. Escanear QR con Móvil
- Escanear el QR mostrado en pantalla
- O abrir manualmente: `http://localhost:8080/auth/biometric?userId=123456&token=abc123`
- **Nota**: El móvil debe estar en la misma red que el PC

### 6. Autenticar en Móvil
- Seleccionar método biométrico:
  - **Huella**: Usa sensor del dispositivo (WebAuthn)
  - **FaceID**: Activa cámara frontal
  - **Voz**: Graba audio de 3 segundos
- Esperar confirmación

### 7. Ver Resultado en Desktop
- El desktop detectará automáticamente la autenticación
- Mostrará:
  - Nombre completo
  - Número de cédula
  - Sueldo
  - Pagos pendientes
  - Pagos realizados
  - Pagos programados

## 🔒 Seguridad

### Token de Sesión
- Cada QR incluye un token único generado con UUID
- El token se valida en el backend antes de aceptar autenticación
- Tiempo de expiración: 2 minutos (120 segundos)

### Almacenamiento Temporal
- Las sesiones se guardan en `ConcurrentHashMap` (memoria)
- Se eliminan automáticamente después de ser consultadas (one-time use)
- No se persisten en base de datos por seguridad

### Datos Biométricos
- Los datos se envían en JSON con vectores numéricos desde el móvil
- **FaceID**: 128D descriptor de face-api.js → L2 distance < 0.6
- **Voz**: 13D MFCC de Meyda → cosine similarity > 0.85
- **Huella**: WebAuthn con public key PEM y signature verification
- No se almacenan imágenes/audio raw, solo templates cifrados

## 📊 Datos de Prueba

### Usuarios con Métodos Biométricos Simulados

| Documento | Huella | FaceID | Voz |
|-----------|--------|--------|-----|
| 123456    | ✅     | ✅     | ✅  |
| 789012    | ✅     | ✅     | ❌  |

Para agregar más usuarios, editar en `BiometricAuthService.java`:
```java
private static final Set<String> USERS_WITH_FINGERPRINT = Set.of("123456", "789012");
private static final Set<String> USERS_WITH_FACEID = Set.of("123456", "789012");
private static final Set<String> USERS_WITH_VOICE = Set.of("123456");
```

## 🚀 Mejoras Futuras

### Corto Plazo
- [x] ~~Implementar validación real de huellas~~ (WebAuthn signature verification)
- [x] ~~Agregar reconocimiento facial con ML~~ (face-api.js con L2 distance)
- [x] ~~Implementar análisis de voz~~ (Meyda MFCC con cosine similarity)
- [x] ~~Agregar tabla `biometric_data` en BD~~ (PlantillaBiometrica con JSON descriptors)
- [ ] Ajustar umbrales L2/cosine tras análisis de producción
- [ ] Liveness detection (anti-spoofing) para FaceID

### Mediano Plazo
- [ ] WebSocket en lugar de polling (notificación instantánea)
- [ ] Soporte para múltiples factores (2FA: huella + voz)
- [ ] Logs de auditoría de intentos de autenticación
- [ ] Rate limiting para prevenir ataques de fuerza bruta

### Largo Plazo
- [ ] Integración con servicios cloud (AWS Rekognition, Azure Face API)
- [ ] Soporte para dispositivos biométricos externos
- [ ] App móvil nativa (Android/iOS)
- [ ] Dashboard de administración de métodos biométricos

## 🐛 Solución de Problemas

### El QR no se genera
- Verificar que ZXing está en el classpath
- Revisar logs de consola en JavaFX

### La página móvil no carga
- Verificar que el backend está corriendo en puerto 8080
- Verificar conectividad de red entre móvil y PC
- Abrir URL manualmente en navegador móvil

### El desktop no detecta autenticación
- Verificar que el polling está activo (logs en consola)
- Verificar que no hay errores de red (firewall/antivirus)
- Revisar logs del backend para ver si llegó la petición POST

### WebAuthn no funciona
- Requiere HTTPS o localhost para WebAuthn
- Algunos navegadores móviles no soportan WebAuthn
- En ese caso, usar FaceID o Voz como alternativa

### MediaDevices no pide permisos
- Verificar que el navegador tiene permisos de cámara/micrófono
- Recargar la página y aceptar permisos
- Probar con otro navegador

## 📝 Notas Técnicas

### Polling vs WebSocket
Se eligió polling por simplicidad y compatibilidad:
- ✅ Funciona sin configuración adicional
- ✅ No requiere dependencias extra
- ✅ Más fácil de debuggear
- ❌ Menos eficiente (hace petición cada 2s)
- ❌ Mayor latencia (hasta 2s de espera)

Para producción, se recomienda migrar a WebSocket.

### Base64 para Datos Biométricos
Los datos se envían en base64 por:
- Compatibilidad con JSON
- Facilidad de transporte HTTP
- No requiere multipart/form-data
- Fácil de implementar en JavaScript

### Parser JSON Manual
En el desktop se usa parser manual en lugar de Jackson por:
- Evitar dependencias pesadas en JavaFX
- Mayor control sobre el parsing
- Mejor rendimiento para JSON simple
- Menor tamaño del JAR final

## 📚 Referencias

- [WebAuthn API](https://developer.mozilla.org/en-US/docs/Web/API/Web_Authentication_API)
- [face-api.js by vladmandic](https://github.com/vladmandic/face-api)
- [Meyda Audio Feature Extraction](https://meyda.js.org/)
- [MediaDevices.getUserMedia()](https://developer.mozilla.org/en-US/docs/Web/API/MediaDevices/getUserMedia)
- [MediaRecorder API](https://developer.mozilla.org/en-US/docs/Web/API/MediaRecorder)
- [CBOR RFC 8949](https://datatracker.ietf.org/doc/html/rfc8949)
- [ZXing Library](https://github.com/zxing/zxing)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [JavaFX](https://openjfx.io/)

---

**Implementado**: 10-14 de noviembre de 2025  
**Versión**: 3.0.0  
**Autor**: Sistema EduFeed

**Última actualización**: 14 de noviembre de 2025 - Documentación V3 con ML real
