-- Tokens de "olvidé mi contraseña": se guarda el hash SHA-256, nunca el token en claro.
CREATE TABLE password_reset_token (
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ux_password_reset_token_hash UNIQUE (token_hash)
);
CREATE INDEX ix_password_reset_token_user ON password_reset_token (user_id);
