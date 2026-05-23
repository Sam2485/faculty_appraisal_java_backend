-- Migration 019: Rejection / resubmission support for teaching appraisals
-- Adds a submission attempt counter to declarations so each resubmission
-- after rejection is distinguishable in the audit trail.

ALTER TABLE public.declarations
  ADD COLUMN IF NOT EXISTS submission_attempt INTEGER NOT NULL DEFAULT 1;
