-- Migration 007: add section_scores to appraisal_reviews, create password_reset_tokens
-- Safe to re-run: all statements use IF NOT EXISTS / ADD COLUMN IF NOT EXISTS guards.

-- 1. Add section_scores JSONB column to appraisal_reviews
ALTER TABLE public.appraisal_reviews
  ADD COLUMN IF NOT EXISTS section_scores jsonb NOT NULL DEFAULT '{}'::jsonb;

-- 2. Create password_reset_tokens table (supports forgot-password / reset-password flow)
CREATE TABLE IF NOT EXISTS public.password_reset_tokens (
  id         uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
  email      text        NOT NULL,
  token_hash text        NOT NULL UNIQUE,
  used       boolean     NOT NULL DEFAULT FALSE,
  expires_at timestamptz NOT NULL,
  created_at timestamptz NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS password_reset_tokens_email_idx
  ON public.password_reset_tokens (email);

CREATE INDEX IF NOT EXISTS password_reset_tokens_expires_idx
  ON public.password_reset_tokens (expires_at);
