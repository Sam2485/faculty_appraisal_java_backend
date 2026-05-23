-- Migration 012: Add reports_to_registrar to faculty_profiles

ALTER TABLE public.faculty_profiles
    ADD COLUMN IF NOT EXISTS reports_to_registrar BOOLEAN NOT NULL DEFAULT FALSE;
