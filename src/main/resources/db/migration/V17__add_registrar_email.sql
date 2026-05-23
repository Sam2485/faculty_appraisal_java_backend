-- Migration 017: add registrar_email to faculty_profiles
-- Allows explicit Registrar assignment per non-teaching staff member,
-- replacing the previous approach where all Registrars saw all non-teaching staff.

ALTER TABLE public.faculty_profiles
  ADD COLUMN IF NOT EXISTS registrar_email TEXT DEFAULT NULL;
