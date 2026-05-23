-- Migration 006: Add appraisal_config and announcements tables

CREATE TABLE IF NOT EXISTS appraisal_config (
    id               SERIAL PRIMARY KEY,
    academic_year    VARCHAR NOT NULL UNIQUE,
    is_open          BOOLEAN NOT NULL DEFAULT FALSE,
    submission_start TIMESTAMPTZ,
    submission_end   TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS announcements (
    id          SERIAL PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    body        VARCHAR(5000) NOT NULL,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_by  VARCHAR,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_appraisal_config_academic_year ON appraisal_config (academic_year);
CREATE INDEX IF NOT EXISTS idx_announcements_is_active ON announcements (is_active);
