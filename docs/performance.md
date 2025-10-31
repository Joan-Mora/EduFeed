# FASE 7.3 · Pruebas de carga y performance

Este documento describe cómo ejecutar pruebas de carga para EduFeed y verificar los objetivos de latencia p95 por endpoint.

## Objetivos
- Concurrencia: 500 usuarios activos sin degradación
- Latencias (p95):
  - POST /api/accesos/verificar < 2s
  - Operaciones de pagos (<=> /api/pagos/*) < 1s
  - Reportes (/api/reportes/*) < 3s

## Preparación
1) Levantar base de datos
- VS Code Task: "DB: up"

2) Iniciar backend (perfil dev)
- VS Code Task: "Backend: run" (el puerto por defecto es 8080)
- Credenciales sembradas (por defecto): admin / Admin123$

3) Variables útiles
- Base URL: http://localhost:8080
- Usuario Admin: configurable vía SEED_OPERATOR_USERNAME / SEED_OPERATOR_PASSWORD

## Opción A: Gatling (recomendada)
Se incluye un módulo opcional `edufeed-perf` con 3 simulaciones:
- AccessCheckSimulation: POST /api/accesos/verificar
- PaymentsSimulation: creación y listado de pagos (GET /api/pagos/estado/APROBADO)
- ReportsSimulation: GET /api/reportes/ingresos

Notas:
- El módulo `edufeed-perf` NO está agregado al build raíz; ejecútalo de forma independiente con `-pl edufeed-perf`.
- Autenticación: cada simulación hace login y reutiliza el accessToken (Bearer).

Parámetros
- `-DbaseUrl=http://localhost:8080`
- `-Dusers=500` número de usuarios virtuales
- `-DrampSeconds=60` rampa de llegada
- `-DholdSeconds=300` fase de sostenimiento (usado para calcular p95 estable)

Ejemplos (PowerShell)

```powershell
# AccessCheck, 500 usuarios
$env:JAVA_HOME='C:\Program Files\Java\jdk-24'
$base='http://localhost:8080'
$mvn="$env:USERPROFILE\tools\maven\apache-maven-3.9.9\bin\mvn.cmd"
& $mvn -pl edufeed-perf -am gatling:test `
  -Dgatling.simulationClass=co.cellano.edufeed.perf.AccessCheckSimulation `
  -DbaseUrl=$base -Dusers=500 -DrampSeconds=60 -DholdSeconds=300

# Payments
& $mvn -pl edufeed-perf -am gatling:test `
  -Dgatling.simulationClass=co.cellano.edufeed.perf.PaymentsSimulation `
  -DbaseUrl=$base -Dusers=500 -DrampSeconds=60 -DholdSeconds=300

# Reports
& $mvn -pl edufeed-perf -am gatling:test `
  -Dgatling.simulationClass=co.cellano.edufeed.perf.ReportsSimulation `
  -DbaseUrl=$base -Dusers=500 -DrampSeconds=60 -DholdSeconds=300
```

Asserts
- Cada simulación incluye asserts de p95 (percentile3) y 99%+ de éxito.
- Los reportes HTML quedan en `edufeed-perf/target/gatling/<simulacion>-<timestamp>/index.html`.

## Opción B: JMeter
Si prefieres JMeter:
- Crea un Test Plan con:
  - HTTP Request Defaults (Base URL)
  - HTTP Header Manager (Content-Type: application/json)
  - Login (POST /api/auth/login) + JSON Extractor (accessToken -> var)
  - Header Manager hijo para “Authorization: Bearer ${accessToken}”
  - Samplers según endpoint objetivo (ver arriba)
  - Concurrency Thread Group: 500 usuarios, rampa 60s, hold 300s
  - Backend Listener o Summary Report para p95

CLI (ejemplo):
```powershell
jmeter -n -t edufeed-load.jmx -l results.jtl -e -o report
```

## Optimización (índices SQL)
Se agregó `V6__perf_indexes.sql` (Flyway) con índices sugeridos para consultas frecuentes:
- pagos: estado_pago + creado_en, usuario_id + creado_en, tipo_pago + creado_en, referencia_externa
- accesos: (usuario_id, fecha_hora), (estado, fecha_hora), (modalidad, fecha_hora)
- derechos_uso: (activo, usuario_id), (vigente_desde, vigente_hasta)

Aplica migraciones reiniciando el backend (Flyway valida y migra al arranque).

## Interpretación de resultados
- Objetivo cumplido si los asserts pasan y el reporte HTML muestra p95 <= objetivo para cada endpoint medido.
- Si no se cumple:
  - Revisa `Explain Analyze` de las consultas implicadas.
  - Verifica que las migraciones de índices estén aplicadas.
  - Aumenta el pool Hikari si hay espera por conexiones.
  - Ajusta paginación en listados grandes.

## Troubleshooting
- Puerto 8080 en uso: cierra procesos previos o cambia `server.port`.
- 401/403: confirma credenciales admin de `application.yml` o variables de entorno.
- Errores 5xx durante carga: revisa logs `logs/edufeed-backend.log` y métricas en `/actuator/metrics`.
