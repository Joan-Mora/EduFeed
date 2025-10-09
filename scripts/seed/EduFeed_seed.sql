-- EduFeed — Semilla de datos de ejemplo para QA (PostgreSQL)
-- Fecha: 2025-10-08
-- Requiere esquema ya creado y extensión pgcrypto

BEGIN;

-- 1) Usuarios
INSERT INTO usuarios (id, documento, nombre_completo, tipo_usuario, email, telefono, activo)
VALUES
  (gen_random_uuid(), '1001', 'Ana Niño',        'NINO',       'ana@colegio.edu',     '3001110001', true),
  (gen_random_uuid(), '1002', 'Bruno Estudiante','ESTUDIANTE', 'bruno@colegio.edu',   '3002220002', true),
  (gen_random_uuid(), '1003', 'Carla Docente',   'DOCENTE',    'carla@colegio.edu',   '3003330003', true),
  (gen_random_uuid(), '1004', 'Diego Personal',  'PERSONAL',   'diego@colegio.edu',   '3004440004', true);

-- 2) Plantillas biométricas (mock)
INSERT INTO plantillas_biometricas (usuario_id, proveedor, modalidad, plantilla, activo)
SELECT u.id, 'MOCK', 'HUELLA', decode('DEADBEEF', 'hex'), true FROM usuarios u WHERE u.documento IN ('1001','1002');
INSERT INTO plantillas_biometricas (usuario_id, proveedor, modalidad, plantilla, activo)
SELECT u.id, 'MOCK', 'ROSTRO', decode('DEADBEEF', 'hex'), true FROM usuarios u WHERE u.documento IN ('1003');

-- 3) Calendario de servicio (15 días alrededor de hoy)
INSERT INTO calendario_servicio (fecha, activo)
SELECT (current_date + gs)::date, true
FROM generate_series(-7, 7) gs
ON CONFLICT (fecha) DO NOTHING;

-- 4) Pagos
-- 4.1 Mensualidad activa para Ana (vigente este mes)
WITH u AS (SELECT id FROM usuarios WHERE documento='1001')
INSERT INTO pagos (usuario_id, monto, tipo_pago, creado_en, vigente_desde, vigente_hasta, metodo_pago, estado_pago, referencia_externa, cajero)
SELECT u.id, 120000, 'MENSUAL', now() - INTERVAL '10 days', date_trunc('month', now()), (date_trunc('month', now()) + INTERVAL '1 month' - INTERVAL '1 day'), 'EFECTIVO', 'APROBADO', 'REF-ANA-001', 'cajero1' FROM u;

-- 4.2 Paquete de 5 días para Bruno
WITH u AS (SELECT id FROM usuarios WHERE documento='1002')
INSERT INTO pagos (usuario_id, monto, tipo_pago, creado_en, vigente_desde, vigente_hasta, metodo_pago, estado_pago, referencia_externa, cajero)
SELECT u.id, 50000, 'PAQUETE', now() - INTERVAL '3 days', current_date - INTERVAL '1 day', current_date + INTERVAL '30 days', 'TARJETA', 'APROBADO', 'REF-BRU-001', 'cajero2' FROM u;

-- 4.3 Pago diario para Carla (hoy)
WITH u AS (SELECT id FROM usuarios WHERE documento='1003')
INSERT INTO pagos (usuario_id, monto, tipo_pago, creado_en, vigente_desde, vigente_hasta, metodo_pago, estado_pago, referencia_externa, cajero)
SELECT u.id, 12000, 'DIARIO', now(), current_date::timestamptz, (current_date::timestamptz + INTERVAL '1 day'), 'EFECTIVO', 'APROBADO', 'REF-CAR-001', 'cajero1' FROM u;

-- 5) Paquetes
INSERT INTO paquetes_pago (pago_id, dias, dias_restantes)
SELECT p.id, 5, 5 FROM pagos p WHERE p.referencia_externa='REF-BRU-001';

-- 6) Derechos de uso
-- Mensualidad (Ana)
WITH u AS (SELECT id FROM usuarios WHERE documento='1001'), p AS (SELECT id FROM pagos WHERE referencia_externa='REF-ANA-001')
INSERT INTO derechos_uso (usuario_id, tipo_derecho, pago_origen_id, vigente_desde, vigente_hasta, activo)
SELECT u.id, 'MENSUAL', p.id, date_trunc('month', now()), (date_trunc('month', now()) + INTERVAL '1 month' - INTERVAL '1 day'), true FROM u, p;

-- Paquete (Bruno)
WITH u AS (SELECT id FROM usuarios WHERE documento='1002'), p AS (SELECT id FROM pagos WHERE referencia_externa='REF-BRU-001')
INSERT INTO derechos_uso (usuario_id, tipo_derecho, pago_origen_id, vigente_desde, vigente_hasta, activo)
SELECT u.id, 'PAQUETE', p.id, current_date - INTERVAL '1 day', current_date + INTERVAL '30 days', true FROM u, p;

-- Diario (Carla)
WITH u AS (SELECT id FROM usuarios WHERE documento='1003'), p AS (SELECT id FROM pagos WHERE referencia_externa='REF-CAR-001')
INSERT INTO derechos_uso (usuario_id, tipo_derecho, pago_origen_id, vigente_desde, vigente_hasta, activo)
SELECT u.id, 'DIARIO', p.id, current_date::timestamptz, (current_date::timestamptz + INTERVAL '1 day'), true FROM u, p;

-- 7) Accesos de ejemplo
-- Ana: asistió 2 veces hoy (aprobado)
WITH u AS (SELECT id FROM usuarios WHERE documento='1001')
INSERT INTO accesos (usuario_id, fecha_hora, estado, modalidad, motivo, derecho_id)
SELECT u.id, date_trunc('day', now()) + INTERVAL '7 hour', 'APROBADO', 'HUELLA', NULL,
       (SELECT id FROM derechos_uso d WHERE d.usuario_id = u.id AND d.tipo_derecho='MENSUAL' AND d.activo LIMIT 1)
FROM u;
WITH u AS (SELECT id FROM usuarios WHERE documento='1001')
INSERT INTO accesos (usuario_id, fecha_hora, estado, modalidad, motivo, derecho_id)
SELECT u.id, date_trunc('day', now()) + INTERVAL '12 hour', 'APROBADO', 'ROSTRO', NULL,
       (SELECT id FROM derechos_uso d WHERE d.usuario_id = u.id AND d.tipo_derecho='MENSUAL' AND d.activo LIMIT 1)
FROM u;

-- Bruno: 1 asistencia ayer, ninguna hoy (para probar inasistencia)
WITH u AS (SELECT id FROM usuarios WHERE documento='1002')
INSERT INTO accesos (usuario_id, fecha_hora, estado, modalidad, motivo, derecho_id)
SELECT u.id, date_trunc('day', now()) - INTERVAL '1 day' + INTERVAL '8 hour', 'APROBADO', 'HUELLA', NULL,
       (SELECT id FROM derechos_uso d WHERE d.usuario_id = u.id AND d.tipo_derecho='PAQUETE' AND d.activo LIMIT 1)
FROM u;

-- Carla: 1 denegado hoy (sin derecho vinculado a propósito)
WITH u AS (SELECT id FROM usuarios WHERE documento='1003')
INSERT INTO accesos (usuario_id, fecha_hora, estado, modalidad, motivo, derecho_id)
SELECT u.id, date_trunc('day', now()) + INTERVAL '9 hour', 'DENEGADO', 'MANUAL', 'Sin coincidencia', NULL
FROM u;

-- 8) Usos de paquete (consumo de Bruno ayer)
INSERT INTO usos_paquete (paquete_id, acceso_id, usado_en)
SELECT pp.id,
       (
         SELECT a.id FROM accesos a
         JOIN derechos_uso d ON d.id = a.derecho_id AND d.tipo_derecho='PAQUETE'
         WHERE a.usuario_id = (SELECT id FROM usuarios WHERE documento='1002')
           AND a.estado='APROBADO'
           AND a.fecha_hora >= date_trunc('day', now()) - INTERVAL '1 day'
           AND a.fecha_hora <  date_trunc('day', now())
         LIMIT 1
       ),
       date_trunc('day', now()) - INTERVAL '1 day' + INTERVAL '9 hour'
FROM paquetes_pago pp
JOIN pagos p ON p.id = pp.pago_id AND p.referencia_externa='REF-BRU-001';

-- 9) Transacciones de caja
INSERT INTO transacciones_caja (proveedor, referencia_externa, monto, metodo_pago, estado, payload, recibido_en, conciliado, pago_id)
SELECT 'POS', 'REF-ANA-001', 120000, 'EFECTIVO', 'APROBADO', '{"ok":true}', now() - INTERVAL '10 days', true,
       (SELECT id FROM pagos WHERE referencia_externa='REF-ANA-001');
INSERT INTO transacciones_caja (proveedor, referencia_externa, monto, metodo_pago, estado, payload, recibido_en, conciliado, pago_id)
SELECT 'GATEWAY', 'REF-BRU-001', 50000, 'TARJETA', 'APROBADO', '{"ok":true}', now() - INTERVAL '3 days', true,
       (SELECT id FROM pagos WHERE referencia_externa='REF-BRU-001');

COMMIT;
