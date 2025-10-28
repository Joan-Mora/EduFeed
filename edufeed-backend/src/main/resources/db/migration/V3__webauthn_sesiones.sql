-- Tabla para sesiones de WebAuthn (tracking de registro/autenticación)
CREATE TABLE IF NOT EXISTS sesiones_webauthn (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    challenge VARCHAR(255) NOT NULL,
    usuario_documento VARCHAR(20),
    tipo VARCHAR(20) NOT NULL, -- REGISTRO | AUTENTICACION
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE', -- PENDIENTE | COMPLETADA | EXPIRADA | FALLIDA
    creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expira_en TIMESTAMPTZ NOT NULL,
    completado_en TIMESTAMPTZ,
    resultado JSONB
);

-- Índices para optimización
CREATE INDEX idx_sesiones_webauthn_estado ON sesiones_webauthn(estado);
CREATE INDEX idx_sesiones_webauthn_expira ON sesiones_webauthn(expira_en);
CREATE INDEX idx_sesiones_webauthn_usuario ON sesiones_webauthn(usuario_documento);
