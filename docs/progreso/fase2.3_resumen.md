# FASE 2.3: Servicio de Control de Acceso

**Periodo:** Semana 7  
**Estado:** ✅ COMPLETADO  
**Fecha de finalización:** 20 de octubre de 2025

---

## 📋 Índice

1. [Objetivos](#objetivos)
2. [Entregables](#entregables)
3. [Excepciones Implementadas](#excepciones-implementadas)
4. [AccesoService](#accesoservice)
5. [Lógica de Verificación de Derechos](#lógica-de-verificación-de-derechos)
6. [API REST Endpoints](#api-rest-endpoints)
7. [Integración con Paquetes](#integración-con-paquetes)
8. [Ejemplos de Uso](#ejemplos-de-uso)
9. [Tests](#tests)
10. [Siguiente Fase](#siguiente-fase)

---

## 🎯 Objetivos

### Completados:
- ✅ Implementar AccesoService con verificación de derechos en tiempo real
- ✅ Registrar accesos (APROBADO/DENEGADO) con motivo y modalidad
- ✅ Consumir días de paquete automáticamente si aplica
- ✅ Generar respuesta con instrucciones para usuario denegado (RF-04)
- ✅ Crear API REST para verificación de acceso e historial
- ✅ Extender repositorios con consultas custom y soporte para Specification

---

## 📦 Entregables

### **Archivos a crear/modificar:**

```
edufeed-backend/src/main/java/co/cellano/edufeed/backend/
├── exception/
│   ├── NoDerechoVigenteException.java              [NUEVO]
│   └── PaqueteAgotadoException.java                [EXTENDIDO de Fase 2.2]
├── service/
│   └── AccesoService.java                          [NUEVO]
├── controller/
│   └── AccesoController.java                       [NUEVO]
├── dto/
│   ├── request/
│   │   └── AccesoCheckRequest.java                 [NUEVO]
│   └── response/
│       ├── AccesoCheckResponse.java                [NUEVO]
│       └── OrientacionCajaResponse.java            [NUEVO]
└── test/
    └── service/
        └── AccesoServiceTest.java                  [NUEVO]
```

---

## 🚨 Excepciones Implementadas

### **1. NoDerechoVigenteException**

**Propósito:** Lanzada cuando un usuario no tiene derecho vigente para acceder

**Código HTTP:** 403 Forbidden

**Atributos:**
- `usuarioId` (UUID)
- `documento` (String)
- `motivoDenegacion` (String)

**Ejemplo:**
```java
throw new NoDerechoVigenteException(
    usuarioId,
    "1234567890",
    "SIN_DERECHO_VIGENTE"
);
```

**Respuesta JSON:**
```json
{
  "status": 403,
  "message": "Usuario con documento 1234567890 no tiene derecho vigente",
  "motivo": "SIN_DERECHO_VIGENTE",
  "timestamp": "2025-10-20T14:30:00-05:00"
}
```

---

### **2. PaqueteAgotadoException** (extendida)

**Propósito:** Lanzada cuando un paquete no tiene días disponibles

**Código HTTP:** 400 Bad Request

**Atributos:**
- `paqueteId` (UUID)
- `diasRestantes` (Integer)
- `message` (String)

**Ejemplo:**
```java
throw new PaqueteAgotadoException(
    paqueteId,
    0,
    "PAQUETE_AGOTADO"
);
```

---

## 🛡️ AccesoService

### **Características principales:**

#### **1. Verificación de Derecho Vigente**

**Método:** `verificarAcceso(UUID usuarioId, Modalidad modalidad)`

**Flujo:**
1. Validar que usuario existe y está activo
2. Buscar derecho vigente para el usuario en el momento actual
3. Si hay derecho vigente:
   - Crear registro de acceso APROBADO
   - Si es PAQUETE, verificar días disponibles y decrementar
   - Registrar modalidad biométrica utilizada
   - Retornar respuesta positiva
4. Si NO hay derecho vigente:
   - Crear registro de acceso DENEGADO
   - Determinar motivo de denegación (SIN_DERECHO, PAQUETE_AGOTADO, USUARIO_INACTIVO)
   - Generar instrucciones de orientación a caja
   - Retornar respuesta con motivo e instrucciones

**Pseudocódigo:**
```java
public AccesoCheckResponse verificarAcceso(UUID usuarioId, Modalidad modalidad) {
    // 1. Validar usuario
    Usuario usuario = usuarioRepository.findById(usuarioId)
        .orElseThrow(() -> new ResourceNotFoundException("Usuario", usuarioId));
    
    if (!usuario.getActivo()) {
        return denegarAcceso(usuario, "USUARIO_INACTIVO", modalidad);
    }
    
    // 2. Buscar derecho vigente
    OffsetDateTime ahora = OffsetDateTime.now(ZoneId.of("America/Bogota"));
    Optional<DerechoUso> derechoOpt = derechoUsoRepository.findDerechoVigente(usuarioId, ahora);
    
    if (derechoOpt.isEmpty()) {
        return denegarAcceso(usuario, "SIN_DERECHO_VIGENTE", modalidad);
    }
    
    DerechoUso derecho = derechoOpt.get();
    
    // 3. Si es paquete, verificar y consumir día
    if (derecho.getTipoDerecho() == TipoPago.PAQUETE) {
        PaquetePago paquete = paquetePagoService.buscarPorPago(derecho.getPago().getId());
        
        if (paquete.getDiasRestantes() <= 0) {
            return denegarAcceso(usuario, "PAQUETE_AGOTADO", modalidad);
        }
        
        // Consumir día del paquete
        paquetePagoService.consumirDia(paquete.getId(), usuario.getId());
    }
    
    // 4. Registrar acceso aprobado
    Acceso acceso = new Acceso();
    acceso.setUsuario(usuario);
    acceso.setDerecho(derecho);
    acceso.setEstado(EstadoAcceso.APROBADO);
    acceso.setModalidad(modalidad);
    acceso.setCreadoEn(ahora);
    accesoRepository.save(acceso);
    
    // 5. Retornar respuesta positiva
    return AccesoCheckResponse.builder()
        .permitido(true)
        .usuario(usuarioMapper.toDto(usuario))
        .derecho(derechoUsoMapper.toDto(derecho))
        .modalidad(modalidad)
        .timestamp(ahora)
        .build();
}
```

---

#### **2. Denegación de Acceso con Orientación**

**Método:** `denegarAcceso(Usuario usuario, String motivo, Modalidad modalidad)`

**Flujo:**
1. Registrar acceso DENEGADO en BD
2. Generar instrucciones personalizadas según motivo
3. Incluir información de orientación a caja (ubicación, referencia, código QR)

**Ejemplo de implementación:**
```java
private AccesoCheckResponse denegarAcceso(Usuario usuario, String motivo, Modalidad modalidad) {
    OffsetDateTime ahora = OffsetDateTime.now(ZoneId.of("America/Bogota"));
    
    // Registrar acceso denegado
    Acceso acceso = new Acceso();
    acceso.setUsuario(usuario);
    acceso.setEstado(EstadoAcceso.DENEGADO);
    acceso.setModalidad(modalidad);
    acceso.setMotivo(motivo);
    acceso.setCreadoEn(ahora);
    accesoRepository.save(acceso);
    
    // Generar orientación a caja
    OrientacionCajaResponse orientacion = generarOrientacionCaja(usuario, motivo);
    
    // Retornar respuesta negativa
    return AccesoCheckResponse.builder()
        .permitido(false)
        .usuario(usuarioMapper.toDto(usuario))
        .motivo(motivo)
        .modalidad(modalidad)
        .orientacionCaja(orientacion)
        .timestamp(ahora)
        .build();
}
```

---

#### **3. Generación de Orientación a Caja (RF-04)**

**Método:** `generarOrientacionCaja(Usuario usuario, String motivo)`

**Contenido de la orientación:**
- Mensaje personalizado según motivo
- Ubicación de la caja (texto + mapa)
- Referencia única para el usuario (documento)
- Código QR con información del usuario
- Horario de atención de caja

**Ejemplo:**
```java
private OrientacionCajaResponse generarOrientacionCaja(Usuario usuario, String motivo) {
    String mensaje;
    
    switch (motivo) {
        case "SIN_DERECHO_VIGENTE":
            mensaje = "No tiene un pago activo. Por favor diríjase a caja para realizar el pago.";
            break;
        case "PAQUETE_AGOTADO":
            mensaje = "Su paquete de días se ha agotado. Por favor diríjase a caja para renovar.";
            break;
        case "USUARIO_INACTIVO":
            mensaje = "Su usuario está inactivo. Por favor diríjase a administración.";
            break;
        default:
            mensaje = "Por favor diríjase a caja para más información.";
    }
    
    return OrientacionCajaResponse.builder()
        .mensaje(mensaje)
        .ubicacionCaja("Planta baja, entrada principal, lado derecho")
        .horarioAtencion("Lunes a Viernes: 7:00 AM - 5:00 PM")
        .referencia(usuario.getDocumento())
        .codigoQR(generarCodigoQR(usuario))
        .build();
}

private String generarCodigoQR(Usuario usuario) {
    // Generar URL o texto para QR
    // Ejemplo: "EDUFEED:USER:{documento}:TIMESTAMP:{timestamp}"
    return String.format("EDUFEED:USER:%s:TIMESTAMP:%d",
        usuario.getDocumento(),
        System.currentTimeMillis()
    );
}
```

---

#### **4. Historial de Accesos**

**Método:** `obtenerHistorial(UUID usuarioId, OffsetDateTime inicio, OffsetDateTime fin, EstadoAcceso estado)`

**Características:**
- Filtrado por usuario, rango de fechas y estado
- Paginación con Pageable
- Ordenamiento por fecha descendente (más recientes primero)

**Ejemplo:**
```java
public Page<AccesoDto> obtenerHistorial(
    UUID usuarioId,
    OffsetDateTime inicio,
    OffsetDateTime fin,
    EstadoAcceso estado,
    Pageable pageable
) {
    // Construir query dinámica con Specification
    Specification<Acceso> spec = (root, query, cb) -> {
        List<Predicate> predicates = new ArrayList<>();
        
        if (usuarioId != null) {
            predicates.add(cb.equal(root.get("usuario").get("id"), usuarioId));
        }
        
        if (inicio != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("creadoEn"), inicio));
        }
        
        if (fin != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("creadoEn"), fin));
        }
        
        if (estado != null) {
            predicates.add(cb.equal(root.get("estado"), estado));
        }
        
        return cb.and(predicates.toArray(new Predicate[0]));
    };
    
    Page<Acceso> accesos = accesoRepository.findAll(spec, pageable);
    return accesos.map(accesoMapper::toDto);
}
```

---

## 🔗 Integración con Paquetes

### **Consumo automático de días**

**Flujo:**
1. Verificar que derecho es de tipo PAQUETE
2. Buscar paquete asociado al pago
3. Validar que tiene días disponibles (dias_restantes > 0)
4. Decrementar dias_restantes
5. Crear registro en usos_paquete (auditoría de consumos)

**Validaciones:**
- No permitir valores negativos en dias_restantes
- Transacción atómica (rollback si falla registro de acceso)
- Concurrencia: usar locking optimista (@Version en PaquetePago)

**Ejemplo de PaquetePagoService.consumirDia():**
```java
@Transactional
public void consumirDia(UUID paqueteId, UUID usuarioId) {
    PaquetePago paquete = paquetePagoRepository.findById(paqueteId)
        .orElseThrow(() -> new ResourceNotFoundException("PaquetePago", paqueteId));
    
    if (paquete.getDiasRestantes() <= 0) {
        throw new PaqueteAgotadoException(paqueteId, 0, "PAQUETE_AGOTADO");
    }
    
    // Decrementar días
    paquete.setDiasRestantes(paquete.getDiasRestantes() - 1);
    paquetePagoRepository.save(paquete);
    
    // Registrar uso
    UsoPaquete uso = new UsoPaquete();
    uso.setPaquete(paquete);
    uso.setUsuarioId(usuarioId);
    uso.setFechaUso(OffsetDateTime.now(ZoneId.of("America/Bogota")));
    usoPaqueteRepository.save(uso);
}
```

---

## 🌐 API REST Endpoints

### **1. POST /api/accesos/verificar**

**Propósito:** Verificar derecho de acceso y registrar intento

**Request:**
```json
{
  "usuarioId": "550e8400-e29b-41d4-a716-446655440000",
  "modalidad": "HUELLA"
}
```

**Response (APROBADO):**
```json
{
  "permitido": true,
  "usuario": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "documento": "1234567890",
    "nombreCompleto": "Juan Pérez",
    "tipoUsuario": "ESTUDIANTE"
  },
  "derecho": {
    "id": "650e8400-e29b-41d4-a716-446655440000",
    "tipoDerecho": "DIARIO",
    "vigenteDesde": "2025-10-20T00:00:00-05:00",
    "vigenteHasta": "2025-10-20T23:59:59.999999999-05:00"
  },
  "modalidad": "HUELLA",
  "timestamp": "2025-10-20T14:30:15-05:00"
}
```

**Response (DENEGADO):**
```json
{
  "permitido": false,
  "usuario": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "documento": "1234567890",
    "nombreCompleto": "Juan Pérez",
    "tipoUsuario": "ESTUDIANTE"
  },
  "motivo": "SIN_DERECHO_VIGENTE",
  "modalidad": "HUELLA",
  "orientacionCaja": {
    "mensaje": "No tiene un pago activo. Por favor diríjase a caja para realizar el pago.",
    "ubicacionCaja": "Planta baja, entrada principal, lado derecho",
    "horarioAtencion": "Lunes a Viernes: 7:00 AM - 5:00 PM",
    "referencia": "1234567890",
    "codigoQR": "EDUFEED:USER:1234567890:TIMESTAMP:1729446615000"
  },
  "timestamp": "2025-10-20T14:30:15-05:00"
}
```

**Códigos HTTP:**
- `200 OK` - Verificación exitosa (permitido o denegado)
- `404 Not Found` - Usuario no encontrado
- `500 Internal Server Error` - Error del servidor

---

### **2. GET /api/accesos/historial**

**Propósito:** Consultar historial de accesos con filtros

**Query Parameters:**
- `usuarioId` (UUID, opcional) - Filtrar por usuario
- `inicio` (OffsetDateTime, opcional) - Fecha/hora de inicio
- `fin` (OffsetDateTime, opcional) - Fecha/hora de fin
- `estado` (EstadoAcceso, opcional) - APROBADO o DENEGADO
- `page` (int, default: 0) - Número de página
- `size` (int, default: 20) - Tamaño de página
- `sort` (string, default: "creadoEn,desc") - Ordenamiento

**Request:**
```
GET /api/accesos/historial?usuarioId=550e8400-e29b-41d4-a716-446655440000&estado=APROBADO&page=0&size=10
```

**Response:**
```json
{
  "content": [
    {
      "id": "750e8400-e29b-41d4-a716-446655440000",
      "usuario": {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "documento": "1234567890",
        "nombreCompleto": "Juan Pérez"
      },
      "estado": "APROBADO",
      "modalidad": "HUELLA",
      "creadoEn": "2025-10-20T08:15:00-05:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "offset": 0
  },
  "totalElements": 45,
  "totalPages": 5,
  "last": false,
  "first": true
}
```

---

### **3. GET /api/accesos/estadisticas**

**Propósito:** Obtener estadísticas de accesos (opcional)

**Query Parameters:**
- `inicio` (OffsetDateTime, requerido)
- `fin` (OffsetDateTime, requerido)

**Response:**
```json
{
  "periodo": {
    "inicio": "2025-10-01T00:00:00-05:00",
    "fin": "2025-10-20T23:59:59-05:00"
  },
  "totalAccesos": 1250,
  "aprobados": 1180,
  "denegados": 70,
  "porModalidad": {
    "HUELLA": 850,
    "ROSTRO": 300,
    "VOZ": 100
  },
  "motivosDenegacion": {
    "SIN_DERECHO_VIGENTE": 50,
    "PAQUETE_AGOTADO": 15,
    "USUARIO_INACTIVO": 5
  }
}
```

---

## 🧪 Tests

### **AccesoServiceTest**

**Casos de prueba:**

#### **1. testVerificarAcceso_ConDerechoDiarioVigente_Aprobado**
```java
@Test
void testVerificarAcceso_ConDerechoDiarioVigente_Aprobado() {
    // Arrange
    Usuario usuario = crearUsuarioConDerechoDiario();
    
    // Act
    AccesoCheckResponse response = accesoService.verificarAcceso(
        usuario.getId(),
        Modalidad.HUELLA
    );
    
    // Assert
    assertTrue(response.getPermitido());
    assertNotNull(response.getDerecho());
    assertEquals(Modalidad.HUELLA, response.getModalidad());
    
    // Verificar que se registró en BD
    List<Acceso> accesos = accesoRepository.findByUsuarioId(usuario.getId());
    assertEquals(1, accesos.size());
    assertEquals(EstadoAcceso.APROBADO, accesos.get(0).getEstado());
}
```

---

#### **2. testVerificarAcceso_SinDerecho_Denegado**
```java
@Test
void testVerificarAcceso_SinDerecho_Denegado() {
    // Arrange
    Usuario usuario = crearUsuarioSinDerecho();
    
    // Act
    AccesoCheckResponse response = accesoService.verificarAcceso(
        usuario.getId(),
        Modalidad.ROSTRO
    );
    
    // Assert
    assertFalse(response.getPermitido());
    assertEquals("SIN_DERECHO_VIGENTE", response.getMotivo());
    assertNotNull(response.getOrientacionCaja());
    assertTrue(response.getOrientacionCaja().getMensaje().contains("diríjase a caja"));
    
    // Verificar registro en BD
    List<Acceso> accesos = accesoRepository.findByUsuarioId(usuario.getId());
    assertEquals(1, accesos.size());
    assertEquals(EstadoAcceso.DENEGADO, accesos.get(0).getEstado());
}
```

---

#### **3. testVerificarAcceso_ConPaquete_ConsumeDia**
```java
@Test
void testVerificarAcceso_ConPaquete_ConsumeDia() {
    // Arrange
    Usuario usuario = crearUsuarioConPaquete(5); // 5 días disponibles
    PaquetePago paquete = obtenerPaqueteUsuario(usuario);
    
    // Act
    AccesoCheckResponse response = accesoService.verificarAcceso(
        usuario.getId(),
        Modalidad.HUELLA
    );
    
    // Assert
    assertTrue(response.getPermitido());
    
    // Verificar que se consumió un día
    PaquetePago paqueteActualizado = paquetePagoRepository.findById(paquete.getId()).get();
    assertEquals(4, paqueteActualizado.getDiasRestantes());
    
    // Verificar registro en usos_paquete
    List<UsoPaquete> usos = usoPaqueteRepository.findByPaqueteId(paquete.getId());
    assertEquals(1, usos.size());
}
```

---

#### **4. testVerificarAcceso_PaqueteAgotado_Denegado**
```java
@Test
void testVerificarAcceso_PaqueteAgotado_Denegado() {
    // Arrange
    Usuario usuario = crearUsuarioConPaquete(0); // 0 días disponibles
    
    // Act
    AccesoCheckResponse response = accesoService.verificarAcceso(
        usuario.getId(),
        Modalidad.VOZ
    );
    
    // Assert
    assertFalse(response.getPermitido());
    assertEquals("PAQUETE_AGOTADO", response.getMotivo());
    assertTrue(response.getOrientacionCaja().getMensaje().contains("agotado"));
}
```

---

#### **5. testObtenerHistorial_ConFiltros**
```java
@Test
void testObtenerHistorial_ConFiltros() {
    // Arrange
    Usuario usuario = crearUsuarioConHistorial(); // 10 accesos registrados
    OffsetDateTime inicio = OffsetDateTime.now(ZoneId.of("America/Bogota")).minusDays(7);
    OffsetDateTime fin = OffsetDateTime.now(ZoneId.of("America/Bogota"));
    
    // Act
    Page<AccesoDto> historial = accesoService.obtenerHistorial(
        usuario.getId(),
        inicio,
        fin,
        EstadoAcceso.APROBADO,
        PageRequest.of(0, 5, Sort.by("creadoEn").descending())
    );
    
    // Assert
    assertNotNull(historial);
    assertEquals(5, historial.getContent().size());
    assertTrue(historial.getTotalElements() >= 5);
    
    // Verificar ordenamiento (más reciente primero)
    OffsetDateTime anterior = null;
    for (AccesoDto acceso : historial.getContent()) {
        if (anterior != null) {
            assertTrue(acceso.getCreadoEn().isBefore(anterior) || 
                       acceso.getCreadoEn().isEqual(anterior));
        }
        anterior = acceso.getCreadoEn();
    }
}
```

---

## ✅ Criterios de Aceptación

### **Funcionales:**
- ✅ Usuario con derecho vigente → acceso APROBADO
- ✅ Usuario sin derecho → acceso DENEGADO con motivo "SIN_DERECHO_VIGENTE"
- ✅ Usuario con paquete agotado → acceso DENEGADO con motivo "PAQUETE_AGOTADO"
- ✅ Registro en tabla `accesos` con derecho_id vinculado (si aplica)
- ✅ Decremento de dias_restantes en paquete cuando se consume día (delegado a PaquetePagoService)
- ✅ Orientación a caja con mensaje personalizado, ubicación, horario y código QR
- ✅ Historial de accesos con paginación y filtros funcionales

### **Técnicos:**
- ✅ Código compilando sin errores
- ✅ Transacciones atómicas con @Transactional
- ✅ Validación de entrada en DTOs con Bean Validation
- ✅ Documentación OpenAPI/Swagger de endpoints
- ✅ Logs informativos y de error apropiados
- ⏳ Tests unitarios con cobertura ≥80% (pendiente)

---

## 🔄 Siguiente Fase

### **FASE 3: API REST y Controladores**

**Objetivos:**
- Completar API REST para usuarios (RF-01, RF-07)
- Implementar API REST completa de pagos (RF-05, RF-12)
- Crear API de reportes (RF-06, RF-10, RF-13)
- Implementar API de integración con caja (RF-08)
- Añadir manejo centralizado de excepciones (@ControllerAdvice)
- Documentar todos los endpoints en Swagger UI

**Estimación:** 2-3 semanas

---

## 📊 Métricas Objetivo

| Métrica | Objetivo | Estado |
|---------|----------|--------|
| Tiempo de desarrollo | 1 semana | ✅ 1 día |
| Tests unitarios | ≥5 casos | ⏳ Pendiente |
| Cobertura de tests | ≥80% | ⏳ Pendiente |
| Endpoints REST | 3 | ✅ 3 |
| Archivos creados/modificados | ~10 | ✅ 9 |

---

## 📝 Archivos Creados/Modificados

### **Archivos nuevos:**
1. `NoDerechoVigenteException.java` - Excepción para usuarios sin derecho vigente
2. `AccesoCheckRequest.java` - DTO de request para verificación de acceso
3. `AccesoCheckResponse.java` - DTO de response con resultado de verificación
4. `OrientacionCajaResponse.java` - DTO con información de orientación a caja
5. `AccesoService.java` - Servicio principal de control de acceso
6. `AccesoController.java` - Controlador REST con endpoints de acceso

### **Archivos modificados:**
1. `GlobalExceptionHandler.java` - Añadido handler para NoDerechoVigenteException → 403
2. `DerechoUsoRepository.java` - Añadido método findDerechoVigente() con @Query
3. `AccesoRepository.java` - Extendido con JpaSpecificationExecutor

---

**Fecha de actualización:** 20 de octubre de 2025  
**Responsable:** Equipo EduFeed  
**Estado del proyecto:** ✅ FASE 2.3 COMPLETADA - Pendiente tests unitarios
**Próxima fase:** FASE 3 - API REST y Controladores
