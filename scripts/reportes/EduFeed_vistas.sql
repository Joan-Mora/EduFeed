-- EduFeed — Vistas y Vistas Materializadas de Reportes Frecuentes (PostgreSQL)
-- Fecha: 2025-10-08
-- Requiere esquema creado (ver EduFeed_DB.sql)

/* ==============================================
   1) Derechos vigentes hoy (vista normal)
   ============================================== */
CREATE OR REPLACE VIEW vw_derechos_vigentes_hoy AS
SELECT d.*
FROM derechos_uso d
WHERE d.activo = true
  AND d.vigente_desde <= now()
  AND (d.vigente_hasta IS NULL OR d.vigente_hasta >= now());

/* ======================================================
   2) Ingresos diarios (vista materializada agregada)
   - Ideal para dashboards. Actualizar con REFRESH.
   - Se crea índice único para permitir REFRESH CONCURRENTLY.
   ====================================================== */
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_ingresos_diarios AS
SELECT (p.creado_en::date)                      AS dia,
       p.tipo_pago,
       COALESCE(p.metodo_pago, 'DESCONOCIDO')   AS metodo,
       COUNT(*)                                  AS n_pagos,
       SUM(p.monto)                              AS total
FROM pagos p
WHERE p.estado_pago = 'APROBADO'
GROUP BY dia, p.tipo_pago, COALESCE(p.metodo_pago, 'DESCONOCIDO')
WITH NO DATA;  -- poblar luego con REFRESH

-- Índice único para habilitar REFRESH CONCURRENTLY
CREATE UNIQUE INDEX IF NOT EXISTS uq_mv_ingresos_diarios
  ON mv_ingresos_diarios (dia, tipo_pago, metodo);

-- Índice auxiliar para ordenación por día
CREATE INDEX IF NOT EXISTS idx_mv_ingresos_diarios_dia
  ON mv_ingresos_diarios (dia DESC);

/* ==================================================
  3) Asistencias por día y tipo de derecho (vista)
  - Agregado que se usa a menudo en reportes operativos.
  - Se hace DROP para permitir cambiar tipo de columna (timestamp -> date).
  ================================================== */
DROP VIEW IF EXISTS vw_asistencias_por_dia;
CREATE OR REPLACE VIEW vw_asistencias_por_dia AS
SELECT a.fecha_hora::date                          AS dia,
       COALESCE(d.tipo_derecho, 'SIN_DERECHO')    AS tipo_derecho,
       COUNT(*)                                   AS asistencias
FROM accesos a
LEFT JOIN derechos_uso d ON d.id = a.derecho_id
WHERE a.estado = 'APROBADO'
GROUP BY dia, COALESCE(d.tipo_derecho, 'SIN_DERECHO');

/* =========================
   Notas de uso/operación:
   =========================
   - Población inicial MV ingresos diarios:
       REFRESH MATERIALIZED VIEW mv_ingresos_diarios;

   - Para actualizar sin bloquear lecturas:
       REFRESH MATERIALIZED VIEW CONCURRENTLY mv_ingresos_diarios;
     (Requiere el índice único creado arriba y suficientes recursos.)

   - Consultas típicas:
       SELECT * FROM vw_derechos_vigentes_hoy;
       SELECT * FROM mv_ingresos_diarios WHERE dia BETWEEN :desde AND :hasta ORDER BY dia;
       SELECT * FROM vw_asistencias_por_dia WHERE dia BETWEEN :desde AND :hasta ORDER BY dia;
*/
