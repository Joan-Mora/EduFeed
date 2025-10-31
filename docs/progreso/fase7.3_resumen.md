# FASE 7.3 · Tests de Performance y Optimización

**Fecha:** 29 de octubre de 2025  
**Estado:** ⚙️ En progreso (infraestructura lista, pendiente ejecución de carga)

## Objetivos

- Validar rendimiento de la API bajo carga concurrente
- Medir p95 latency de endpoints críticos
- Identificar cuellos de botella antes de producción
- Aplicar optimizaciones de base de datos (índices)

## Requisitos de Performance

| Endpoint | Operación | Usuarios Concurrentes | p95 Latency | Success Rate |
|----------|-----------|----------------------|-------------|--------------|
| `POST /api/accesos/verificar` | Verificación de acceso | 500 | < 2s | ≥ 99% |
| `POST /api/pagos` | Crear pago | 500 | < 1s | ≥ 99% |
| `GET /api/reportes/ingresos` | Reporte de ingresos | 500 | < 3s | ≥ 99% |

## Infraestructura de Performance Testing

### Módulo Gatling

**Ubicación:** `edufeed-perf/`

Nuevo módulo Maven opcional para tests de carga con Gatling (Scala DSL).

**Estructura:**
```
edufeed-perf/
├── pom.xml                           # Plugin Gatling + deps
└── src/test/scala/co/cellano/edufeed/perf/
    ├── BaseSimulation.scala          # Configuración común (login, httpProtocol)
    ├── AccessCheckSimulation.scala   # Carga en verificación de accesos
    ├── PaymentsSimulation.scala      # Carga en creación de pagos
    └── ReportsSimulation.scala       # Carga en reportes
```

### Configuración Maven

**Archivo:** `edufeed-perf/pom.xml`

```xml
<properties>
    <gatling.version>3.9.5</gatling.version>
    <gatling.plugin.version>3.5.1</gatling.plugin.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.gatling</groupId>
            <artifactId>gatling-bom</artifactId>
            <version>${gatling.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>io.gatling</groupId>
        <artifactId>gatling-app</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>io.gatling</groupId>
        <artifactId>gatling-charts-highcharts</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>io.gatling</groupId>
            <artifactId>gatling-maven-plugin</artifactId>
            <version>${gatling.plugin.version}</version>
        </plugin>
    </plugins>
</build>
```

---

## Simulaciones Implementadas

### 1. BaseSimulation (Común)

**Propósito:** Configuración compartida para todas las simulaciones.

**Características:**
- Login automático para obtener JWT token
- HTTP protocol configurado con base URL parametrizable
- Headers comunes (Content-Type, User-Agent)
- Credenciales desde system properties o environment variables

**Código:**
```scala
object BaseSimulation {
  val baseUrl: String = System.getProperty("baseUrl", "http://localhost:8080")
  val adminUser: String = System.getProperty("perf.username", 
    sys.env.getOrElse("SEED_OPERATOR_USERNAME", "admin"))
  val adminPass: String = System.getProperty("perf.password", 
    sys.env.getOrElse("SEED_OPERATOR_PASSWORD", "Admin123$"))

  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("EduFeed-Perf/1.0")

  val login = exec(
    http("auth_login")
      .post("/api/auth/login")
      .body(StringBody(session => s"""{"username":"${adminUser}","password":"${adminPass}"}"""))
      .check(status.is(200))
      .check(jsonPath("$.accessToken").saveAs("accessToken"))
  ).exec(session => session.set("authHeader", s"Bearer ${session("accessToken").as[String]}"))
}
```

---

### 2. AccessCheckSimulation

**Objetivo:** Validar latencia de verificación de accesos bajo carga.

**Flujo:**
1. Login → obtener token JWT
2. Llamar a `POST /api/accesos/verificar` con usuarioId y modalidad HUELLA
3. Validar respuesta 200 OK

**Perfil de carga:**
- **Smoke:** 20 usuarios, ramp 10s, hold 30s
- **Load:** 500 usuarios, ramp 60s, hold 300s

**Asserts:**
- p95 response time < 2000ms
- Success rate ≥ 99%

**Código:**
```scala
class AccessCheckSimulation extends Simulation {
  import BaseSimulation._

  val accessCheck = exec(login)
    .exec(
      http("verificar_acceso")
        .post("/api/accesos/verificar")
        .header("Authorization", "${authHeader}")
        .body(StringBody("""{"usuarioId":"<UUID>","modalidad":"HUELLA"}"""))
        .check(status.is(200))
    )

  val scn = scenario("AccessCheck").exec(accessCheck)

  setUp(
    scn.inject(
      rampUsers(500).during(60.seconds),
      constantUsersPerSec(10).during(300.seconds)
    )
  ).protocols(httpProtocol)
   .assertions(
     global.responseTime.percentile3.lt(2000),
     global.successfulRequests.percent.gte(99)
   )
}
```

---

### 3. PaymentsSimulation

**Objetivo:** Validar latencia de creación de pagos bajo carga.

**Flujo:**
1. Login → obtener token JWT
2. Crear pago DIARIO con `POST /api/pagos`
3. Listar pagos del usuario con `GET /api/pagos/usuario/{id}`

**Perfil de carga:**
- **Smoke:** 20 usuarios, ramp 10s, hold 30s
- **Load:** 500 usuarios, ramp 60s, hold 300s

**Asserts:**
- p95 response time < 1000ms
- Success rate ≥ 99%

**Código:**
```scala
class PaymentsSimulation extends Simulation {
  import BaseSimulation._

  val createPayment = exec(login)
    .exec(
      http("crear_pago")
        .post("/api/pagos")
        .header("Authorization", "${authHeader}")
        .body(StringBody("""{"usuarioId":"<UUID>","monto":12000,"tipoPago":"DIARIO","metodoPago":"EFECTIVO"}"""))
        .check(status.is(201))
        .check(jsonPath("$.id").saveAs("pagoId"))
    )
    .exec(
      http("listar_pagos")
        .get("/api/pagos/usuario/${usuarioId}")
        .header("Authorization", "${authHeader}")
        .check(status.is(200))
    )

  val scn = scenario("Payments").exec(createPayment)

  setUp(
    scn.inject(rampUsers(500).during(60.seconds))
  ).protocols(httpProtocol)
   .assertions(
     global.responseTime.percentile3.lt(1000),
     global.successfulRequests.percent.gte(99)
   )
}
```

---

### 4. ReportsSimulation

**Objetivo:** Validar latencia de reportes administrativos bajo carga.

**Flujo:**
1. Login → obtener token JWT
2. Consultar `GET /api/reportes/ingresos`
3. Consultar `GET /api/reportes/asistencias`

**Perfil de carga:**
- **Smoke:** 20 usuarios, ramp 10s, hold 30s
- **Load:** 500 usuarios, ramp 60s, hold 300s

**Asserts:**
- p95 response time < 3000ms
- Success rate ≥ 99%

**Código:**
```scala
class ReportsSimulation extends Simulation {
  import BaseSimulation._

  val reports = exec(login)
    .exec(
      http("reporte_ingresos")
        .get("/api/reportes/ingresos")
        .header("Authorization", "${authHeader}")
        .check(status.is(200))
    )
    .exec(
      http("reporte_asistencias")
        .get("/api/reportes/asistencias")
        .header("Authorization", "${authHeader}")
        .check(status.is(200))
    )

  val scn = scenario("Reports").exec(reports)

  setUp(
    scn.inject(rampUsers(500).during(60.seconds))
  ).protocols(httpProtocol)
   .assertions(
     global.responseTime.percentile3.lt(3000),
     global.successfulRequests.percent.gte(99)
   )
}
```

---

## Optimizaciones de Base de Datos

### Migración V6: Índices de Performance

**Archivo:** `edufeed-backend/src/main/resources/db/migration/V6__perf_indexes.sql`

**Índices creados:**

```sql
-- Pagos: filtrado por estado y fecha de creación
CREATE INDEX IF NOT EXISTS idx_pagos_estado_creado 
  ON pagos(estado_pago, creado_en DESC);

-- Pagos: filtrado por usuario y fecha de creación
CREATE INDEX IF NOT EXISTS idx_pagos_usuario_creado 
  ON pagos(usuario_id, creado_en DESC);

-- Pagos: filtrado por tipo y fecha de creación
CREATE INDEX IF NOT EXISTS idx_pagos_tipo_creado 
  ON pagos(tipo_pago, creado_en DESC);

-- Accesos: filtrado por usuario y fecha (reportes de asistencia)
CREATE INDEX IF NOT EXISTS idx_accesos_usuario_fecha 
  ON accesos(usuario_id, fecha_hora DESC);

-- Accesos: filtrado por estado y fecha (reportes de rechazos)
CREATE INDEX IF NOT EXISTS idx_accesos_estado_fecha 
  ON accesos(estado, fecha_hora DESC);

-- Derechos de uso: búsqueda de derechos activos vigentes
CREATE INDEX IF NOT EXISTS idx_derechos_usuario_activo_vigencia 
  ON derechos_uso(usuario_id, activo, vigente_hasta DESC) 
  WHERE activo = true;
```

**Queries optimizadas:**
- `GET /api/pagos/estado/{estado}` → usa `idx_pagos_estado_creado`
- `GET /api/pagos/usuario/{id}` → usa `idx_pagos_usuario_creado`
- `GET /api/reportes/asistencias` → usa `idx_accesos_usuario_fecha`
- `GET /api/reportes/rechazos` → usa `idx_accesos_estado_fecha`
- Verificación de derecho vigente → usa `idx_derechos_usuario_activo_vigencia`

**Aplicación:**
```bash
# Flyway aplica automáticamente al arrancar backend
mvn spring-boot:run
```

---

## Comandos de Ejecución

### Ejecutar Smoke Test (20 usuarios)

```bash
cd edufeed-perf

# AccessCheck
mvn gatling:test -Dgatling.simulationClass=co.cellano.edufeed.perf.AccessCheckSimulation \
  -DbaseUrl=http://localhost:8080 \
  -Dperf.username=admin \
  -Dperf.password=Admin123$

# Payments
mvn gatling:test -Dgatling.simulationClass=co.cellano.edufeed.perf.PaymentsSimulation

# Reports
mvn gatling:test -Dgatling.simulationClass=co.cellano.edufeed.perf.ReportsSimulation
```

### Ejecutar Load Test (500 usuarios)

```bash
# Ajustar perfil de carga en el código (rampUsers(500))
mvn gatling:test -Dgatling.simulationClass=co.cellano.edufeed.perf.AccessCheckSimulation
```

### Ver Reporte HTML

```bash
# Gatling genera reporte automáticamente en:
edufeed-perf/target/gatling/<simulation-timestamp>/index.html

# Abrir en navegador
start target/gatling/<timestamp>/index.html
```

---

## Configuración de Ambiente para Tests

### Aumentar Pool de Conexiones (application.yml)

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50        # Default: 10
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
```

### Tuning PostgreSQL (docker-compose.yml)

```yaml
services:
  db:
    environment:
      POSTGRES_SHARED_BUFFERS: 256MB
      POSTGRES_EFFECTIVE_CACHE_SIZE: 1GB
      POSTGRES_MAX_CONNECTIONS: 200
```

---

## Estado Actual

### ✅ Completado

- [x] Módulo `edufeed-perf` creado
- [x] Simulaciones Gatling implementadas (AccessCheck, Payments, Reports)
- [x] Configuración de autenticación JWT en tests
- [x] Índices de performance aplicados (V6 migration)
- [x] Documentación de performance (`docs/performance.md`)

### ⚙️ Pendiente

- [ ] Corregir dependencias de Gatling (BOM 3.9.5 no resuelve)
- [ ] Ejecutar smoke tests (20 usuarios)
- [ ] Ejecutar load tests (500 usuarios)
- [ ] Recopilar métricas p95 y validar vs SLAs
- [ ] Ajustar pool de conexiones si hay timeouts
- [ ] Optimizar queries lentas identificadas con EXPLAIN ANALYZE

---

## Troubleshooting

### Problema: Dependency resolution failed (Gatling BOM)

**Error:**
```
Non-resolvable import POM: io.gatling:gatling-bom:pom:3.9.5
```

**Solución temporal:**
- Usar versiones explícitas en lugar de BOM
- O cambiar a versión 3.11.x de Gatling

### Problema: Puerto 8080 en uso

**Solución:**
```bash
# Detener backend dev
docker compose down

# O usar puerto staging 8081
-DbaseUrl=http://localhost:8081
```

### Problema: 401 Unauthorized en tests

**Causa:** Token JWT expirado o credenciales incorrectas.

**Solución:**
- Verificar operadores seed en `DevOperatorSeeder`
- Validar que el backend está corriendo con perfil correcto

---

## Métricas Esperadas (Post-Ejecución)

| Simulación | Usuarios | Requests/seg | p50 | p95 | p99 | Success % |
|------------|----------|--------------|-----|-----|-----|-----------|
| AccessCheck | 500 | ~50 | <500ms | <2s | <3s | ≥99% |
| Payments | 500 | ~40 | <300ms | <1s | <1.5s | ≥99% |
| Reports | 500 | ~30 | <1s | <3s | <5s | ≥99% |

---

## Archivos Clave

```
edufeed-perf/
├── pom.xml
└── src/test/scala/co/cellano/edufeed/perf/
    ├── BaseSimulation.scala
    ├── AccessCheckSimulation.scala
    ├── PaymentsSimulation.scala
    └── ReportsSimulation.scala

edufeed-backend/src/main/resources/db/migration/
└── V6__perf_indexes.sql

docs/
└── performance.md                    # Guía completa de ejecución
```

---

## Próximos Pasos

1. **Ajustar dependencias Gatling** para permitir ejecución
2. **Ejecutar smoke tests** con 20 usuarios para validar setup
3. **Ejecutar load tests** con 500 usuarios y recopilar métricas
4. **Analizar cuellos de botella** con EXPLAIN ANALYZE en queries lentas
5. **Aplicar optimizaciones** (índices adicionales, ajuste de pool, caching)
6. ➡️ [FASE 7.4 - UAT (Pruebas de Aceptación)](fase7.4_resumen.md)

---

**Responsable:** Equipo de Desarrollo  
**Revisado por:** Tech Lead  
**Estado:** ⚙️ Infraestructura lista, pendiente ejecución
