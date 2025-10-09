# V2 — Registro de cambios (08-10-2025)

Este documento detalla TODOS los cambios realizados hoy en el proyecto, con especificación exacta de archivos, objetos creados y modificaciones.

## 1) Estructura de reportes y consultas SQL

### 1.1 Nuevo: `scripts/reportes/EduFeed_consultas.sql`
Se creó un catálogo de consultas clave, organizado por secciones, listo para uso con parámetros tipo `:desde`, `:hasta`, `:usuario_id`, `:modalidad`, etc.

Secciones principales y propósito:
- Operación diaria:
  - Búsqueda de usuarios por texto/tipo/activo.
  - Derecho vigente ahora (para control de acceso).
  - Paquete disponible con días restantes.
  - Insert de acceso (aprobado/denegado) vinculando derecho actual.
  - Asistencias del día.
- Asistencia/Inasistencias:
  - Asistencias por rango y modalidad.
  - Inasistencias mensualistas (hoy y por rango usando `calendario_servicio`).
  - Inasistencias de prepago (versión simple) y nota de versión precisa.
  - Asistencias por tipo de derecho.
- Pagos e ingresos:
  - Ingresos por periodo (day|month), tipo y método.
  - Ingresos por usuario y mes.
  - Mensualidades por vencer en 7 días.
- Conciliación con caja:
  - Transacciones aprobadas sin pago asociado.
  - Pagos aprobados con referencia sin transacción.
  - Descuadres de montos por referencia.
- Biometría:
  - Cobertura de plantillas por modalidad.
  - Usuarios sin plantilla activa por modalidad.
- Auditoría:
  - Cambios por rango y entidad.
  - Últimos cambios por actor.
- KPIs y analítica:
  - Tasa de asistencia diaria (mensualistas) sobre `calendario_servicio`.
  - Distribución horaria de accesos.
  - Motivos de denegación más frecuentes.
- Gestión/calidad de datos:
  - Duplicidades de plantillas.
  - Documentos atípicos.
  - Pagos aprobados sin derecho.
  - Accesos aprobados sin derecho vinculado.

Optimizaciones realizadas sobre este archivo (para uso de índices/sargabilidad):
- Se reemplazaron comparaciones con `a.fecha_hora::date = ...` por rangos de tiempo, en 4 consultas específicas:
  1) "Asistencias del día" ahora usa:
     - `a.fecha_hora >= date_trunc('day', now()) AND a.fecha_hora < date_trunc('day', now()) + INTERVAL '1 day'`.
  2) "Inasistencias mensualistas hoy" — subconsulta NOT EXISTS ahora filtra por rango del día actual con `date_trunc`.
  3) "Inasistencias mensualistas por rango" — el NOT EXISTS usa `a.fecha_hora >= f.fecha::timestamptz AND a.fecha_hora < (f.fecha::timestamptz + INTERVAL '1 day')`.
  4) "Inasistencias de prepago por rango" — mismo patrón de rango para el NOT EXISTS.

### 1.2 Nuevo: `scripts/reportes/EduFeed_vistas.sql`
Se añadieron objetos de soporte a reportes frecuentes:
- Vista: `vw_derechos_vigentes_hoy`
  - Consulta de derechos activos hoy (vigencias y activo=true).
- Vista materializada: `mv_ingresos_diarios`
  - Agregado de ingresos por día, tipo de pago y método.
  - Creada con `WITH NO DATA` (requiere `REFRESH` para poblar).
  - Índices creados:
    - `uq_mv_ingresos_diarios (dia, tipo_pago, metodo)` (único) para permitir `REFRESH CONCURRENTLY`.
    - `idx_mv_ingresos_diarios_dia (dia DESC)` para lectura ordenada.
- Vista: `vw_asistencias_por_dia`
  - Asistencias por día y tipo de derecho (incluye 'SIN_DERECHO' si no hay vínculo).

Notas de operación incluidas en el archivo:
- REFRESH inicial: `REFRESH MATERIALIZED VIEW mv_ingresos_diarios;`
- REFRESH no bloqueante: `REFRESH MATERIALIZED VIEW CONCURRENTLY mv_ingresos_diarios;`

## 2) Índices compuestos para performance

Se añadieron índices compuestos en ambos esquemas (Flyway y SQL portátil):

Archivos afectados:
- `edufeed-backend/src/main/resources/db/migration/V1__init.sql`
- `EduFeed_DB.sql`

Índices agregados:
- En `accesos`:
  - `CREATE INDEX IF NOT EXISTS idx_accesos_usuario_estado_fecha ON accesos (usuario_id, estado, fecha_hora DESC);`
  - Propósito: acelerar filtros combinados por usuario + estado + rango temporal.
- En `pagos`:
  - `CREATE INDEX IF NOT EXISTS idx_pagos_estado_creado ON pagos (estado_pago, creado_en DESC);`
  - Propósito: acelerar reportes y listados por estado de pago en un rango de tiempo.

(Se mantienen además los índices ya existentes: `idx_accesos_usuario_fecha`, `idx_pagos_usuario_creado`, `idx_derechos_usuario_activo`, `idx_plantillas_usuario`, `idx_accesos_estado_fecha`, `idx_derechos_usuario_vigencias`, `idx_pagos_tipo_creado` y únicos sobre referencias.)

## 3) Semilla de datos para QA

### 3.1 Nuevo: `scripts/seed/EduFeed_seed.sql`
Semilla realista para validar reportes y flujos end-to-end. Incluye:
- Usuarios (4): Niño, Estudiante, Docente, Personal.
- Plantillas biométricas mock (`HUELLA` y `ROSTRO`).
- Calendario de servicio: 15 días (±7 días alrededor de hoy).
- Pagos:
  - Mensualidad activa (Ana) — `APROBADO` con vigencia mes actual.
  - Paquete de 5 días (Bruno) — `APROBADO`, vigencia 30 días.
  - Diario (Carla) — `APROBADO`, vigencia hoy.
- Paquetes:
  - Registro de `paquetes_pago` para Bruno con 5 días.
- Derechos de uso:
  - MENSUAL (Ana), PAQUETE (Bruno), DIARIO (Carla).
- Accesos:
  - Ana: 2 aprobados hoy (distintas horas y modalidades).
  - Bruno: 1 aprobado ayer (para probar inasistencia hoy).
  - Carla: 1 denegado hoy (sin derecho vinculado a propósito).
- Usos de paquete:
  - Consumo de Bruno (ayer), enlazado a su acceso aprobado.
- Transacciones de caja:
  - POS/EFECTIVO (Ana) y GATEWAY/TARJETA (Bruno), ambas `APROBADO` y conciliadas.

Se usan tablas temporales (`TEMP TABLE`) para capturar IDs de inserciones y asociarlas correctamente en cascada.

## 4) Backend — Zona horaria parametrizada

Archivo modificado: `edufeed-backend/src/main/resources/application.yml`
- Se añadió `app.timezone` (por defecto: `America/Bogota`).
- `spring.datasource.hikari.connectionInitSql`: `SET TIME ZONE '${app.timezone}'` (fija TZ por sesión en PostgreSQL).
- `spring.jpa.properties.hibernate.jdbc.time_zone: UTC` (driver consistente).
- `spring.jackson.time-zone: ${app.timezone}` (serialización JSON consistente).

Archivo agregado: `edufeed-backend/src/main/java/co/cellano/edufeed/backend/config/TimeZoneConfig.java`
- Configuración Spring que fija la zona horaria por defecto de la JVM al arrancar según `app.timezone` y la registra en logs.
- Nota: El archivo tuvo ediciones manuales posteriores el mismo día (realizadas por el usuario). La función final permanece: establecer la TZ de la JVM desde configuración.

## 5) Directorios nuevos
- `scripts/reportes/`
- `scripts/seed/`

## 6) Notas de uso y operación
- Para refrescar la MV de ingresos diarios sin bloquear lecturas:
  - `REFRESH MATERIALIZED VIEW CONCURRENTLY mv_ingresos_diarios;`
- Para poblar datos de QA:
  - Ejecutar `scripts/seed/EduFeed_seed.sql` después de tener el esquema creado.
- Para cambiar zona horaria del backend sin recompilar:
  - Variable de entorno `APP_TIMEZONE` (p.ej. `America/Bogota`, `UTC`).

## 7) Impacto
- Reportes más rápidos y consistentes (vistas y MV).
- Consultas sargables que aprovechan índices.
- Mejor experiencia de QA con semilla realista.
- Cortes diarios coherentes con la TZ configurada (DB + JVM + JSON).
