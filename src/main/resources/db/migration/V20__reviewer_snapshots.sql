-- Migration 020: reviewer draft snapshots
-- Allows HOD, Director, Dean, VC, Registrar, Reporting Officer etc. to save
-- partial review scores without finalising the declaration status.

CREATE TABLE IF NOT EXISTS public.reviewer_snapshots (
    id             UUID         NOT NULL DEFAULT gen_random_uuid(),
    faculty_email  TEXT         NOT NULL,
    academic_year  TEXT         NOT NULL,
    reviewer_email TEXT         NOT NULL,
    reviewer_role  TEXT         NOT NULL,
    payload        JSONB        NOT NULL DEFAULT '{}',
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT reviewer_snapshots_pkey   PRIMARY KEY (id),
    CONSTRAINT reviewer_snapshots_unique UNIQUE (faculty_email, academic_year, reviewer_role)
);

CREATE INDEX IF NOT EXISTS idx_reviewer_snapshots_faculty_year
    ON public.reviewer_snapshots (faculty_email, academic_year);
