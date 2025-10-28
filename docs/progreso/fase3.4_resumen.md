# Fase 3.4: Exception Handling y Documentación OpenAPI - Resumen de Implementación

**Fecha:** 2025-01-27  
**Estado:** ✅ Completado  
**Desarrollador:** Equipo EduFeed

---

## 📋 Objetivo

Mejorar la **gestión de errores** y la **documentación de la API** mediante:
- **OpenAPI/Swagger** personalizado con información completa de la API
- **GlobalExceptionHandler mejorado** con códigos de error estandarizados
- **Modelos de error consistentes** (ErrorResponse, ValidationErrorResponse)

---

## 🚀 Funcionalidades Implementadas

### 1. Configuración Personalizada de OpenAPI

**Archivo:** `OpenApiConfig.java`

**Características:**
- ✅ Información completa de la API (título, descripción, versión)
- ✅ Contacto del equipo y licencia MIT
- ✅ Múltiples servidores (desarrollo local + producción)
- ✅ Descripción de características principales
- ✅ Documentación de códigos de error HTTP

**Acceso a la documentación:**
- **Swagger UI:** http://localhost:8080/swagger-ui/index.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs
- **OpenAPI YAML:** http://localhost:8080/v3/api-docs.yaml

**Configuración:**
```java
@Bean
public OpenAPI customOpenAPI() {
    return new OpenAPI()
            .info(new Info()
                    .title("EduFeed API")
                    .description("""
                            API REST para el sistema de control de acceso con biometría EduFeed.
                            
                            ## Características principales:
                            - Gestión de usuarios con datos biométricos
                            - Control de acceso con verificación de derechos de uso
                            - Pagos y paquetes (DIARIO, MENSUAL, PAQUETE)
                            - Reportes administrativos
                            - Webhooks para integración con sistemas de caja
                            """)
                    .version("0.1.0-SNAPSHOT")
                    .contact(new Contact()
                            .name("Equipo EduFeed")
                            .email("soporte@edufeed.co")
                            .url("https://github.com/Joan-Mora/EduFeed"))
                    .license(new License()
                            .name("MIT License")
                            .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                    new Server()
                            .url("http://localhost:8080")
                            .description("Servidor de desarrollo local"),
                    new Server()
                            .url("https://api.edufeed.co")
                            .description("Servidor de producción")));
}
```

---

### 2. Manejo Global de Excepciones Mejorado

**Archivo:** `GlobalExceptionHandler.java` (actualizado)

**Mejoras implementadas:**
- ✅ **Códigos de error estandarizados** en todas las respuestas
- ✅ **ErrorResponse** con campos: `status`, `code`, `message`, `timestamp`
- ✅ **ValidationErrorResponse** con detalles por campo (`fieldErrors`)
- ✅ Mapeo inteligente de mensajes a códigos de error

#### Estructura de ErrorResponse

```json
{
  "status": 400,
  "code": "DUPLICATE_DOCUMENT",
  "message": "Ya existe un usuario con el documento 1234567890",
  "timestamp": "2025-01-27T14:30:00-05:00"
}
```

#### Estructura de ValidationErrorResponse

```json
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Error de validación en los campos de entrada",
  "timestamp": "2025-01-27T14:30:00-05:00",
  "fieldErrors": {
    "nombre": "no debe estar vacío",
    "documento": "debe tener entre 6 y 12 caracteres",
    "email": "debe ser una dirección de correo electrónico válida"
  }
}
```

---

### 3. Tabla de Códigos de Error

| Código HTTP | Código de Error | Descripción | Excepción |
|-------------|-----------------|-------------|-----------|
| **409** | `DUPLICATE_DOCUMENT` | Documento ya registrado | `DuplicateDocumentException` |
| **404** | `RESOURCE_NOT_FOUND` | Recurso no encontrado | `ResourceNotFoundException` |
| **400** | `INVALID_BUSINESS_RULE` | Regla de negocio violada | `InvalidBusinessRuleException` |
| **400** | `PAGO_YA_APROBADO` | Pago ya aprobado anteriormente | `InvalidPaymentException` |
| **400** | `PAGO_YA_RECHAZADO` | Pago ya rechazado anteriormente | `InvalidPaymentException` |
| **400** | `PAGO_PREVIAMENTE_RECHAZADO` | No se puede aprobar pago rechazado | `InvalidPaymentException` |
| **400** | `VIGENCIAS_INCOHERENTES` | vigente_hasta < vigente_desde | `InvalidVigenciaException` |
| **400** | `VIGENCIAS_FALTANTES` | Vigencias requeridas no definidas | `InvalidVigenciaException` |
| **400** | `INSUFFICIENT_PACKAGE_DAYS` | Paquete sin días restantes | `InsufficientPackageException` |
| **400** | `VALIDATION_ERROR` | Errores de validación @Valid | `MethodArgumentNotValidException` |
| **403** | `NO_VALID_ACCESS_RIGHT` | Sin derecho de uso vigente | `NoDerechoVigenteException` |
| **500** | `BIOMETRIC_ENROLLMENT_FAILED` | Fallo al enrolar biometría | `BiometricEnrollmentException` |
| **500** | `BIOMETRIC_VERIFICATION_FAILED` | Fallo al verificar biometría | `BiometricVerificationException` |
| **500** | `INTERNAL_SERVER_ERROR` | Error genérico del servidor | `Exception` |

---

## 🔧 Cambios en el Código

### Archivo Creado: `OpenApiConfig.java`

**Ubicación:** `co.cellano.edufeed.backend.config.OpenApiConfig`

**Configuración completa:**
```java
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:EduFeed}")
    private String applicationName;

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EduFeed API")
                        .description("""
                                API REST para el sistema de control de acceso con biometría EduFeed.
                                
                                ## Características principales:
                                - **Gestión de usuarios** con datos biométricos (huella, rostro, voz)
                                - **Control de acceso** con verificación de derechos de uso
                                - **Pagos y paquetes** (DIARIO, MENSUAL, PAQUETE)
                                - **Reportes administrativos** (asistencias, rechazos, ingresos, derechos activos)
                                - **Webhooks** para integración con sistemas de caja
                                
                                ## Autenticación:
                                - Actualmente sin autenticación (desarrollo)
                                - Producción: OAuth2/JWT (próximamente)
                                
                                ## Códigos de error:
                                - **400**: Datos inválidos o regla de negocio violada
                                - **404**: Recurso no encontrado
                                - **409**: Conflicto (ej: documento duplicado)
                                - **403**: Sin derecho de acceso vigente
                                - **500**: Error interno del servidor
                                """)
                        .version("0.1.0-SNAPSHOT")
                        .contact(new Contact()
                                .name("Equipo EduFeed")
                                .email("soporte@edufeed.co")
                                .url("https://github.com/Joan-Mora/EduFeed"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Servidor de desarrollo local"),
                        new Server()
                                .url("https://api.edufeed.co")
                                .description("Servidor de producción (próximamente)")));
    }
}
```

---

### Archivo Modificado: `GlobalExceptionHandler.java`

#### Clase ErrorResponse (actualizada)

**Antes:**
```java
public static class ErrorResponse {
    private int status;
    private String message;
    private OffsetDateTime timestamp;
    // ...
}
```

**Después:**
```java
public static class ErrorResponse {
    private int status;
    private String code;        // NUEVO
    private String message;
    private OffsetDateTime timestamp;
    // ...
}
```

#### Handlers de excepciones (mejorados)

**Ejemplo: DuplicateDocumentException**
```java
@ExceptionHandler(DuplicateDocumentException.class)
public ResponseEntity<ErrorResponse> handleDuplicateDocument(DuplicateDocumentException ex) {
    ErrorResponse error = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            "DUPLICATE_DOCUMENT",           // Código estandarizado
            ex.getMessage(),
            OffsetDateTime.now());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
}
```

**Ejemplo: InvalidPaymentException (con lógica de código)**
```java
@ExceptionHandler(InvalidPaymentException.class)
public ResponseEntity<ErrorResponse> handleInvalidPayment(InvalidPaymentException ex) {
    // Mapeo inteligente basado en el mensaje
    String errorCode = ex.getMessage().contains("YA_APROBADO") ? "PAGO_YA_APROBADO"
            : ex.getMessage().contains("YA_RECHAZADO") ? "PAGO_YA_RECHAZADO"
                    : ex.getMessage().contains("PREVIAMENTE_RECHAZADO") ? "PAGO_PREVIAMENTE_RECHAZADO"
                            : "INVALID_PAYMENT";
    ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            errorCode,
            ex.getMessage(),
            OffsetDateTime.now());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
}
```

**Ejemplo: ValidationErrorResponse**
```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ValidationErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach(error -> {
        String fieldName = ((FieldError) error).getField();
        String errorMessage = error.getDefaultMessage();
        errors.put(fieldName, errorMessage);
    });

    ValidationErrorResponse response = new ValidationErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "VALIDATION_ERROR",
            "Error de validación en los campos de entrada",
            OffsetDateTime.now(),
            errors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
}
```

---

## 📊 Ejemplos de Respuestas de Error

### Error 409: Documento Duplicado

**Request:**
```bash
POST /api/usuarios
{
  "documento": "1234567890",
  "nombre": "Juan Pérez",
  "email": "juan@example.com"
}
```

**Response:**
```json
{
  "status": 409,
  "code": "DUPLICATE_DOCUMENT",
  "message": "Ya existe un usuario con el documento 1234567890",
  "timestamp": "2025-01-27T14:30:00-05:00"
}
```

---

### Error 404: Usuario No Encontrado

**Request:**
```bash
GET /api/usuarios/a1b2c3d4-5678-90ab-cdef-1234567890ab
```

**Response:**
```json
{
  "status": 404,
  "code": "RESOURCE_NOT_FOUND",
  "message": "Usuario no encontrado con ID: a1b2c3d4-5678-90ab-cdef-1234567890ab",
  "timestamp": "2025-01-27T14:31:00-05:00"
}
```

---

### Error 400: Validación de Campos

**Request:**
```bash
POST /api/usuarios
{
  "documento": "123",
  "nombre": "",
  "email": "correo-invalido"
}
```

**Response:**
```json
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Error de validación en los campos de entrada",
  "timestamp": "2025-01-27T14:32:00-05:00",
  "fieldErrors": {
    "nombre": "no debe estar vacío",
    "documento": "debe tener entre 6 y 12 caracteres",
    "email": "debe ser una dirección de correo electrónico válida"
  }
}
```

---

### Error 400: Pago Ya Aprobado

**Request:**
```bash
PUT /api/pagos/a1b2c3d4-5678-90ab-cdef-1234567890ab/aprobar
```

**Response:**
```json
{
  "status": 400,
  "code": "PAGO_YA_APROBADO",
  "message": "El pago ya fue aprobado anteriormente",
  "timestamp": "2025-01-27T14:33:00-05:00"
}
```

---

### Error 403: Sin Derecho de Acceso

**Request:**
```bash
POST /api/accesos/verificar
{
  "usuarioId": "user-uuid",
  "modalidad": "HUELLA"
}
```

**Response:**
```json
{
  "status": 403,
  "code": "NO_VALID_ACCESS_RIGHT",
  "message": "Usuario sin derecho de uso vigente",
  "timestamp": "2025-01-27T14:34:00-05:00"
}
```

---

### Error 500: Fallo de Biometría

**Request:**
```bash
POST /api/usuarios/user-uuid/biometria/enrolar
{
  "modalidad": "HUELLA"
}
```

**Response:**
```json
{
  "status": 500,
  "code": "BIOMETRIC_ENROLLMENT_FAILED",
  "message": "Error al capturar plantilla biométrica: dispositivo no disponible",
  "timestamp": "2025-01-27T14:35:00-05:00"
}
```

---

## 🧪 Casos de Prueba Recomendados

### Test 1: ErrorResponse con Código Personalizado
```java
@Test
void duplicateDocumentDebeRetornarCodigoEspecifico() throws Exception {
    // Given: Usuario existente
    Usuario existente = crearUsuario("1234567890");
    
    // When: Intentar crear usuario duplicado
    mockMvc.perform(post("/api/usuarios")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "documento": "1234567890",
                    "nombre": "Juan Pérez"
                }
                """))
            // Then
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.code").value("DUPLICATE_DOCUMENT"))
            .andExpect(jsonPath("$.message").value(containsString("1234567890")))
            .andExpect(jsonPath("$.timestamp").exists());
}
```

### Test 2: ValidationErrorResponse con Detalles de Campos
```java
@Test
void validacionDebeRetornarErroresPorCampo() throws Exception {
    // When: Enviar datos inválidos
    mockMvc.perform(post("/api/usuarios")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "documento": "123",
                    "nombre": "",
                    "email": "correo-invalido"
                }
                """))
            // Then
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.fieldErrors.nombre").exists())
            .andExpect(jsonPath("$.fieldErrors.documento").exists())
            .andExpect(jsonPath("$.fieldErrors.email").exists());
}
```

### Test 3: Mapeo Inteligente de Códigos (InvalidPaymentException)
```java
@Test
void aprobarPagoYaAprobadoDebeRetornarCodigoCorrecto() throws Exception {
    // Given: Pago ya aprobado
    Pago pago = crearPagoAprobado();
    
    // When: Intentar aprobar nuevamente
    mockMvc.perform(put("/api/pagos/" + pago.getId() + "/aprobar"))
            // Then
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("PAGO_YA_APROBADO"))
            .andExpect(jsonPath("$.message").value(containsString("ya fue aprobado")));
}
```

### Test 4: OpenAPI JSON Disponible
```java
@Test
void openApiJsonDebeEstarDisponible() throws Exception {
    mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.info.title").value("EduFeed API"))
            .andExpect(jsonPath("$.info.version").value("0.1.0-SNAPSHOT"))
            .andExpect(jsonPath("$.servers[0].url").value("http://localhost:8080"));
}
```

### Test 5: Swagger UI Accesible
```java
@Test
void swaggerUIDebeEstarAccesible() throws Exception {
    mockMvc.perform(get("/swagger-ui/index.html"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.TEXT_HTML));
}
```

---

## 📈 Métricas de Implementación

| Métrica | Valor |
|---------|-------|
| **Archivos creados** | 1 (`OpenApiConfig.java`) |
| **Archivos modificados** | 1 (`GlobalExceptionHandler.java`) |
| **Handlers de excepciones** | 11 (todos mejorados con códigos) |
| **Códigos de error definidos** | 14 códigos únicos |
| **Clases de error** | 2 (`ErrorResponse`, `ValidationErrorResponse`) |
| **Líneas de código añadidas** | ~180 |
| **Tests recomendados** | 5 (validación de estructura de error y OpenAPI) |

---

## ✅ Verificación de Compilación

```bash
cd "c:\Users\Julia\OneDrive\Documentos\GitHub\EduFeed"
$env:JAVA_HOME='C:/Program Files/Java/jdk-24'
& "$env:USERPROFILE\tools\maven\apache-maven-3.9.9\bin\mvn.cmd" clean compile -DskipTests
```

**Resultado:** ✅ `BUILD SUCCESS` - 91 archivos compilados sin errores

---

## 🔗 Acceso a la Documentación

### Desarrollo Local

- **Swagger UI Interactivo:** http://localhost:8080/swagger-ui/index.html
  - Permite probar endpoints directamente desde el navegador
  - Documentación generada automáticamente desde anotaciones OpenAPI
  - Ejemplos de request/response
  
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs
  - Especificación completa en formato JSON
  - Útil para importar en Postman/Insomnia
  
- **OpenAPI YAML:** http://localhost:8080/v3/api-docs.yaml
  - Formato YAML (más legible para humanos)

### Producción

Una vez desplegado en producción:
- **Swagger UI:** https://api.edufeed.co/swagger-ui/index.html
- **OpenAPI JSON:** https://api.edufeed.co/v3/api-docs

---

## 📝 Notas Técnicas

### Personalización de Mensajes de Error

Los mensajes de error se pueden personalizar usando anotaciones de validación:

```java
public class UsuarioDto {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    
    @Size(min = 6, max = 12, message = "El documento debe tener entre 6 y 12 caracteres")
    private String documento;
    
    @Email(message = "Debe proporcionar un correo electrónico válido")
    private String email;
}
```

### Anotaciones OpenAPI en Controladores

Los controladores ya usan anotaciones `@Operation`, `@ApiResponse`, `@Parameter` para mejorar la documentación:

```java
@PostMapping("/verificar")
@Operation(summary = "Verificar derecho de acceso", 
           description = "Verifica si un usuario tiene derecho vigente para acceder")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Verificación exitosa"),
    @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
})
public ResponseEntity<AccesoCheckResponse> verificarAcceso(@Valid @RequestBody AccesoCheckRequest request) {
    // ...
}
```

### Mapeo Inteligente de Códigos

El `GlobalExceptionHandler` usa lógica condicional para mapear mensajes a códigos específicos:

```java
String errorCode = ex.getMessage().contains("YA_APROBADO") ? "PAGO_YA_APROBADO"
        : ex.getMessage().contains("YA_RECHAZADO") ? "PAGO_YA_RECHAZADO"
                : "INVALID_PAYMENT";
```

**Ventaja:** No necesita modificar las clases de excepción existentes.

**Alternativa futura:** Añadir campo `code` a las excepciones personalizadas.

### Deshabilitación de Swagger en Producción

Si se desea deshabilitar Swagger en producción, añadir en `application.properties`:

```properties
# application-prod.properties
springdoc.swagger-ui.enabled=false
springdoc.api-docs.enabled=false
```

---

## 🚀 Próximos Pasos

### Mejoras Futuras

1. **Autenticación en Swagger:**
   ```java
   @SecurityRequirement(name = "bearerAuth")
   @SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
   ```

2. **Ejemplos de Request/Response:**
   ```java
   @Operation(
       requestBody = @RequestBody(
           content = @Content(
               examples = @ExampleObject(value = """
                   {
                       "documento": "1234567890",
                       "nombre": "Juan Pérez"
                   }
                   """)
           )
       )
   )
   ```

3. **Agrupación de Endpoints:**
   - Usar `@Tag` en controladores para agrupar endpoints en Swagger UI
   - Ya implementado en `ReportController`, `AccesoController`, etc.

4. **Rate Limiting:**
   - Añadir `@RateLimitException` para manejar límites de peticiones
   - Código de error: `TOO_MANY_REQUESTS` (HTTP 429)

5. **Internacionalización de Mensajes:**
   - Usar `MessageSource` para mensajes en múltiples idiomas
   - Detectar idioma desde header `Accept-Language`

---

## 📚 Referencias

- **Código fuente:**
  - `OpenApiConfig.java`: Configuración OpenAPI
  - `GlobalExceptionHandler.java`: Manejo global de excepciones
  - `ErrorResponse.java` (inner class): Modelo de error estándar
  - `ValidationErrorResponse.java` (inner class): Modelo de error de validación

- **Documentación relacionada:**
  - [SpringDoc OpenAPI](https://springdoc.org/)
  - [OpenAPI Specification](https://swagger.io/specification/)
  - [Spring @RestControllerAdvice](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-exceptionhandler)

- **Documentación del proyecto:**
  - [Fase 3.1: API de Usuarios](./fase3.1_resumen.md)
  - [Fase 3.2: API de Pagos](./fase3.2_resumen.md)
  - [Fase 3.3: Reportes Adicionales](./fase3.3_resumen.md)
  - [Architecture.md](../architecture.md)

---

**🎉 Fase 3.4 completada con éxito**

**Próxima acción recomendada:**  
- Implementar **Fase 3.5 (Endpoints WebAuthn)** para huella por teléfono, o  
- Comenzar **Fase 5 (Integración de biometría real)** con OpenCV para rostro

**URLs útiles:**
- 📄 **Swagger UI:** http://localhost:8080/swagger-ui/index.html
- 📋 **OpenAPI JSON:** http://localhost:8080/v3/api-docs
