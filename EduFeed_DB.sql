-- Esquema base en español consolidado en un solo archivo (PostgreSQL 16)

-- Extensión para generar UUIDs
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1) Usuarios del sistema (estudiantes, docentes, personal)
CREATE TABLE IF NOT EXISTS usuarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    documento VARCHAR(50) UNIQUE NOT NULL,
    nombre_completo VARCHAR(200) NOT NULL,
    tipo_usuario VARCHAR(30) NOT NULL, -- NINO | ESTUDIANTE | DOCENTE | PERSONAL
    email VARCHAR(200),
    telefono VARCHAR(30),
    activo BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT chk_usuarios_tipo CHECK (tipo_usuario IN ('NINO','ESTUDIANTE','DOCENTE','PERSONAL'))
);

-- 2) Plantillas biométricas (cifradas) por usuario
CREATE TABLE IF NOT EXISTS plantillas_biometricas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    proveedor VARCHAR(100),
    modalidad VARCHAR(20) NOT NULL, -- HUELLA | ROSTRO | VOZ
    plantilla BYTEA NOT NULL,       -- datos binarios (idealmente cifrados a nivel app)
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    activo BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT chk_plantillas_modalidad CHECK (modalidad IN ('HUELLA','ROSTRO','VOZ'))
);

-- 3) Pagos registrados (diario, mensual o paquetes)
CREATE TABLE IF NOT EXISTS pagos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES usuarios(id),
    monto NUMERIC(12,2) NOT NULL,
    tipo_pago VARCHAR(20) NOT NULL,     -- DIARIO | MENSUAL | PAQUETE
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    vigente_desde TIMESTAMPTZ,
    vigente_hasta TIMESTAMPTZ,
    metadatos JSONB,
    -- Integración con caja
    metodo_pago VARCHAR(20),            -- EFECTIVO | TARJETA | TRANSFERENCIA | etc.
    estado_pago VARCHAR(20),            -- PENDIENTE | APROBADO | RECHAZADO
    referencia_externa VARCHAR(100),
    cajero VARCHAR(100),
    CONSTRAINT chk_pagos_tipo CHECK (tipo_pago IN ('DIARIO','MENSUAL','PAQUETE')),
    CONSTRAINT chk_pagos_vigencias CHECK (vigente_hasta IS NULL OR vigente_hasta > vigente_desde)
);

-- 4) Detalle de paquetes (prepago de días)
CREATE TABLE IF NOT EXISTS paquetes_pago (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pago_id UUID NOT NULL REFERENCES pagos(id) ON DELETE CASCADE,
    dias INTEGER NOT NULL,
    dias_restantes INTEGER NOT NULL
);

-- 5) Derechos efectivos (lo que permite o no el acceso)
CREATE TABLE IF NOT EXISTS derechos_uso (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    tipo_derecho VARCHAR(20) NOT NULL,  -- DIARIO | MENSUAL | PAQUETE
    pago_origen_id UUID REFERENCES pagos(id),
    vigente_desde TIMESTAMPTZ NOT NULL,
    vigente_hasta TIMESTAMPTZ,
    activo BOOLEAN NOT NULL DEFAULT true,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_derechos_tipo CHECK (tipo_derecho IN ('DIARIO','MENSUAL','PAQUETE')),
    CONSTRAINT chk_derechos_vigencias CHECK (vigente_hasta IS NULL OR vigente_hasta > vigente_desde)
);

-- 6) Registro de accesos (aprobados/denegados)
CREATE TABLE IF NOT EXISTS accesos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID REFERENCES usuarios(id),
    fecha_hora TIMESTAMPTZ NOT NULL DEFAULT now(),
    estado VARCHAR(20) NOT NULL,        -- APROBADO | DENEGADO
    motivo VARCHAR(200),
    modalidad VARCHAR(20),              -- HUELLA | ROSTRO | VOZ | MANUAL
    metadatos_coincidencia JSONB,
    derecho_id UUID REFERENCES derechos_uso(id) ON DELETE SET NULL,
    CONSTRAINT chk_accesos_estado CHECK (estado IN ('APROBADO','DENEGADO')),
    CONSTRAINT chk_accesos_modalidad CHECK (modalidad IN ('HUELLA','ROSTRO','VOZ','MANUAL'))
);

-- 7) Trazabilidad de consumo de paquetes
CREATE TABLE IF NOT EXISTS usos_paquete (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    paquete_id UUID NOT NULL REFERENCES paquetes_pago(id) ON DELETE CASCADE,
    acceso_id UUID REFERENCES accesos(id) ON DELETE SET NULL,
    usado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_usos_paquete_acceso ON usos_paquete (acceso_id);

-- 8) Auditoría de cambios administrativos
CREATE TABLE IF NOT EXISTS auditoria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo_entidad VARCHAR(50) NOT NULL,
    entidad_id UUID,
    accion VARCHAR(20) NOT NULL,        -- CREATE | UPDATE | DELETE
    realizado_por VARCHAR(200),
    realizado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    valores_anteriores JSONB,
    valores_nuevos JSONB,
    reason TEXT
);

-- 9) Índices útiles
CREATE INDEX IF NOT EXISTS idx_accesos_usuario_fecha ON accesos (usuario_id, fecha_hora DESC);
-- Índice compuesto para filtros por usuario + estado + rango temporal
CREATE INDEX IF NOT EXISTS idx_accesos_usuario_estado_fecha ON accesos (usuario_id, estado, fecha_hora DESC);
CREATE INDEX IF NOT EXISTS idx_pagos_usuario_creado ON pagos (usuario_id, creado_en DESC);
-- Índice compuesto para filtros por estado_pago + rango temporal
CREATE INDEX IF NOT EXISTS idx_pagos_estado_creado ON pagos (estado_pago, creado_en DESC);
CREATE INDEX IF NOT EXISTS idx_derechos_usuario_activo ON derechos_uso (usuario_id, activo, vigente_hasta);
CREATE INDEX IF NOT EXISTS idx_plantillas_usuario ON plantillas_biometricas (usuario_id);

-- 10) Roles y asignaciones de usuarios a roles (para administración/entregables)
CREATE TABLE IF NOT EXISTS roles (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        nombre VARCHAR(50) UNIQUE NOT NULL,
        descripcion VARCHAR(200),
        creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS usuarios_roles (
        usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
        rol_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
        asignado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
        PRIMARY KEY (usuario_id, rol_id)
);

-- 11) Integración y conciliación con caja
CREATE TABLE IF NOT EXISTS transacciones_caja (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        proveedor VARCHAR(60),
        referencia_externa VARCHAR(100),
        monto NUMERIC(12,2) NOT NULL,
        metodo_pago VARCHAR(20),
        estado VARCHAR(20),              -- PENDIENTE | APROBADO | RECHAZADO | ANULADO
        payload JSONB,                   -- request/response crudos del proveedor
        recibido_en TIMESTAMPTZ NOT NULL DEFAULT now(),
        conciliado BOOLEAN NOT NULL DEFAULT false,
        pago_id UUID REFERENCES pagos(id) ON DELETE SET NULL
);

-- Unicidad por proveedor+referencia cuando exista referencia
CREATE UNIQUE INDEX IF NOT EXISTS uq_transacciones_proveedor_ref
    ON transacciones_caja (proveedor, referencia_externa)
    WHERE referencia_externa IS NOT NULL;

-- Aceleradores de consulta
CREATE INDEX IF NOT EXISTS idx_transacciones_estado ON transacciones_caja (estado);
CREATE INDEX IF NOT EXISTS idx_transacciones_proveedor ON transacciones_caja (proveedor);

-- Evitar referencias externas duplicadas en pagos
CREATE UNIQUE INDEX IF NOT EXISTS uq_pagos_referencia_externa
    ON pagos (referencia_externa)
    WHERE referencia_externa IS NOT NULL;

-- Validación de estado de pago
DO $$ BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.constraint_column_usage ccu
        WHERE ccu.table_name='pagos' AND ccu.constraint_name='chk_pagos_estado') THEN
        ALTER TABLE pagos
            ADD CONSTRAINT chk_pagos_estado
            CHECK (estado_pago IS NULL OR estado_pago IN ('PENDIENTE','APROBADO','RECHAZADO'));
    END IF;
END $$;

-- 12) Calendario de servicio para reportes de inasistencias
CREATE TABLE IF NOT EXISTS calendario_servicio (
    fecha DATE PRIMARY KEY,
    activo BOOLEAN NOT NULL DEFAULT true,
    observacion TEXT
);

-- 13) Índices adicionales de soporte a reportes
CREATE INDEX IF NOT EXISTS idx_accesos_estado_fecha ON accesos (estado, fecha_hora DESC);
CREATE INDEX IF NOT EXISTS idx_derechos_usuario_vigencias ON derechos_uso (usuario_id, vigente_desde DESC, vigente_hasta DESC);
CREATE INDEX IF NOT EXISTS idx_pagos_tipo_creado ON pagos (tipo_pago, creado_en DESC);
