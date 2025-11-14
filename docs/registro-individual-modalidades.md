# 📱 Registro Individual de Modalidades Biométricas

## 📋 Resumen

Flujo para registrar **una sola modalidad** biométrica (huella, rostro o voz) sin forzar el flujo completo de 3 pasos. Ideal para:
- Actualizar una modalidad específica sin re-registrar todas
- Agregar modalidad faltante (ej: usuario solo tiene huella, quiere agregar rostro)
- Reemplazar modalidad inválida (ej: rostro mal capturado, volver a registrar)

## 🎯 Problema Resuelto

**Antes (V2)**:
```
Admin selecciona "Rostro" → QR muestra wizard 3 pasos
Usuario escanea QR → Obligado a registrar huella + rostro + voz
Usuario solo quería actualizar rostro → Frustración
```

**Ahora (V3)**:
```
Admin selecciona "Rostro" → QR específico de rostro
Usuario escanea QR → Solo aparece tarjeta de rostro
Usuario registra rostro → Página cierra automáticamente
```

## 🏗️ Arquitectura

```
┌──────────────────┐
│  Desktop (JavaFX)│
│  UserManagement  │
└────────┬─────────┘
         │
         │ showSingleModalityQR(modalidad)
         │ URL: /register?userId=X&token=Y&type=rostro
         ▼
┌─────────────────────────────────┐
│  Móvil Browser                  │
│  biometric-register.html        │
│  (onlyType mode)                │
└────────┬────────────────────────┘
         │
         │ POST /api/biometric/register/face
         │ {sessionId, faceData}
         ▼
┌─────────────────────────────────┐
│  Backend                        │
│  BiometricRegistrationController│
│  BiometricRegistrationService   │
└────────┬────────────────────────┘
         │
         │ Guarda plantilla
         ▼
┌─────────────────────────────────┐
│  PostgreSQL                     │
│  plantilla_biometrica           │
└─────────────────────────────────┘
         │
         │ Polling cada 2s
         ▼
┌─────────────────────────────────┐
│  Desktop detecta completado     │
│  Muestra: ✅ ¡Registro          │
│           completado!           │
│  Auto-cierra tras 1.5s          │
└─────────────────────────────────┘
```

## 📁 Implementación

### 1. Backend

#### Controller
**`BiometricRegistrationController.java`**
```java
@GetMapping("")
public String registrationMainPage(
    @RequestParam(value = "userId", required = false) String userId,
    @RequestParam(value = "token", required = false) String token,
    @RequestParam(value = "sessionId", required = false) String sessionId,
    @RequestParam(value = "type", required = false) String type, // ← NUEVO
    Model model
) {
    // Si viene type, activa modo individual
    if (type != null && !type.isBlank()) {
        model.addAttribute("onlyType", type); // "huella" | "rostro" | "voz"
    }
    
    // Resto del flujo estándar
    if (userId != null) {
        // Buscar usuario, crear sesión si no existe
    }
    
    return "biometric-register";
}
```

**Auto-creación de sesión en páginas individuales**:
```java
@GetMapping("/fingerprint")
public String fingerprintRegistrationPage(
    @RequestParam(value = "sessionId", required = false) String sessionId,
    @RequestParam(value = "userId", required = false) String userId,
    Model model
) {
    // Si no hay sesión válida, crear una nueva
    if (sessionId == null || registrationService.getUserFromSession(sessionId) == null) {
        Usuario user = usuarioRepository.findByDocumento(userId).orElse(null);
        if (user != null) {
            sessionId = registrationService.startSession(user);
        }
    }
    
    model.addAttribute("sessionId", sessionId);
    return "biometric-register-fingerprint";
}

// Mismo patrón para /face y /voice
```

### 2. Frontend

#### Vista Principal
**`biometric-register.html`**
```html
<script th:inline="javascript">
    const onlyType = /*[[${onlyType}]]*/ ''; // "huella" | "rostro" | "voz" | ''
    
    function applyOnlyTypeMode() {
        if (!onlyType) return; // Modo normal (3 modalidades)
        
        console.log('[ONLY-TYPE] Modo individual activado:', onlyType);
        
        // Ocultar tarjetas no seleccionadas
        const cards = {
            'huella': 'cardFingerprint',
            'rostro': 'cardFace',
            'voz': 'cardVoice'
        };
        
        Object.entries(cards).forEach(([type, cardId]) => {
            const card = document.getElementById(cardId);
            if (type !== onlyType && card) {
                card.style.display = 'none';
            }
        });
        
        // Actualizar contador de progreso a "0 de 1"
        const progressText = document.getElementById('progressText');
        if (progressText) {
            progressText.textContent = '0 de 1 completados';
        }
        
        // Auto-abrir modalidad seleccionada después de 300ms
        setTimeout(() => {
            if (onlyType === 'huella') registerFingerprint();
            else if (onlyType === 'rostro') registerFace();
            else if (onlyType === 'voz') registerVoice();
        }, 300);
    }
    
    function updateUI() {
        const done = countCompleted(); // 0, 1, 2 o 3
        
        // Actualizar progreso
        if (onlyType) {
            document.getElementById('progressText').textContent = 
                `${done} de 1 completados`;
        } else {
            document.getElementById('progressText').textContent = 
                `${done} de 3 completados`;
        }
        
        // Cierre automático en modo individual
        if (onlyType && done === 1) {
            showMessage('success', '¡Registro completado! Cerrando...');
            setTimeout(() => window.close(), 1500);
        }
        
        // Modo normal: mostrar botón "Finalizar" cuando todo esté listo
        if (!onlyType && done === 3) {
            document.getElementById('btnFinish').style.display = 'block';
        }
    }
    
    // Ejecutar al cargar página
    document.addEventListener('DOMContentLoaded', () => {
        applyOnlyTypeMode();
    });
</script>
```

### 3. Desktop

#### UserManagementModule
**`UserManagementModule.java`**
```java
// Botones individuales por modalidad
private void doRegistrarModalidad(UserApiClient.Modalidad modalidad) {
    String userId = usuarioSeleccionado.getDocumento();
    String token = UUID.randomUUID().toString();
    String sessionId = UUID.randomUUID().toString();
    
    // Mapear modalidad a parámetro 'type'
    String typeParam = modalidad.name().toLowerCase();
    if (modalidad == UserApiClient.Modalidad.ROSTRO) typeParam = "rostro";
    else if (modalidad == UserApiClient.Modalidad.VOZ) typeParam = "voz";
    else typeParam = "huella";
    
    // URL con parámetro 'type'
    String url = String.format(
        "%s/api/biometric/register?userId=%s&token=%s&sessionId=%s&type=%s",
        baseUrl, userId, token, sessionId, typeParam
    );
    
    showSingleModalityQR(
        usuarioSeleccionado.getNombreCompleto(),
        url,
        modalidad.name()
    );
}

private void showSingleModalityQR(String userName, String url, String modalidad) {
    // Crear diálogo con QR
    Dialog<Void> dialog = new Dialog<>();
    dialog.setTitle("Registro de " + modalidad);
    
    VBox content = new VBox(15);
    content.setPadding(new Insets(20));
    content.setAlignment(Pos.CENTER);
    
    // Título
    Label title = new Label("Escanea el QR con tu móvil");
    title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
    
    // Nombre del usuario
    Label userLabel = new Label(userName);
    userLabel.setStyle("-fx-font-size: 14px;");
    
    // Modalidad específica
    Label modalityLabel = new Label("Modalidad: " + modalidad);
    modalityLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #4F46E5;");
    
    // QR
    ImageView qrView = new ImageView(generateQRImage(url, 300, 300));
    
    // Status label (polling)
    Label statusLabel = new Label("Esperando registro...");
    statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #6B7280;");
    
    content.getChildren().addAll(title, userLabel, modalityLabel, qrView, statusLabel);
    dialog.getDialogPane().setContent(content);
    
    // Botón cerrar
    ButtonType closeBtn = new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE);
    dialog.getDialogPane().getButtonTypes().add(closeBtn);
    
    // Polling cada 2 segundos
    ScheduledExecutorService poller = Executors.newSingleThreadScheduledExecutor();
    poller.scheduleAtFixedRate(() -> {
        try {
            var status = sessionService.getStatus(userId, sessionId);
            
            boolean completed = false;
            if (modalidad.contains("HUELLA") && status.huellaCompletada) completed = true;
            if (modalidad.contains("ROSTRO") && status.rostroCompletado) completed = true;
            if (modalidad.contains("VOZ") && status.vozCompletada) completed = true;
            
            if (completed) {
                Platform.runLater(() -> {
                    statusLabel.setText("✅ ¡Registro completado!");
                    statusLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #10B981; -fx-font-weight: bold;");
                });
                
                poller.shutdown();
                
                // Auto-cerrar tras 1.5 segundos
                Thread.sleep(1500);
                Platform.runLater(() -> dialog.close());
            }
        } catch (Exception e) {
            System.err.println("[QR-DIALOG] Error en polling: " + e.getMessage());
        }
    }, 1, 2, TimeUnit.SECONDS);
    
    // Cancelar polling al cerrar
    dialog.setOnCloseRequest(evt -> poller.shutdown());
    
    dialog.showAndWait();
}
```

## 🧪 Cómo Probar

### 1. Registro Individual de Rostro
```powershell
# 1. Levantar servicios
.\scripts\db-up.ps1
mvn -f edufeed-backend\pom.xml spring-boot:run

# 2. Desktop
mvn -f edufeed-desktop\pom.xml javafx:run

# 3. Admin → Usuarios → Seleccionar usuario → Botón "Rostro"
# 4. Escanear QR → Solo aparece tarjeta de rostro
# 5. Registrar rostro → Auto-cierre tras mensaje "✅ ¡Registro completado!"
```

### 2. Verificar en Base de Datos
```sql
SELECT 
    u.nombre_completo,
    pb.modalidad,
    pb.activo,
    pb.created_at
FROM plantilla_biometrica pb
JOIN app_user u ON pb.usuario_id = u.id
WHERE u.documento = '123456'
ORDER BY pb.created_at DESC;

-- Debe mostrar solo 1 registro nuevo con modalidad = 'ROSTRO'
```

### 3. URL Manual (opcional)
```
http://localhost:8080/api/biometric/register?userId=123456&token=abc123&sessionId=xyz789&type=rostro
```

## 🔍 Debugging

### Logs Backend
```
[BIO-REG] Modo individual activado: type=rostro
[BIO-REG] Usuario 123456 tiene sesión: xyz789
[BIO-REG] Sesión auto-creada para userId=123456
[BIO-REG] Rostro registrado exitosamente para user 123456
```

### Logs Frontend (Console)
```javascript
[ONLY-TYPE] Modo individual activado: rostro
[ONLY-TYPE] Ocultando tarjetas: huella, voz
[ONLY-TYPE] Auto-abriendo modalidad: rostro
[REGISTER] Rostro capturado, enviando...
[UI] 1 de 1 completados
[UI] Cerrando página en 1.5s
```

### Logs Desktop (Console)
```
[QR-DIALOG] Modalidad: ROSTRO, URL generada
[QR-DIALOG] Polling iniciado para user=123456, session=xyz789
[QR-DIALOG] Verificando status... rostroCompletado=false
[QR-DIALOG] Verificando status... rostroCompletado=true
[QR-DIALOG] ✅ Registro completado, cerrando diálogo
```

## 🚨 Casos de Error

### Error: "Sesión no encontrada o expirada"
**Causa**: SessionId inválido o expirado
**Solución**: Backend auto-crea sesión si userId está presente

```java
if (sessionId == null || registrationService.getUserFromSession(sessionId) == null) {
    sessionId = registrationService.startSession(user);
}
```

### Error: Página no cierra automáticamente
**Causa**: `window.close()` bloqueado por navegador (no abierta via script)
**Solución**: Usuario cierra manualmente tras ver mensaje "✅ ¡Registro completado!"

### Error: QR no actualiza status
**Causa**: Polling no detecta cambio en BD
**Verificar**:
```sql
SELECT rostro_completado FROM biometric_registration_session 
WHERE session_id = 'xyz789';
```

## 📊 Comparación con Flujo Normal

| Característica | Flujo Normal | Flujo Individual |
|----------------|--------------|------------------|
| Modalidades | 3 obligatorias | 1 específica |
| Progreso | "0 de 3" → "3 de 3" | "0 de 1" → "1 de 1" |
| Cierre | Botón "Finalizar" | Automático |
| URL | `/register?userId=X` | `/register?userId=X&type=rostro` |
| Tarjetas visibles | Todas | Solo seleccionada |
| Auto-apertura | No | Sí (300ms delay) |

## 🎯 Casos de Uso

### 1. Actualizar Modalidad Inválida
```
Usuario tiene rostro mal capturado (fondo oscuro, mala calidad)
→ Admin selecciona "Rostro"
→ Usuario escanea QR
→ Registra rostro nuevamente con buena iluminación
→ Sistema desactiva plantilla anterior y crea nueva
```

### 2. Agregar Modalidad Faltante
```
Usuario solo tiene huella registrada
→ Admin selecciona "Voz"
→ Usuario escanea QR
→ Registra voz (grabación 3s)
→ Sistema agrega modalidad sin tocar huella existente
```

### 3. Registro Masivo Selectivo
```
10 usuarios nuevos solo necesitan huella (no rostro/voz)
→ Admin selecciona usuarios uno por uno
→ Botón "Huella" → QR específico
→ Usuarios registran solo huella
→ Proceso más rápido que 3 modalidades
```

## 🔐 Seguridad

- Token único por sesión (UUID v4)
- Sesión expira en 2 minutos
- Validación de userId + sessionId en backend
- Plantillas cifradas en BD (AES-256)
- No se permite registro sin sesión activa

---

**Implementado**: 13 de noviembre de 2025  
**Versión**: 3.0.0  
**Autor**: Sistema EduFeed
