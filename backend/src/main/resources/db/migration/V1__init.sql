-- ReviewTap — esquema inicial. Todas las fechas en UTC (timestamptz).

CREATE TABLE app_user (
    id            UUID PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    role          VARCHAR(30)  NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL
);
CREATE UNIQUE INDEX ux_app_user_email ON app_user (lower(email));

CREATE TABLE business (
    id                UUID PRIMARY KEY,
    name              VARCHAR(150)  NOT NULL,
    slug              VARCHAR(160)  NOT NULL,
    google_review_url VARCHAR(2048),
    logo_url          VARCHAR(2048),
    address           VARCHAR(255),
    phone             VARCHAR(40),
    timezone          VARCHAR(64)   NOT NULL DEFAULT 'Europe/Madrid',
    active            BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ   NOT NULL,
    updated_at        TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ux_business_slug UNIQUE (slug)
);

CREATE TABLE business_user (
    id          UUID PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    business_id UUID        NOT NULL REFERENCES business (id) ON DELETE CASCADE,
    role        VARCHAR(30) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT ux_business_user UNIQUE (user_id, business_id)
);
CREATE INDEX ix_business_user_business ON business_user (business_id);

CREATE TABLE device (
    id                   UUID PRIMARY KEY,
    business_id          UUID         NOT NULL REFERENCES business (id),
    public_code          VARCHAR(32)  NOT NULL,
    name                 VARCHAR(120) NOT NULL,
    location_description VARCHAR(200),
    type                 VARCHAR(20)  NOT NULL,
    active               BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ux_device_public_code UNIQUE (public_code)
);
CREATE INDEX ix_device_business ON device (business_id);

CREATE TABLE interaction (
    id                  UUID PRIMARY KEY,
    device_id           UUID        NOT NULL REFERENCES device (id),
    created_at          TIMESTAMPTZ NOT NULL,
    interaction_type    VARCHAR(20) NOT NULL,
    user_agent_category VARCHAR(20),
    referer             VARCHAR(255)
);
-- Consultas de analítica: siempre filtran por dispositivo(s) y rango temporal.
CREATE INDEX ix_interaction_device_created ON interaction (device_id, created_at);
CREATE INDEX ix_interaction_created ON interaction (created_at);
