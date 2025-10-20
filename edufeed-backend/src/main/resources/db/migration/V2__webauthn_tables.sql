-- V2: Tablas para soporte de WebAuthn (huella por teléfono)

CREATE TABLE IF NOT EXISTS dispositivos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    plataforma VARCHAR(50),
    modelo VARCHAR(100),
    push_token VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT true,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS credenciales_webauthn (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    dispositivo_id UUID NOT NULL REFERENCES dispositivos(id) ON DELETE CASCADE,
    credential_id VARCHAR(255) NOT NULL,
    public_key TEXT NOT NULL,
    sign_count BIGINT,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT now(),
    activo BOOLEAN NOT NULL DEFAULT true
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_credenciales_webauthn_credential
    ON credenciales_webauthn(credential_id);

CREATE INDEX IF NOT EXISTS idx_credenciales_webauthn_usuario
    ON credenciales_webauthn(usuario_id);
