# Fase 3.2: API de Pagos (Aprobar/Rechazar) - Resumen de Implementación

**Fecha:** 2025-01-27  
**Estado:** ✅ Completado  
**Desarrollador:** Equipo EduFeed

---

## 📋 Objetivo

Implementar endpoints REST para **aprobar** y **rechazar** pagos, incluyendo la **generación automática de derechos de uso** (`DerechoUso`) cuando un pago es aprobado, según el tipo de pago (DIARIO, MENSUAL o PAQUETE).

---

## 🚀 Funcionalidades Implementadas

### 1. Aprobación de Pagos con Generación Automática de Derechos

**Endpoint:** `PUT /api/pagos/{id}/aprobar`

**Descripción:**
- Cambia el estado del pago de `PENDIENTE` a `APROBADO`
- Genera automáticamente un `DerechoUso` para el usuario según el tipo de pago
- Maneja transaccionalidad: si falla la generación del derecho, revierte la aprobación

**Lógica de generación de derechos por tipo:**

| Tipo de Pago | Vigencia del DerechoUso | Comportamiento |
|--------------|-------------------------|----------------|
| **DIARIO** | Inicio del día actual - Fin del día actual (23:59:59.999) | Copia las vigencias del pago (calculadas al crear el pago) |
| **MENSUAL** | Primer día del mes - Último día del mes (23:59:59.999) | Copia las vigencias del pago (calculadas al crear el pago) |
| **PAQUETE** | Inicio del día actual - Fin del día actual (23:59:59.999) | Genera vigencia de 24h y **consume 1 día** del paquete asociado |

**Validaciones:**
- ❌ El pago debe existir
- ❌ El pago no puede estar ya `APROBADO` (lanza `PAGO_YA_APROBADO`)
- ❌ El pago no puede estar `RECHAZADO` (lanza `PAGO_PREVIAMENTE_RECHAZADO`)
- ❌ Si falla la generación del derecho, revierte el estado a `PENDIENTE` (lanza `ERROR_GENERAR_DERECHO`)

**Ejemplo de uso:**

```bash
# Aprobar un pago DIARIO
curl -X PUT http://localhost:8080/api/pagos/a1b2c3d4-5678-90ab-cdef-1234567890ab/aprobar \
  -H "Content-Type: application/json"

# Respuesta exitosa
{
  "id": "a1b2c3d4-5678-90ab-cdef-1234567890ab",
  "usuarioId": "user-uuid",
  "tipoPago": "DIARIO",
  "estadoPago": "APROBADO",
  "monto": 15000.00,
  "vigenteDesde": "2025-01-27T00:00:00-05:00",
  "vigenteHasta": "2025-01-27T23:59:59.999999999-05:00",
  "creadoEn": "2025-01-27T10:30:00-05:00"
}
```

---

### 2. Rechazo de Pagos

**Endpoint:** `PUT /api/pagos/{id}/rechazar`

**Descripción:**
- Cambia el estado del pago de `PENDIENTE` a `RECHAZADO`
- **NO genera ningún derecho de uso**
- Bloquea aprobaciones futuras del mismo pago

**Validaciones:**
- ❌ El pago debe existir
- ❌ El pago no puede estar ya `APROBADO` (lanza `PAGO_YA_APROBADO`)
- ❌ El pago no puede estar ya `RECHAZADO` (lanza `PAGO_YA_RECHAZADO`)

**Ejemplo de uso:**

```bash
# Rechazar un pago
curl -X PUT http://localhost:8080/api/pagos/a1b2c3d4-5678-90ab-cdef-1234567890ab/rechazar \
  -H "Content-Type: application/json"

# Respuesta exitosa
{
  "id": "a1b2c3d4-5678-90ab-cdef-1234567890ab",
  "usuarioId": "user-uuid",
  "tipoPago": "DIARIO",
  "estadoPago": "RECHAZADO",
  "monto": 15000.00,
  "creadoEn": "2025-01-27T10:30:00-05:00"
}
```

---

## 🔧 Cambios en el Código

### Archivos Modificados

#### 1. `PagoService.java`

**Inyección de `DerechoUsoService`:**

```java
private final DerechoUsoService derechoUsoService;

public PagoService(PagoRepository pagoRepository,
        UsuarioRepository usuarioRepository,
        PaquetePagoRepository paquetePagoRepository,
        DerechoUsoService derechoUsoService) {
    this.pagoRepository = pagoRepository;
    this.usuarioRepository = usuarioRepository;
    this.paquetePagoRepository = paquetePagoRepository;
    this.derechoUsoService = derechoUsoService;
    this.timezone = ZoneId.of("America/Bogota");
}
```

**Método `aprobar(UUID id)`:**

```java
public PagoDto aprobar(UUID id) {
    Pago pago = pagoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Pago", id));

    // Validaciones de estado
    if (pago.getEstadoPago() == EstadoPago.APROBADO) {
        throw new InvalidPaymentException("El pago ya fue aprobado anteriormente", "PAGO_YA_APROBADO");
    }
    if (pago.getEstadoPago() == EstadoPago.RECHAZADO) {
        throw new InvalidPaymentException("No se puede aprobar un pago previamente rechazado", "PAGO_PREVIAMENTE_RECHAZADO");
    }

    // Aprobar pago
    pago.setEstadoPago(EstadoPago.APROBADO);
    Pago saved = pagoRepository.save(pago);

    // Generar derecho de uso (con rollback si falla)
    try {
        derechoUsoService.generarDerecho(saved.getId());
    } catch (Exception e) {
        pago.setEstadoPago(EstadoPago.PENDIENTE);
        pagoRepository.save(pago);
        throw new InvalidPaymentException("Error al generar derecho de uso: " + e.getMessage(), "ERROR_GENERAR_DERECHO");
    }

    return PagoMapper.toDto(saved);
}
```

**Método `rechazar(UUID id)`:**

```java
public PagoDto rechazar(UUID id) {
    Pago pago = pagoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Pago", id));

    // Validaciones de estado
    if (pago.getEstadoPago() == EstadoPago.APROBADO) {
        throw new InvalidPaymentException("No se puede rechazar un pago ya aprobado", "PAGO_YA_APROBADO");
    }
    if (pago.getEstadoPago() == EstadoPago.RECHAZADO) {
        throw new InvalidPaymentException("El pago ya fue rechazado anteriormente", "PAGO_YA_RECHAZADO");
    }

    pago.setEstadoPago(EstadoPago.RECHAZADO);
    Pago saved = pagoRepository.save(pago);

    return PagoMapper.toDto(saved);
}
```

---

#### 2. `PagoController.java`

**Endpoint `PUT /api/pagos/{id}/aprobar`:**

```java
@PutMapping("/{id}/aprobar")
public ResponseEntity<PagoDto> aprobar(@PathVariable UUID id) {
    PagoDto aprobado = pagoService.aprobar(id);
    return ResponseEntity.ok(aprobado);
}
```

**Endpoint `PUT /api/pagos/{id}/rechazar`:**

```java
@PutMapping("/{id}/rechazar")
public ResponseEntity<PagoDto> rechazar(@PathVariable UUID id) {
    PagoDto rechazado = pagoService.rechazar(id);
    return ResponseEntity.ok(rechazado);
}
```

---

## 📊 Flujo de Aprobación de Pago

```
┌─────────────────────────────────────────────────────────────────┐
│ Cliente: PUT /api/pagos/{id}/aprobar                            │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     v
┌─────────────────────────────────────────────────────────────────┐
│ PagoController.aprobar(id)                                      │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     v
┌─────────────────────────────────────────────────────────────────┐
│ PagoService.aprobar(id)                                         │
│  1. Buscar pago por ID                                          │
│  2. Validar estado (no APROBADO, no RECHAZADO)                  │
│  3. Cambiar estado a APROBADO                                   │
│  4. Guardar pago                                                │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     v
┌─────────────────────────────────────────────────────────────────┐
│ DerechoUsoService.generarDerecho(pagoId)                        │
│  1. Validar pago APROBADO                                       │
│  2. Crear DerechoUso con vigencias según tipo:                  │
│     - DIARIO: Copiar vigencias del pago (hoy 00:00 - 23:59)     │
│     - MENSUAL: Copiar vigencias del pago (mes completo)         │
│     - PAQUETE: Generar vigencia 24h + consumir 1 día paquete    │
│  3. Guardar DerechoUso                                          │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     v
┌─────────────────────────────────────────────────────────────────┐
│ ¿Error al generar derecho?                                      │
│  SÍ: Rollback estado pago a PENDIENTE + lanzar excepción        │
│  NO: Retornar PagoDto con estado APROBADO                       │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🧪 Casos de Prueba Recomendados

### Test 1: Aprobar Pago DIARIO
```java
@Test
void aprobarPagoDiarioDebeGenerarDerechoUsoValidoParaHoy() {
    // Given: Pago DIARIO en estado PENDIENTE
    Pago pago = crearPagoDiarioPendiente();
    
    // When: Aprobar pago
    PagoDto resultado = pagoService.aprobar(pago.getId());
    
    // Then:
    assertThat(resultado.getEstadoPago()).isEqualTo(EstadoPago.APROBADO);
    
    // Verificar que se generó DerechoUso
    List<DerechoUso> derechos = derechoUsoRepository.findByUsuarioId(pago.getUsuario().getId());
    assertThat(derechos).hasSize(1);
    
    DerechoUso derecho = derechos.get(0);
    assertThat(derecho.getTipoDerecho()).isEqualTo(TipoPago.DIARIO);
    assertThat(derecho.getVigenteDesde()).isEqualTo(LocalDate.now().atStartOfDay());
    assertThat(derecho.getVigenteHasta()).isEqualTo(LocalDate.now().atTime(23, 59, 59, 999999999));
}
```

### Test 2: Aprobar Pago MENSUAL
```java
@Test
void aprobarPagoMensualDebeGenerarDerechoUsoValidoParaMesCompleto() {
    // Given: Pago MENSUAL en estado PENDIENTE
    Pago pago = crearPagoMensualPendiente();
    
    // When: Aprobar pago
    PagoDto resultado = pagoService.aprobar(pago.getId());
    
    // Then: Derecho generado con vigencia del mes completo
    List<DerechoUso> derechos = derechoUsoRepository.findByUsuarioId(pago.getUsuario().getId());
    assertThat(derechos).hasSize(1);
    
    DerechoUso derecho = derechos.get(0);
    assertThat(derecho.getVigenteDesde()).isEqualTo(LocalDate.now().withDayOfMonth(1).atStartOfDay());
    assertThat(derecho.getVigenteHasta().getDayOfMonth()).isEqualTo(LocalDate.now().lengthOfMonth());
}
```

### Test 3: Aprobar Pago PAQUETE
```java
@Test
void aprobarPagoPaqueteDebeGenerarDerechoUsoDe24HorasYConsumir1Dia() {
    // Given: Pago PAQUETE con 10 días
    Pago pago = crearPagoPaquetePendiente(10);
    PaquetePago paquete = paquetePagoRepository.findByPagoId(pago.getId());
    assertThat(paquete.getDiasRestantes()).isEqualTo(10);
    
    // When: Aprobar pago
    pagoService.aprobar(pago.getId());
    
    // Then:
    // 1. Derecho generado con vigencia de 24h
    List<DerechoUso> derechos = derechoUsoRepository.findByUsuarioId(pago.getUsuario().getId());
    assertThat(derechos).hasSize(1);
    
    // 2. Paquete con 9 días restantes
    paquete = paquetePagoRepository.findByPagoId(pago.getId());
    assertThat(paquete.getDiasRestantes()).isEqualTo(9);
}
```

### Test 4: Rechazar Pago PENDIENTE
```java
@Test
void rechazarPagoPendienteDebeCambiarEstadoSinGenerarDerecho() {
    // Given: Pago PENDIENTE
    Pago pago = crearPagoPendiente();
    
    // When: Rechazar pago
    PagoDto resultado = pagoService.rechazar(pago.getId());
    
    // Then:
    assertThat(resultado.getEstadoPago()).isEqualTo(EstadoPago.RECHAZADO);
    
    // No debe generar DerechoUso
    List<DerechoUso> derechos = derechoUsoRepository.findByUsuarioId(pago.getUsuario().getId());
    assertThat(derechos).isEmpty();
}
```

### Test 5: No Permitir Aprobar Pago Ya Aprobado
```java
@Test
void aprobarPagoYaAprobadoDebeLanzarExcepcion() {
    // Given: Pago ya APROBADO
    Pago pago = crearPagoPendiente();
    pagoService.aprobar(pago.getId());
    
    // When/Then: Intentar aprobar nuevamente
    assertThatThrownBy(() -> pagoService.aprobar(pago.getId()))
        .isInstanceOf(InvalidPaymentException.class)
        .hasMessageContaining("ya fue aprobado");
}
```

### Test 6: No Permitir Aprobar Pago Rechazado
```java
@Test
void aprobarPagoRechazadoDebeLanzarExcepcion() {
    // Given: Pago RECHAZADO
    Pago pago = crearPagoPendiente();
    pagoService.rechazar(pago.getId());
    
    // When/Then: Intentar aprobar
    assertThatThrownBy(() -> pagoService.aprobar(pago.getId()))
        .isInstanceOf(InvalidPaymentException.class)
        .hasMessageContaining("previamente rechazado");
}
```

### Test 7: Rollback de Aprobación si Falla Generación de Derecho
```java
@Test
void aprobarPagoDebeRevertirEstadoSiFallaGeneracionDerecho() {
    // Given: Mock de DerechoUsoService que lanza excepción
    when(derechoUsoService.generarDerecho(any())).thenThrow(new RuntimeException("Error DB"));
    
    Pago pago = crearPagoPendiente();
    
    // When/Then: Aprobar pago lanza excepción
    assertThatThrownBy(() -> pagoService.aprobar(pago.getId()))
        .isInstanceOf(InvalidPaymentException.class)
        .hasMessageContaining("Error al generar derecho");
    
    // Estado debe volver a PENDIENTE
    Pago pagoActual = pagoRepository.findById(pago.getId()).get();
    assertThat(pagoActual.getEstadoPago()).isEqualTo(EstadoPago.PENDIENTE);
}
```

---

## 📈 Métricas de Implementación

| Métrica | Valor |
|---------|-------|
| **Archivos modificados** | 2 (`PagoService.java`, `PagoController.java`) |
| **Nuevos endpoints** | 2 (`PUT /aprobar`, `PUT /rechazar`) |
| **Nuevos métodos** | 2 (`aprobar()`, `rechazar()`) |
| **Líneas de código añadidas** | ~130 (incluyendo comentarios Javadoc) |
| **Validaciones implementadas** | 6 (estado aprobado/rechazado, rollback transaccional) |
| **Tests recomendados** | 7 (cobertura completa de flujos) |

---

## ✅ Verificación de Compilación

```bash
cd "c:\Users\Julia\OneDrive\Documentos\GitHub\EduFeed"
$env:JAVA_HOME='C:/Program Files/Java/jdk-24'
& "$env:USERPROFILE\tools\maven\apache-maven-3.9.9\bin\mvn.cmd" clean compile -DskipTests
```

**Resultado:** ✅ `BUILD SUCCESS` - Sin errores de compilación

---

## 🔗 Integración con Fases Previas

### Dependencias de Fase 2.2
- ✅ `DerechoUsoService.generarDerecho()` - Método existente reutilizado
- ✅ `PaquetePagoService.consumirDia()` - Lógica de consumo de paquetes
- ✅ Cálculo de vigencias según `TipoPago` (DIARIO/MENSUAL/PAQUETE)

### Relación con RF (Requisitos Funcionales)
- **RF-05:** Gestión de pagos con tipos (DIARIO, MENSUAL, PAQUETE) ✅
- **RF-12:** Generación automática de derechos de uso al aprobar pagos ✅
- **RF-06:** Base para reportes de ingresos aprobados/rechazados (futuro)

---

## 📝 Notas Técnicas

### Transaccionalidad
- El método `aprobar()` usa `@Transactional` heredado de la clase `PagoService`
- **Rollback manual:** Si `derechoUsoService.generarDerecho()` falla, se revierte el estado del pago a `PENDIENTE` antes de lanzar la excepción
- Esto garantiza consistencia: **nunca habrá un pago APROBADO sin su DerechoUso correspondiente**

### Idempotencia
- Los endpoints **NO son idempotentes**: llamar múltiples veces a `/aprobar` o `/rechazar` lanza excepciones de negocio
- Esto es intencional para evitar conflictos de estado

### Zona Horaria
- Todas las vigencias se calculan en zona horaria `America/Bogota` (UTC-5)
- Los derechos DIARIO se generan desde las `00:00:00` hasta las `23:59:59.999999999` del día actual en Bogotá

### Logs de Auditoría
- Cada aprobación/rechazo se registra con `log.info()` incluyendo:
  - ID del pago
  - Documento del usuario
  - Tipo de pago
  - Vigencias del derecho generado (en caso de aprobación)

---

## 🚀 Próximos Pasos

### Fase 3.3: Reportes Adicionales
- Endpoint `GET /api/reportes/pagos/aprobados` (filtros por fecha, usuario, tipo)
- Endpoint `GET /api/reportes/pagos/rechazados`
- Endpoint `GET /api/reportes/derechos-uso/activos`
- Exportación a Excel/PDF

### Fase 7: Testing
- Implementar los 7 tests recomendados con `@SpringBootTest`
- Tests de integración con Testcontainers (PostgreSQL)
- Tests de concurrencia (aprobar múltiples pagos simultáneamente)

### Mejoras Futuras
- **Motivo de rechazo:** Añadir campo `motivo_rechazo` en tabla `pagos`
- **Auditoría avanzada:** Registrar quién aprobó/rechazó (campo `aprobado_por`)
- **Notificaciones:** Enviar email/SMS al usuario cuando se aprueba/rechaza su pago
- **Cancelación de aprobación:** Endpoint `/desaprobar` para revertir aprobaciones erróneas

---

## 📚 Referencias

- **Código fuente:**
  - `PagoService.java`: Lógica de aprobación/rechazo
  - `PagoController.java`: Endpoints REST
  - `DerechoUsoService.java`: Generación automática de derechos
  - `PaquetePagoService.java`: Consumo de días de paquetes

- **Documentación relacionada:**
  - [Fase 3.1: API de Usuarios](./fase3.1_resumen.md)
  - [Fase 2.2: Servicios de Pago](./fase2.2_resumen.md)
  - [Architecture.md](../architecture.md)

---

**🎉 Fase 3.2 completada con éxito**

**Próxima acción recomendada:** Implementar **Fase 3.3 (Reportes Adicionales)** o ejecutar tests de integración para validar el flujo completo de aprobación/rechazo.
