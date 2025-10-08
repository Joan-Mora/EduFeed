-- Extensión para generar UUIDs
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Usuarios del sistema (estudiantes, docentes, personal)
CREATE TABLE IF NOT EXISTS app_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    document VARCHAR(50) UNIQUE NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    user_type VARCHAR(30) NOT NULL, -- STUDENT/TEACHER/STAFF
    email VARCHAR(200),
    phone VARCHAR(30),
    active BOOLEAN NOT NULL DEFAULT true
);

-- Plantillas biométricas (cifradas) por usuario
CREATE TABLE IF NOT EXISTS biometric_template (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    provider VARCHAR(100),
    modality VARCHAR(20) NOT NULL, -- FINGERPRINT | FACE
    template BYTEA NOT NULL,       -- datos binarios cifrados
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    active BOOLEAN NOT NULL DEFAULT true
);

-- Pagos registrados (diario, mensual o paquetes)
CREATE TABLE IF NOT EXISTS payment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_user(id),
    amount NUMERIC(12,2) NOT NULL,
    payment_type VARCHAR(20) NOT NULL, -- MONTHLY | DAILY | PACKAGE
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    valid_from TIMESTAMPTZ,
    valid_to TIMESTAMPTZ,
    metadata JSONB
);

-- Detalle de paquetes (prepago de días)
CREATE TABLE IF NOT EXISTS payment_package (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL REFERENCES payment(id) ON DELETE CASCADE,
    days INTEGER NOT NULL,
    remaining_days INTEGER NOT NULL
);

-- Derechos efectivos (lo que permite o no el acceso)
CREATE TABLE IF NOT EXISTS user_right (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    right_type VARCHAR(20) NOT NULL, -- MONTHLY | DAILY | PACKAGE
    source_payment UUID REFERENCES payment(id),
    valid_from TIMESTAMPTZ NOT NULL,
    valid_to TIMESTAMPTZ,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Registro de accesos (aprobados/denegados)
CREATE TABLE IF NOT EXISTS access_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES app_user(id),
    at TIMESTAMPTZ NOT NULL DEFAULT now(),
    status VARCHAR(20) NOT NULL, -- APPROVED | DENIED
    reason VARCHAR(200),
    modality VARCHAR(20), -- FINGERPRINT | FACE | MANUAL
    matcher_metadata JSONB
);

-- Auditoría de cambios administrativos
CREATE TABLE IF NOT EXISTS audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID,
    action VARCHAR(20) NOT NULL, -- CREATE | UPDATE | DELETE
    performed_by VARCHAR(200),
    performed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    old_values JSONB,
    new_values JSONB,
    reason TEXT
);

-- Índices recomendados
CREATE INDEX IF NOT EXISTS idx_accesslog_user_at ON access_log (user_id, at DESC);
CREATE INDEX IF NOT EXISTS idx_payment_user_created ON payment (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_userright_user_active ON user_right (user_id, active, valid_to);
CREATE INDEX IF NOT EXISTS idx_biometric_user_id ON biometric_template (user_id);

