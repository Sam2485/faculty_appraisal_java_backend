-- Migration 009: Add module_config table for Section Controls toggles

CREATE TABLE IF NOT EXISTS module_config (
    id                       INTEGER PRIMARY KEY DEFAULT 1,
    appraisal_module_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    self_appraisal_enabled   BOOLEAN NOT NULL DEFAULT TRUE,
    peer_review_enabled      BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Seed the single config row with defaults
INSERT INTO module_config (id, appraisal_module_enabled, self_appraisal_enabled, peer_review_enabled)
VALUES (1, TRUE, TRUE, FALSE)
ON CONFLICT (id) DO NOTHING;
