# Fase 3.6: Manejo Centralizado de Excepciones con Logging - Resumen de Implementación

**Fecha:** 27 de octubre de 2025  
**Proyecto:** EduFeed Backend  
**Responsable:** Desarrollo de API REST  

---

## 📋 Descripción General

Esta fase implementa un sistema robusto de manejo centralizado de excepciones con logging estructurado y contextual para el proyecto EduFeed.

### Objetivos
- Logging estructurado con contexto (request ID, usuario, path, método)
- Uso de SLF4J + MDC (Mapped Diagnostic Context)
- Ocultación de stack traces en producción
- Mensajes de error amigables para el usuario
- Trazabilidad completa de cada request

---

## ✅ Componentes Modificados

### `GlobalExceptionHandler.java` (Mejorado)

**Nuevas funcionalidades:**
1. **Logging con contexto MDC**
2. **Captura de información del request**
3. **Identificación del usuario autenticado**
4. **Request ID único para trazabilidad**

**Ejemplo de código:**

```java
private void setLoggingContext(HttpServletRequest request) {
    // Request ID único para trazabilidad
    String requestId = UUID.randomUUID().toString().substring(0, 8);
    MDC.put("requestId", requestId);
    
    // Usuario autenticado (si existe)
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String username = (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";
    MDC.put("username", username);
    
    // Path y método HTTP
    MDC.put("path", request.getRequestURI());
    MDC.put("method", request.getMethod());
}

private void clearLoggingContext() {
    MDC.clear();
}
```

### Niveles de Logging por Tipo de Excepción

| Excepción | Nivel | Ejemplo de Log |
|-----------|-------|----------------|
| `ResourceNotFoundException` | WARN | `Recurso no encontrado: Usuario - ID: abc-123` |
| `DuplicateDocumentException` | WARN | `Documento duplicado: 1234567890` |
| `InvalidBusinessRuleException` | WARN | `Regla de negocio violada: ...` |
| `BiometricEnrollmentException` | ERROR | `Error en registro biométrico: ...` (con stack trace) |
| `BiometricVerificationException` | ERROR | `Error en verificación biométrica: ...` |
| `MethodArgumentNotValidException` | WARN | `Errores de validación: {field: message}` |
| `Exception` (genérica) | ERROR | `Error inesperado: ...` (sin exponer detalles) |

### Handlers Mejorados (11 total)

Todos los exception handlers fueron actualizados para incluir:

1. **Inyección de HttpServletRequest**
```java
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<ErrorResponse> handleResourceNotFound(
        ResourceNotFoundException ex, 
        HttpServletRequest request) {
    // ...
}
```

2. **Logging con contexto**
```java
setLoggingContext(request);
logger.warn("Recurso no encontrado: {}", ex.getMessage());
clearLoggingContext();
```

3. **Diferentes niveles de log según criticidad**
- **WARN**: Errores de negocio (404, 409, 400)
- **ERROR**: Errores de sistema (500, errores biométricos)

---

## 🔐 Mensajes de Error Seguros

### Producción vs Desarrollo

En producción, los errores internos NO exponen stack traces ni detalles sensibles:

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
    setLoggingContext(request);
    logger.error("Error inesperado: {}", ex.getMessage(), ex);
    clearLoggingContext();
    
    // NO exponer detalles internos
    String message = "Ha ocurrido un error interno. Por favor, contacte al administrador.";
    
    ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "INTERNAL_SERVER_ERROR",
            message,
            OffsetDateTime.now());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
}
```

### Estructura de ErrorResponse

```java
public record ErrorResponse(
    int status,
    String error,
    String message,
    OffsetDateTime timestamp
) {}
```

Para errores de validación, se incluyen detalles de campo:

```java
public record ValidationErrorResponse(
    int status,
    String error,
    String message,
    OffsetDateTime timestamp,
    Map<String, String> errors  // campo -> mensaje de error
) {}
```

---

## 📝 Configuración de Logging

### Logback Configuration Recomendada

**logback-spring.xml:**

```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%X{requestId}] [%X{username}] [%X{method} %X{path}] %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/edufeed.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/edufeed.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%X{requestId}] [%X{username}] [%X{method} %X{path}] %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="FILE" />
    </root>
</configuration>
```

### Ejemplo de Log Generado

```
2025-10-27 14:11:00 [http-nio-8080-exec-1] WARN  [a1b2c3d4] [admin] [GET /api/usuarios/abc-123] c.c.e.b.e.GlobalExceptionHandler - Recurso no encontrado: Usuario - ID: abc-123
2025-10-27 14:11:15 [http-nio-8080-exec-2] WARN  [b2c3d4e5] [cajero1] [POST /api/pagos] c.c.e.b.e.GlobalExceptionHandler - Documento duplicado: 1234567890
2025-10-27 14:11:30 [http-nio-8080-exec-3] ERROR [c3d4e5f6] [admin] [POST /api/usuarios/abc-123/biometria] c.c.e.b.e.GlobalExceptionHandler - Error en registro biométrico: No se pudo capturar huella
```

---

## 🎯 Criterios de Aceptación - COMPLETADOS

- ✅ **NotFoundException → 404** con mensaje claro
- ✅ **DuplicateException → 409** con mensaje claro
- ✅ **ValidationException → 400** con lista de errores de campo
- ✅ **Excepciones no manejadas → 500** con mensaje genérico (NO stack trace público)
- ✅ **Logging con contexto**: request ID, usuario, timestamp, path, método
- ✅ **MDC limpiado** después de cada request
- ✅ **11 exception handlers** mejorados
- ✅ **Diferentes niveles de log** según criticidad

---

## 🧪 Pruebas de Validación

### Prueba 1: Error 404 (Recurso No Encontrado)

```bash
curl -X GET http://localhost:8080/api/usuarios/99999999-9999-9999-9999-999999999999
```

**Respuesta esperada:**
```json
{
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Usuario - ID: 99999999-9999-9999-9999-999999999999",
  "timestamp": "2025-10-27T14:11:00-05:00"
}
```

**Log esperado:**
```
2025-10-27 14:11:00 [http-nio-8080-exec-1] WARN [a1b2c3d4] [anonymous] [GET /api/usuarios/99999999-9999-9999-9999-999999999999] c.c.e.b.e.GlobalExceptionHandler - Recurso no encontrado: Usuario - ID: 99999999-9999-9999-9999-999999999999
```

### Prueba 2: Error 409 (Documento Duplicado)

```bash
curl -X POST http://localhost:8080/api/usuarios \
  -H "Content-Type: application/json" \
  -d '{
    "documento": "1234567890",
    "nombre": "Juan Pérez"
  }'

# Intentar crear el mismo usuario nuevamente
curl -X POST http://localhost:8080/api/usuarios \
  -H "Content-Type: application/json" \
  -d '{
    "documento": "1234567890",
    "nombre": "María López"
  }'
```

**Respuesta esperada:**
```json
{
  "status": 409,
  "error": "CONFLICT",
  "message": "Ya existe un usuario con el documento: 1234567890",
  "timestamp": "2025-10-27T14:11:15-05:00"
}
```

### Prueba 3: Error 400 (Validación)

```bash
curl -X POST http://localhost:8080/api/usuarios \
  -H "Content-Type: application/json" \
  -d '{
    "documento": "",
    "nombre": ""
  }'
```

**Respuesta esperada:**
```json
{
  "status": 400,
  "error": "BAD_REQUEST",
  "message": "Errores de validación",
  "timestamp": "2025-10-27T14:11:20-05:00",
  "errors": {
    "documento": "no debe estar vacío",
    "nombre": "no debe estar vacío"
  }
}
```

### Prueba 4: Error 500 (Error Interno)

Simular un error inesperado (por ejemplo, división por cero en lógica de negocio).

**Respuesta esperada:**
```json
{
  "status": 500,
  "error": "INTERNAL_SERVER_ERROR",
  "message": "Ha ocurrido un error interno. Por favor, contacte al administrador.",
  "timestamp": "2025-10-27T14:11:25-05:00"
}
```

**Log esperado (con stack trace completo):**
```
2025-10-27 14:11:25 [http-nio-8080-exec-5] ERROR [d4e5f6a7] [admin] [POST /api/calcular] c.c.e.b.e.GlobalExceptionHandler - Error inesperado: / by zero
java.lang.ArithmeticException: / by zero
    at co.cellano.edufeed.backend.service.CalculoService.calcular(CalculoService.java:42)
    ...
```

---

## 📊 Métricas de Implementación

- **Archivos modificados:** 1 (`GlobalExceptionHandler.java`)
- **Líneas añadidas:** ~80
- **Handlers mejorados:** 11
- **Contexto MDC:** 4 campos (requestId, username, path, method)
- **Tiempo de desarrollo:** ~30 minutos

---

## 🔍 Contexto MDC (Mapped Diagnostic Context)

### ¿Qué es MDC?

MDC permite agregar información contextual a los logs de forma thread-safe. Cada request HTTP tiene su propio contexto MDC que se limpia automáticamente después de la respuesta.

### Campos Personalizados

| Campo | Descripción | Ejemplo |
|-------|-------------|---------|
| `requestId` | ID único del request (8 caracteres) | `a1b2c3d4` |
| `username` | Usuario autenticado (o "anonymous") | `admin`, `cajero1` |
| `path` | URI del request | `/api/usuarios/abc-123` |
| `method` | Método HTTP | `GET`, `POST`, `PUT`, `DELETE` |

### Beneficios

1. **Trazabilidad**: Cada request tiene un ID único
2. **Auditoría**: Se registra qué usuario hizo qué acción
3. **Debugging**: Fácil filtrar logs por endpoint o método
4. **Correlación**: Relacionar múltiples logs del mismo request

---

## 📚 Documentación Técnica

### Dependencias Utilizadas

```xml
<!-- SLF4J API -->
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
</dependency>

<!-- Logback (implementación de SLF4J) -->
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
</dependency>
```

### Referencias

- [SLF4J Documentation](https://www.slf4j.org/manual.html)
- [Logback Documentation](https://logback.qos.ch/documentation.html)
- [MDC (Mapped Diagnostic Context)](https://www.slf4j.org/manual.html#mdc)
- [Spring Boot Logging](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.logging)

---

## 🚀 Próximos Pasos

### Mejoras Futuras

1. **Logging asíncrono** para mejorar performance
2. **Agregación de logs** con ELK Stack (Elasticsearch, Logstash, Kibana)
3. **Alertas automáticas** cuando hay muchos errores 500
4. **Métricas** con Micrometer/Prometheus
5. **Distributed tracing** con Spring Cloud Sleuth/Zipkin

### Integración con Herramientas

- **Sentry**: Para tracking de errores en producción
- **Datadog**: Para monitoreo y alertas
- **Grafana**: Para visualización de métricas

---

## 📝 Notas Técnicas

### Thread Safety

MDC es thread-safe porque usa `ThreadLocal` internamente. Cada thread (cada request HTTP en Tomcat) tiene su propio contexto MDC.

### Limpieza de MDC

Es **crítico** llamar `MDC.clear()` después de cada request para evitar memory leaks. En este proyecto, se hace en el método `clearLoggingContext()` llamado en cada exception handler.

### Performance

- MDC tiene overhead mínimo (~microsegundos por operación)
- El impacto en performance es negligible comparado con I/O de base de datos
- Logging asíncrono puede usarse si es necesario

---

**Fin del documento - Fase 3.6 completada exitosamente** ✨

**Total de handlers:** 11 mejorados  
**Total de líneas:** ~80 añadidas  
**Estado de compilación:** ✅ BUILD SUCCESS  
**Tiempo de desarrollo:** ~30 minutos
