# Fase 3.3: Reportes Adicionales - Resumen de Implementación

**Fecha:** 2025-01-27  
**Estado:** ✅ Completado  
**Desarrollador:** Equipo EduFeed

---

## 📋 Objetivo

Implementar endpoints REST para **reportes administrativos adicionales**, incluyendo:
- **Asistencias diarias** (accesos exitosos agregados)
- **Rechazos diarios** (intentos fallidos por motivo)
- **Derechos de uso activos** (usuarios con acceso vigente)
- **Exportación CSV** para todos los reportes

---

## 🚀 Funcionalidades Implementadas

### 1. Reporte de Asistencias Diarias

**Endpoint:** `GET /api/reportes/asistencias`

**Descripción:**
- Agrega accesos exitosos por día
- Cuenta total de accesos y usuarios únicos
- Soporta filtrado por rango de fechas

**Parámetros:**
- `desde` (opcional): Fecha inicial en formato ISO 8601
- `hasta` (opcional): Fecha final en formato ISO 8601

**Respuesta:**
```json
[
  {
    "dia": "2025-01-27",
    "totalAccesos": 145,
    "usuariosUnicos": 87
  },
  {
    "dia": "2025-01-26",
    "totalAccesos": 132,
    "usuariosUnicos": 79
  }
]
```

**Ejemplo de uso:**
```bash
# Asistencias de los últimos 7 días
curl "http://localhost:8080/api/reportes/asistencias?desde=2025-01-20T00:00:00-05:00&hasta=2025-01-27T23:59:59-05:00"

# Todas las asistencias (sin filtro)
curl "http://localhost:8080/api/reportes/asistencias"
```

**Exportación CSV:**
```bash
# GET /api/reportes/asistencias.csv
curl -O "http://localhost:8080/api/reportes/asistencias.csv?desde=2025-01-01T00:00:00-05:00"

# Archivo generado: asistencias.csv
# dia,total_accesos,usuarios_unicos
# 2025-01-27,145,87
# 2025-01-26,132,79
```

---

### 2. Reporte de Rechazos Diarios

**Endpoint:** `GET /api/reportes/rechazos`

**Descripción:**
- Agrega intentos de acceso fallidos por día y motivo
- Útil para detectar problemas de autenticación
- Identifica patrones de usuarios sin derechos vigentes

**Parámetros:**
- `desde` (opcional): Fecha inicial
- `hasta` (opcional): Fecha final

**Respuesta:**
```json
[
  {
    "dia": "2025-01-27",
    "motivoRechazo": "SIN_DERECHO_VIGENTE",
    "cantidad": 23
  },
  {
    "dia": "2025-01-27",
    "motivoRechazo": "AUTENTICACION_FALLIDA",
    "cantidad": 8
  },
  {
    "dia": "2025-01-26",
    "motivoRechazo": "SIN_DERECHO_VIGENTE",
    "cantidad": 19
  }
]
```

**Motivos de rechazo comunes:**
- `SIN_DERECHO_VIGENTE`: Usuario sin pago vigente
- `AUTENTICACION_FALLIDA`: Biometría no coincide
- `USUARIO_INACTIVO`: Cuenta deshabilitada
- `SIN_ESPECIFICAR`: Otros errores

**Ejemplo de uso:**
```bash
# Rechazos del mes actual
curl "http://localhost:8080/api/reportes/rechazos?desde=2025-01-01T00:00:00-05:00"
```

**Exportación CSV:**
```bash
# GET /api/reportes/rechazos.csv
curl -O "http://localhost:8080/api/reportes/rechazos.csv"

# Archivo generado: rechazos.csv
# dia,motivo_rechazo,cantidad
# 2025-01-27,SIN_DERECHO_VIGENTE,23
# 2025-01-27,AUTENTICACION_FALLIDA,8
```

---

### 3. Reporte de Derechos de Uso Activos

**Endpoint:** `GET /api/reportes/derechos-activos`

**Descripción:**
- Lista todos los derechos de uso vigentes en el momento actual
- Incluye información del usuario (documento, nombre)
- Muestra tipo de derecho (DIARIO, MENSUAL, PAQUETE)
- Para paquetes, indica días restantes

**Respuesta:**
```json
[
  {
    "usuarioDocumento": "1234567890",
    "usuarioNombre": "Juan Pérez",
    "tipoDerecho": "MENSUAL",
    "vigenteDesde": "2025-01-01T00:00:00-05:00",
    "vigenteHasta": "2025-01-31T23:59:59.999999999-05:00",
    "diasRestantes": null
  },
  {
    "usuarioDocumento": "0987654321",
    "usuarioNombre": "María García",
    "tipoDerecho": "PAQUETE",
    "vigenteDesde": "2025-01-27T00:00:00-05:00",
    "vigenteHasta": "2025-01-27T23:59:59.999999999-05:00",
    "diasRestantes": 7
  }
]
```

**Casos de uso:**
- Auditoría: ¿Quiénes tienen acceso actualmente?
- Soporte: Verificar si un usuario tiene derecho vigente
- Renovaciones: Identificar derechos próximos a vencer

**Ejemplo de uso:**
```bash
# Listar derechos activos
curl "http://localhost:8080/api/reportes/derechos-activos"
```

**Exportación CSV:**
```bash
# GET /api/reportes/derechos-activos.csv
curl -O "http://localhost:8080/api/reportes/derechos-activos.csv"

# Archivo generado: derechos_activos.csv
# documento,nombre,tipo_derecho,vigente_desde,vigente_hasta,dias_restantes
# 1234567890,Juan Pérez,MENSUAL,2025-01-01T00:00:00-05:00,2025-01-31T23:59:59-05:00,
# 0987654321,María García,PAQUETE,2025-01-27T00:00:00-05:00,2025-01-27T23:59:59-05:00,7
```

---

## 🔧 Cambios en el Código

### Archivos Creados

#### 1. `AsistenciasDiariasItem.java`
```java
public class AsistenciasDiariasItem {
    private LocalDate dia;
    private Long totalAccesos;
    private Long usuariosUnicos;
    // getters/setters...
}
```

#### 2. `RechazosDiariosItem.java`
```java
public class RechazosDiariosItem {
    private LocalDate dia;
    private String motivoRechazo;
    private Long cantidad;
    // getters/setters...
}
```

#### 3. `DerechoActivoItem.java`
```java
public class DerechoActivoItem {
    private String usuarioDocumento;
    private String usuarioNombre;
    private String tipoDerecho;
    private OffsetDateTime vigenteDesde;
    private OffsetDateTime vigenteHasta;
    private Integer diasRestantes; // null si no es PAQUETE
    // getters/setters...
}
```

---

### Archivos Modificados

#### 1. `AccesoRepository.java`

**Consulta nativa: Asistencias diarias**
```java
@Query(value = """
    SELECT 
        DATE(a.creado_en AT TIME ZONE 'America/Bogota') as dia,
        COUNT(*) as total_accesos,
        COUNT(DISTINCT a.usuario_id) as usuarios_unicos
    FROM accesos a
    WHERE a.exitoso = true
        AND (:desde IS NULL OR a.creado_en >= :desde)
        AND (:hasta IS NULL OR a.creado_en <= :hasta)
    GROUP BY DATE(a.creado_en AT TIME ZONE 'America/Bogota')
    ORDER BY dia DESC
    """, nativeQuery = true)
List<Object[]> aggregateAsistenciasDiarias(
    @Param("desde") OffsetDateTime desde,
    @Param("hasta") OffsetDateTime hasta);
```

**Consulta nativa: Rechazos diarios**
```java
@Query(value = """
    SELECT 
        DATE(a.creado_en AT TIME ZONE 'America/Bogota') as dia,
        COALESCE(a.motivo_rechazo, 'SIN_ESPECIFICAR') as motivo_rechazo,
        COUNT(*) as cantidad
    FROM accesos a
    WHERE a.exitoso = false
        AND (:desde IS NULL OR a.creado_en >= :desde)
        AND (:hasta IS NULL OR a.creado_en <= :hasta)
    GROUP BY DATE(a.creado_en AT TIME ZONE 'America/Bogota'), COALESCE(a.motivo_rechazo, 'SIN_ESPECIFICAR')
    ORDER BY dia DESC, cantidad DESC
    """, nativeQuery = true)
List<Object[]> aggregateRechazosDiarios(
    @Param("desde") OffsetDateTime desde,
    @Param("hasta") OffsetDateTime hasta);
```

---

#### 2. `DerechoUsoRepository.java`

**Consulta nativa: Derechos activos con detalle**
```java
@Query(value = """
    SELECT 
        u.documento,
        u.nombre,
        d.tipo_derecho,
        d.vigente_desde,
        d.vigente_hasta,
        pp.dias_restantes
    FROM derechos_uso d
    JOIN usuarios u ON d.usuario_id = u.id
    LEFT JOIN paquetes_pago pp ON d.pago_origen_id = pp.pago_id
    WHERE d.activo = true
        AND d.vigente_hasta > :ahora
    ORDER BY d.vigente_hasta ASC
    """, nativeQuery = true)
List<Object[]> findDerechosActivosConDetalle(@Param("ahora") OffsetDateTime ahora);
```

**Características:**
- `LEFT JOIN` con `paquetes_pago`: permite incluir DIARIO/MENSUAL sin paquete
- Filtro `vigente_hasta > :ahora`: solo derechos aún vigentes
- Orden `vigente_hasta ASC`: muestra próximos a vencer primero

---

#### 3. `ReportService.java`

**Método: `asistenciasDiarias()`**
```java
public List<AsistenciasDiariasItem> asistenciasDiarias(OffsetDateTime desde, OffsetDateTime hasta) {
    List<Object[]> rows = accesoRepository.aggregateAsistenciasDiarias(desde, hasta);
    List<AsistenciasDiariasItem> out = new ArrayList<>();
    for (Object[] r : rows) {
        AsistenciasDiariasItem item = new AsistenciasDiariasItem();
        item.setDia(((java.sql.Date) r[0]).toLocalDate());
        item.setTotalAccesos(((Number) r[1]).longValue());
        item.setUsuariosUnicos(((Number) r[2]).longValue());
        out.add(item);
    }
    return out;
}
```

**Método: `rechazosDiarios()`**
```java
public List<RechazosDiariosItem> rechazosDiarios(OffsetDateTime desde, OffsetDateTime hasta) {
    List<Object[]> rows = accesoRepository.aggregateRechazosDiarios(desde, hasta);
    List<RechazosDiariosItem> out = new ArrayList<>();
    for (Object[] r : rows) {
        RechazosDiariosItem item = new RechazosDiariosItem();
        item.setDia(((java.sql.Date) r[0]).toLocalDate());
        item.setMotivoRechazo((String) r[1]);
        item.setCantidad(((Number) r[2]).longValue());
        out.add(item);
    }
    return out;
}
```

**Método: `derechosActivos()`**
```java
public List<DerechoActivoItem> derechosActivos() {
    OffsetDateTime ahora = OffsetDateTime.now(timezone);
    List<Object[]> rows = derechoUsoRepository.findDerechosActivosConDetalle(ahora);
    List<DerechoActivoItem> out = new ArrayList<>();
    for (Object[] r : rows) {
        DerechoActivoItem item = new DerechoActivoItem();
        item.setUsuarioDocumento((String) r[0]);
        item.setUsuarioNombre((String) r[1]);
        item.setTipoDerecho((String) r[2]);
        
        // Convertir Timestamp a OffsetDateTime
        if (r[3] instanceof Timestamp) {
            item.setVigenteDesde(((Timestamp) r[3]).toInstant().atZone(timezone).toOffsetDateTime());
        }
        if (r[4] instanceof Timestamp) {
            item.setVigenteHasta(((Timestamp) r[4]).toInstant().atZone(timezone).toOffsetDateTime());
        }
        
        // dias_restantes puede ser null si no es paquete
        if (r[5] != null) {
            item.setDiasRestantes(((Number) r[5]).intValue());
        }
        out.add(item);
    }
    return out;
}
```

---

#### 4. `ReportController.java`

**Nuevos endpoints JSON:**
```java
@GetMapping("/asistencias")
public List<AsistenciasDiariasItem> asistencias(
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta) {
    return reportService.asistenciasDiarias(desde, hasta);
}

@GetMapping("/rechazos")
public List<RechazosDiariosItem> rechazos(...) {
    return reportService.rechazosDiarios(desde, hasta);
}

@GetMapping("/derechos-activos")
public List<DerechoActivoItem> derechosActivos() {
    return reportService.derechosActivos();
}
```

**Nuevos endpoints CSV:**
```java
@GetMapping(value = "/asistencias.csv", produces = "text/csv")
public ResponseEntity<String> exportAsistenciasCsv(...) {
    List<AsistenciasDiariasItem> items = reportService.asistenciasDiarias(desde, hasta);
    StringJoiner sj = new StringJoiner("\n");
    sj.add("dia,total_accesos,usuarios_unicos");
    for (AsistenciasDiariasItem i : items) {
        sj.add(String.format("%s,%d,%d", i.getDia(), i.getTotalAccesos(), i.getUsuariosUnicos()));
    }
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=asistencias.csv")
        .contentType(MediaType.valueOf("text/csv"))
        .body(sj.toString());
}

// Similar para /rechazos.csv y /derechos-activos.csv
```

---

## 📊 Resumen de Endpoints

| Endpoint | Método | Descripción | Formato |
|----------|--------|-------------|---------|
| `/api/reportes/asistencias` | GET | Asistencias diarias agregadas | JSON |
| `/api/reportes/asistencias.csv` | GET | Exportar asistencias | CSV |
| `/api/reportes/rechazos` | GET | Rechazos diarios por motivo | JSON |
| `/api/reportes/rechazos.csv` | GET | Exportar rechazos | CSV |
| `/api/reportes/derechos-activos` | GET | Derechos vigentes con detalle | JSON |
| `/api/reportes/derechos-activos.csv` | GET | Exportar derechos activos | CSV |
| `/api/reportes/ingresos` | GET | Ingresos diarios (existente) | JSON |
| `/api/reportes/ingresos.csv` | GET | Exportar ingresos (existente) | CSV |
| `/api/reportes/ingresos/resumen` | GET | Total ingresos (existente) | JSON |

**Total:** 9 endpoints (3 nuevos JSON + 3 nuevos CSV + 3 existentes)

---

## 🧪 Casos de Prueba Recomendados

### Test 1: Asistencias Diarias Sin Filtros
```java
@Test
void asistenciasDiariasSinFiltrosDebeRetornarTodos() {
    // Given: 3 accesos exitosos en diferentes días
    crearAccesoExitoso("2025-01-25", "user1");
    crearAccesoExitoso("2025-01-25", "user2");
    crearAccesoExitoso("2025-01-26", "user1");
    
    // When
    List<AsistenciasDiariasItem> resultado = reportService.asistenciasDiarias(null, null);
    
    // Then
    assertThat(resultado).hasSize(2); // 2 días distintos
    
    AsistenciasDiariasItem dia1 = resultado.stream()
        .filter(a -> a.getDia().equals(LocalDate.of(2025, 1, 25)))
        .findFirst().get();
    assertThat(dia1.getTotalAccesos()).isEqualTo(2);
    assertThat(dia1.getUsuariosUnicos()).isEqualTo(2);
}
```

### Test 2: Rechazos Agrupados Por Motivo
```java
@Test
void rechazosDiariosDebeAgruparPorMotivo() {
    // Given: Rechazos con diferentes motivos
    crearAccesoFallido("2025-01-27", "SIN_DERECHO_VIGENTE");
    crearAccesoFallido("2025-01-27", "SIN_DERECHO_VIGENTE");
    crearAccesoFallido("2025-01-27", "AUTENTICACION_FALLIDA");
    
    // When
    List<RechazosDiariosItem> resultado = reportService.rechazosDiarios(null, null);
    
    // Then
    assertThat(resultado).hasSize(2); // 2 motivos diferentes
    
    RechazosDiariosItem sinDerecho = resultado.stream()
        .filter(r -> r.getMotivoRechazo().equals("SIN_DERECHO_VIGENTE"))
        .findFirst().get();
    assertThat(sinDerecho.getCantidad()).isEqualTo(2);
}
```

### Test 3: Derechos Activos Solo Vigentes
```java
@Test
void derechosActivosDebeExcluirVencidos() {
    // Given: 1 derecho vigente, 1 vencido
    crearDerechoVigente("user1", LocalDate.now(), LocalDate.now().plusDays(10));
    crearDerechoVencido("user2", LocalDate.now().minusDays(30), LocalDate.now().minusDays(1));
    
    // When
    List<DerechoActivoItem> resultado = reportService.derechosActivos();
    
    // Then
    assertThat(resultado).hasSize(1);
    assertThat(resultado.get(0).getUsuarioDocumento()).isEqualTo("user1");
}
```

### Test 4: Exportación CSV Asistencias
```java
@Test
void exportAsistenciasCsvDebeGenerarFormatoCorrecto() {
    // Given
    crearAccesoExitoso("2025-01-27", "user1");
    
    // When
    ResponseEntity<String> response = reportController.exportAsistenciasCsv(null, null);
    
    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.valueOf("text/csv"));
    assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
        .contains("attachment; filename=asistencias.csv");
    
    String csv = response.getBody();
    assertThat(csv).startsWith("dia,total_accesos,usuarios_unicos\n");
    assertThat(csv).contains("2025-01-27,1,1");
}
```

### Test 5: Días Restantes Solo en Paquetes
```java
@Test
void derechosActivosDebeMostrarDiasRestantesSoloPaquetes() {
    // Given
    Pago pagoDiario = crearPagoDiario("user1");
    Pago pagoPaquete = crearPagoPaquete("user2", 10);
    
    aprobarPago(pagoDiario); // Genera DerechoUso DIARIO
    aprobarPago(pagoPaquete); // Genera DerechoUso PAQUETE + PaquetePago
    
    // When
    List<DerechoActivoItem> resultado = reportService.derechosActivos();
    
    // Then
    DerechoActivoItem diario = resultado.stream()
        .filter(d -> d.getTipoDerecho().equals("DIARIO"))
        .findFirst().get();
    assertThat(diario.getDiasRestantes()).isNull();
    
    DerechoActivoItem paquete = resultado.stream()
        .filter(d -> d.getTipoDerecho().equals("PAQUETE"))
        .findFirst().get();
    assertThat(paquete.getDiasRestantes()).isEqualTo(10);
}
```

---

## 📈 Métricas de Implementación

| Métrica | Valor |
|---------|-------|
| **DTOs creados** | 3 (`AsistenciasDiariasItem`, `RechazosDiariosItem`, `DerechoActivoItem`) |
| **Repositorios modificados** | 2 (`AccesoRepository`, `DerechoUsoRepository`) |
| **Consultas nativas añadidas** | 3 (asistencias, rechazos, derechos activos) |
| **Métodos en ReportService** | 3 nuevos (total 5 con existentes) |
| **Endpoints nuevos** | 6 (3 JSON + 3 CSV) |
| **Líneas de código añadidas** | ~350 (incluyendo DTOs, consultas, controladores) |
| **Tests recomendados** | 5 (cobertura completa de flujos) |

---

## ✅ Verificación de Compilación

```bash
cd "c:\Users\Julia\OneDrive\Documentos\GitHub\EduFeed"
$env:JAVA_HOME='C:/Program Files/Java/jdk-24'
& "$env:USERPROFILE\tools\maven\apache-maven-3.9.9\bin\mvn.cmd" clean compile -DskipTests
```

**Resultado:** ✅ `BUILD SUCCESS` - 90 archivos compilados sin errores

---

## 🔗 Integración con Fases Previas

### Reutilización de Infraestructura
- ✅ `AccesoRepository` (Fase 2.3) - Extendido con consultas nativas
- ✅ `DerechoUsoRepository` (Fase 2.2) - Extendida con JOIN a paquetes_pago
- ✅ Patrón de exportación CSV heredado de `/ingresos.csv` (Fase anterior)

### Relación con RF (Requisitos Funcionales)
- **RF-06:** Reportes de ingresos y asistencias ✅
- **RF-10:** Consultas administrativas (derechos activos, rechazos) ✅
- **RF-09:** Auditoría de accesos (implícito en asistencias/rechazos) ✅

---

## 📝 Notas Técnicas

### Zona Horaria
- Todas las agregaciones usan `AT TIME ZONE 'America/Bogota'` en las consultas SQL
- Los `Timestamp` de PostgreSQL se convierten a `OffsetDateTime` con zona horaria Bogotá
- Garantiza consistencia en reportes diarios (día = 00:00 - 23:59 hora Bogotá)

### Optimización de Consultas
- **Índices recomendados:**
  ```sql
  CREATE INDEX idx_accesos_creado_exitoso ON accesos(creado_en, exitoso);
  CREATE INDEX idx_derechos_uso_activo_vigencia ON derechos_uso(activo, vigente_hasta);
  ```
- Las consultas usan `GROUP BY` eficiente con conversión de fecha
- `COUNT(DISTINCT usuario_id)` puede ser costoso en tablas grandes (considerar cache)

### Manejo de NULL
- `COALESCE(motivo_rechazo, 'SIN_ESPECIFICAR')`: evita agrupar NULLs
- `LEFT JOIN paquetes_pago`: permite incluir derechos DIARIO/MENSUAL sin paquete
- `diasRestantes` es `null` en Java si el SQL devuelve `NULL` (pagos no-paquete)

### Formato CSV
- Separador: `,` (coma)
- Sin comillas alrededor de strings (asume nombres sin comas)
- Header en primera línea
- `Content-Disposition: attachment` fuerza descarga en navegadores

---

## 🚀 Próximos Pasos

### Mejoras Futuras
- **Exportación Excel:** Añadir endpoints `.xlsx` usando Apache POI
- **Reportes PDF:** Integrar JasperReports o iText para reportes con logo/encabezados
- **Gráficos:** Añadir endpoint `/reportes/dashboard` con estadísticas agregadas
- **Cache:** Implementar cache de reportes con TTL (e.g., 5 minutos) para reducir carga DB
- **Paginación:** Para reportes muy grandes (asistencias de varios meses)

### Fase 7: Testing
- Implementar los 5 tests recomendados con `@SpringBootTest`
- Tests de performance: reportes con 100K+ registros
- Tests de concurrencia: múltiples usuarios descargando CSV simultáneamente

### Optimizaciones
- **Vista materializada:** Para reportes frecuentes (refrescar cada hora)
  ```sql
  CREATE MATERIALIZED VIEW mv_asistencias_diarias AS
  SELECT DATE(creado_en AT TIME ZONE 'America/Bogota') AS dia,
         COUNT(*) AS total_accesos,
         COUNT(DISTINCT usuario_id) AS usuarios_unicos
  FROM accesos
  WHERE exitoso = true
  GROUP BY DATE(creado_en AT TIME ZONE 'America/Bogota');
  ```

### Nuevos Reportes
- **Reporte de renovaciones:** Usuarios con derechos próximos a vencer (< 3 días)
- **Reporte de uso de paquetes:** Promedio de días usados vs comprados
- **Reporte de horarios:** Picos de asistencia por hora del día

---

## 📚 Referencias

- **Código fuente:**
  - `ReportService.java`: Lógica de agregación
  - `ReportController.java`: Endpoints REST
  - `AccesoRepository.java`: Consultas de accesos
  - `DerechoUsoRepository.java`: Consultas de derechos
  - DTOs: `AsistenciasDiariasItem`, `RechazosDiariosItem`, `DerechoActivoItem`

- **Documentación relacionada:**
  - [Fase 3.1: API de Usuarios](./fase3.1_resumen.md)
  - [Fase 3.2: API de Pagos](./fase3.2_resumen.md)
  - [Fase 2.3: Servicio de Accesos](./fase2.3_resumen.md)
  - [Architecture.md](../architecture.md)

---

**🎉 Fase 3.3 completada con éxito**

**Próxima acción recomendada:** Ejecutar tests de integración con datos reales o implementar **exportación Excel** para reportes más profesionales.
