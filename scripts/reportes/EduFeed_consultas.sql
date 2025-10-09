-- EduFeed — Consultas y Reportes Clave (PostgreSQL)
-- Fecha: 2025-10-08
-- Esquema base referenciado: EduFeed_DB.sql
-- Notación de parámetros: :param (sustituir según cliente o ORM)

BEGIN;

/* =============================
   1) Operación diaria (ingreso)
   ============================= */

-- 1.1 Buscar usuario por texto, tipo y activo
-- Params: :q (texto), :tipo (NINO|ESTUDIANTE|DOCENTE|PERSONAL|NULL), :activo (true|false|NULL)
-- Ordena por nombre para fácil UI
SELECT u.*
FROM usuarios u
WHERE (:q IS NULL OR u.documento ILIKE '%'||:q||'%' OR u.nombre_completo ILIKE '%'||:q||'%')
  AND (:tipo IS NULL OR u.tipo_usuario = :tipo)
  AND (:activo IS NULL OR u.activo = :activo)
ORDER BY u.nombre_completo;

-- 1.2 Derecho vigente ahora (para control de acceso)
-- Params: :usuario_id (UUID)
SELECT d.*
FROM derechos_uso d
WHERE d.usuario_id = :usuario_id
  AND d.activo = true
  AND d.vigente_desde <= now()
  AND (d.vigente_hasta IS NULL OR d.vigente_hasta >= now())
ORDER BY d.vigente_desde DESC
LIMIT 1;

-- 1.3 Paquete disponible (con días restantes)
-- Params: :usuario_id (UUID)
SELECT pp.*, p.creado_en
FROM derechos_uso d
JOIN pagos p           ON p.id = d.pago_origen_id
JOIN paquetes_pago pp  ON pp.pago_id = p.id
WHERE d.usuario_id = :usuario_id
  AND d.tipo_derecho = 'PAQUETE'
  AND d.activo = true
  AND (d.vigente_hasta IS NULL OR d.vigente_hasta >= now())
  AND pp.dias_restantes > 0
ORDER BY p.creado_en DESC
LIMIT 1;

-- 1.4 Registrar acceso (vincula derecho vigente)
-- Params: :usuario_id, :estado ('APROBADO'|'DENEGADO'), :motivo, :modalidad ('HUELLA'|'ROSTRO'|'VOZ'|'MANUAL'), :metadatos_coincidencia (JSONB)
INSERT INTO accesos (usuario_id, estado, motivo, modalidad, metadatos_coincidencia, derecho_id)
VALUES (
  :usuario_id,
  :estado,
  :motivo,
  :modalidad,
  :metadatos_coincidencia,
  (
    SELECT d.id FROM derechos_uso d
    WHERE d.usuario_id = :usuario_id
      AND d.activo = true
      AND d.vigente_desde <= now()
      AND (d.vigente_hasta IS NULL OR d.vigente_hasta >= now())
    ORDER BY d.vigente_desde DESC
    LIMIT 1
  )
)
RETURNING id;

-- 1.5 Asistencias del día
SELECT a.*
FROM accesos a
WHERE a.estado = 'APROBADO'
  AND a.fecha_hora >= date_trunc('day', now())
  AND a.fecha_hora <  date_trunc('day', now()) + INTERVAL '1 day'
ORDER BY a.fecha_hora DESC;

/* =====================================
   2) Reportes de asistencia/inasistencia
   ===================================== */

-- 2.1 Asistencias por rango y modalidad (agregado por día)
-- Params: :desde, :hasta, :modalidad (NULL para todas)
SELECT a.usuario_id,
       date_trunc('day', a.fecha_hora) AS dia,
       COUNT(*) AS accesos
FROM accesos a
WHERE a.estado = 'APROBADO'
  AND a.fecha_hora >= :desde AND a.fecha_hora < :hasta
  AND (:modalidad IS NULL OR a.modalidad = :modalidad)
GROUP BY a.usuario_id, dia
ORDER BY dia, a.usuario_id;

-- 2.2 Inasistencias mensualistas hoy
SELECT u.id, u.nombre_completo
FROM derechos_uso d
JOIN usuarios u ON u.id = d.usuario_id
WHERE d.tipo_derecho = 'MENSUAL'
  AND d.activo = true
  AND d.vigente_desde::date <= current_date
  AND (d.vigente_hasta IS NULL OR d.vigente_hasta::date >= current_date)
  AND NOT EXISTS (
    SELECT 1 FROM accesos a
    WHERE a.usuario_id = u.id AND a.estado = 'APROBADO'
      AND a.fecha_hora >= date_trunc('day', now())
      AND a.fecha_hora <  date_trunc('day', now()) + INTERVAL '1 day'
  )
ORDER BY u.nombre_completo;

-- 2.3 Inasistencias mensualistas por rango (usa calendario_servicio)
-- Params: :desde::date, :hasta::date
WITH fechas AS (
  SELECT c.fecha
  FROM calendario_servicio c
  WHERE c.activo = true AND c.fecha BETWEEN :desde::date AND :hasta::date
)
SELECT f.fecha, u.id AS usuario_id, u.nombre_completo
FROM fechas f
JOIN derechos_uso d ON d.tipo_derecho = 'MENSUAL'
                   AND d.activo = true
                   AND d.vigente_desde::date <= f.fecha
                   AND (d.vigente_hasta IS NULL OR d.vigente_hasta::date >= f.fecha)
JOIN usuarios u ON u.id = d.usuario_id
WHERE NOT EXISTS (
  SELECT 1 FROM accesos a
  WHERE a.usuario_id = u.id AND a.estado = 'APROBADO'
    AND a.fecha_hora >= f.fecha::timestamptz
    AND a.fecha_hora <  (f.fecha::timestamptz + INTERVAL '1 day')
)
ORDER BY f.fecha, u.nombre_completo;

-- 2.4 Inasistencias de prepago (simplificada)
-- Params: :desde::date, :hasta::date
WITH fechas AS (
  SELECT c.fecha
  FROM calendario_servicio c
  WHERE c.activo = true AND c.fecha BETWEEN :desde::date AND :hasta::date
)
SELECT f.fecha, u.id AS usuario_id, u.nombre_completo
FROM fechas f
JOIN derechos_uso d ON d.tipo_derecho = 'PAQUETE'
                   AND d.activo = true
                   AND d.vigente_desde::date <= f.fecha
                   AND (d.vigente_hasta IS NULL OR d.vigente_hasta::date >= f.fecha)
JOIN usuarios u ON u.id = d.usuario_id
WHERE NOT EXISTS (
  SELECT 1 FROM accesos a
  WHERE a.usuario_id = u.id AND a.estado = 'APROBADO'
    AND a.fecha_hora >= f.fecha::timestamptz
    AND a.fecha_hora <  (f.fecha::timestamptz + INTERVAL '1 day')
)
ORDER BY f.fecha, u.nombre_completo;

-- 2.5 Asistencias por tipo de derecho
-- Params: :desde, :hasta
SELECT COALESCE(d.tipo_derecho, 'SIN_DERECHO') AS tipo_derecho,
       date_trunc('day', a.fecha_hora) AS dia,
       COUNT(*) AS asistencias
FROM accesos a
LEFT JOIN derechos_uso d ON d.id = a.derecho_id
WHERE a.estado = 'APROBADO'
  AND a.fecha_hora >= :desde AND a.fecha_hora < :hasta
GROUP BY dia, COALESCE(d.tipo_derecho, 'SIN_DERECHO')
ORDER BY dia;

/* ============================
   3) Pagos e ingresos (RF-05/10)
   ============================ */

-- 3.1 Ingresos por periodo, tipo y método
-- Params: :desde, :hasta, :granularidad ('day'|'month')
SELECT date_trunc(:granularidad, p.creado_en) AS periodo,
       p.tipo_pago, COALESCE(p.metodo_pago, 'DESCONOCIDO') AS metodo,
       COUNT(*) AS n_pagos, SUM(p.monto) AS total
FROM pagos p
WHERE p.estado_pago = 'APROBADO'
  AND p.creado_en >= :desde AND p.creado_en < :hasta
GROUP BY periodo, p.tipo_pago, COALESCE(p.metodo_pago, 'DESCONOCIDO')
ORDER BY periodo, p.tipo_pago, metodo;

-- 3.2 Ingresos por usuario y mes
-- Params: :desde, :hasta
SELECT u.id AS usuario_id, u.nombre_completo,
       date_trunc('month', p.creado_en) AS mes,
       SUM(p.monto) AS total, COUNT(*) AS n_pagos
FROM pagos p
JOIN usuarios u ON u.id = p.usuario_id
WHERE p.estado_pago = 'APROBADO'
  AND p.creado_en >= :desde AND p.creado_en < :hasta
GROUP BY u.id, u.nombre_completo, mes
ORDER BY mes, total DESC;

-- 3.3 Mensualidades por vencer en 7 días
SELECT d.*, u.nombre_completo
FROM derechos_uso d
JOIN usuarios u ON u.id = d.usuario_id
WHERE d.tipo_derecho = 'MENSUAL'
  AND d.activo = true
  AND d.vigente_hasta IS NOT NULL
  AND d.vigente_hasta BETWEEN now() AND (now() + INTERVAL '7 days')
ORDER BY d.vigente_hasta;

/* ==========================
   4) Conciliación con caja
   ========================== */

-- 4.1 Transacciones aprobadas sin pago asociado
SELECT t.*
FROM transacciones_caja t
WHERE t.estado = 'APROBADO' AND t.pago_id IS NULL;

-- 4.2 Pagos aprobados con referencia sin transacción
SELECT p.*
FROM pagos p
LEFT JOIN transacciones_caja t
  ON t.referencia_externa = p.referencia_externa
 AND (t.estado IN ('APROBADO','PENDIENTE'))
WHERE p.referencia_externa IS NOT NULL
  AND p.estado_pago = 'APROBADO'
  AND t.id IS NULL;

-- 4.3 Descuadre de montos por referencia
SELECT p.id AS pago_id, p.referencia_externa, p.monto AS monto_pago,
       t.id AS transaccion_id, t.monto AS monto_transaccion
FROM pagos p
JOIN transacciones_caja t ON t.referencia_externa = p.referencia_externa
WHERE p.estado_pago = 'APROBADO' AND t.estado = 'APROBADO' AND p.monto <> t.monto;

/* =============================
   5) Biometría y cobertura (RF-01/02)
   ============================= */

-- 5.1 Cobertura de plantillas por modalidad
SELECT modalidad,
       COUNT(*) FILTER (WHERE activo)      AS plantillas_activas,
       COUNT(*) FILTER (WHERE NOT activo)  AS plantillas_inactivas,
       COUNT(*)                            AS total
FROM plantillas_biometricas
GROUP BY modalidad
ORDER BY modalidad;

-- 5.2 Usuarios sin plantilla activa por modalidad
-- Params: :modalidad ('HUELLA'|'ROSTRO'|'VOZ')
SELECT u.id, u.nombre_completo
FROM usuarios u
WHERE u.activo = true
  AND NOT EXISTS (
    SELECT 1 FROM plantillas_biometricas pb
    WHERE pb.usuario_id = u.id AND pb.activo = true AND pb.modalidad = :modalidad
  )
ORDER BY u.nombre_completo;

/* ====================
   6) Auditoría (RF-11)
   ==================== */

-- 6.1 Cambios por rango y entidad
-- Params: :desde, :hasta, :entidad (NULL para todas)
SELECT a.*
FROM auditoria a
WHERE a.realizado_en >= :desde AND a.realizado_en < :hasta
  AND (:entidad IS NULL OR a.tipo_entidad = :entidad)
ORDER BY a.realizado_en DESC;

-- 6.2 Últimos cambios por actor
SELECT a.realizado_por, COUNT(*) AS cambios, MAX(a.realizado_en) AS ultimo
FROM auditoria a
GROUP BY a.realizado_por
ORDER BY ultimo DESC;

/* ====================
   7) KPIs y analítica
   ==================== */

-- 7.1 Tasa de asistencia diaria (mensualistas)
-- Params: :desde::date, :hasta::date
WITH fechas AS (
  SELECT c.fecha
  FROM calendario_servicio c
  WHERE c.activo = true AND c.fecha BETWEEN :desde::date AND :hasta::date
),
base AS (
  SELECT f.fecha, d.usuario_id
  FROM fechas f
  JOIN derechos_uso d ON d.tipo_derecho = 'MENSUAL'
                     AND d.activo = true
                     AND d.vigente_desde::date <= f.fecha
                     AND (d.vigente_hasta IS NULL OR d.vigente_hasta::date >= f.fecha)
),
asis AS (
  SELECT a.usuario_id, a.fecha_hora::date AS fecha
  FROM accesos a
  WHERE a.estado = 'APROBADO'
)
SELECT f.fecha,
       COUNT(DISTINCT b.usuario_id) AS base_mensualistas,
       COUNT(DISTINCT b.usuario_id) FILTER (
         WHERE EXISTS (
           SELECT 1 FROM asis s WHERE s.usuario_id = b.usuario_id AND s.fecha = f.fecha
         )
       ) AS asistieron,
       ROUND(
         100.0 * COUNT(DISTINCT b.usuario_id) FILTER (
           WHERE EXISTS (
             SELECT 1 FROM asis s WHERE s.usuario_id = b.usuario_id AND s.fecha = f.fecha
           )
         ) / NULLIF(COUNT(DISTINCT b.usuario_id), 0), 2
       ) AS tasa_asistencia_pct
FROM fechas f
LEFT JOIN base b ON b.fecha = f.fecha
GROUP BY f.fecha
ORDER BY f.fecha;

-- 7.2 Distribución horaria de accesos
-- Params: :desde, :hasta
SELECT EXTRACT(HOUR FROM a.fecha_hora) AS hora,
       COUNT(*) FILTER (WHERE a.estado='APROBADO') AS aprobados,
       COUNT(*) FILTER (WHERE a.estado='DENEGADO') AS denegados,
       COUNT(*) AS total
FROM accesos a
WHERE a.fecha_hora >= :desde AND a.fecha_hora < :hasta
GROUP BY hora
ORDER BY hora;

-- 7.3 Motivos de denegación más frecuentes
-- Params: :desde, :hasta
SELECT a.motivo, COUNT(*) AS veces
FROM accesos a
WHERE a.estado = 'DENEGADO'
  AND a.fecha_hora >= :desde AND a.fecha_hora < :hasta
GROUP BY a.motivo
ORDER BY veces DESC NULLS LAST
LIMIT 20;

/* ================================
   8) Gestión y calidad de datos
   ================================ */

-- 8.1 Duplicidades de plantillas activas por usuario/modalidad
SELECT pb.usuario_id, pb.modalidad, COUNT(*) AS activas
FROM plantillas_biometricas pb
WHERE pb.activo = true
GROUP BY pb.usuario_id, pb.modalidad
HAVING COUNT(*) > 1
ORDER BY activas DESC;

-- 8.2 Usuarios con documento nulo o patrón atípico
SELECT u.*
FROM usuarios u
WHERE u.documento IS NULL OR u.documento !~ '^[0-9A-Za-z.-]+$';

-- 8.3 Pagos aprobados sin derecho asociado
SELECT p.*
FROM pagos p
LEFT JOIN derechos_uso d ON d.pago_origen_id = p.id
WHERE p.estado_pago = 'APROBADO' AND d.id IS NULL;

-- 8.4 Accesos aprobados sin derecho vinculado
-- Params: :desde, :hasta
SELECT a.*
FROM accesos a
WHERE a.estado = 'APROBADO' AND a.derecho_id IS NULL
  AND a.fecha_hora >= :desde AND a.fecha_hora < :hasta;

COMMIT;
