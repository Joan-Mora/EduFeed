-- Inicial: tablas minimas (usuarios y accesos) para probar Flyway
CREATE TABLE IF NOT EXISTS app_user (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    document VARCHAR(50) UNIQUE NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    user_type VARCHAR(30) NOT NULL -- STUDENT, TEACHER, STAFF
);

CREATE TABLE IF NOT EXISTS access_log (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES app_user(id),
    at TIMESTAMPTZ NOT NULL DEFAULT now(),
    status VARCHAR(20) NOT NULL, -- APPROVED/DENIED
    reason VARCHAR(200)
);
