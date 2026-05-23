-- Migration 016: add reporting_officer_email to faculty_profiles
-- Allows explicit RO assignment per non-teaching staff member,
-- replacing the previous school+department matching approach.

ALTER TABLE public.faculty_profiles
  ADD COLUMN IF NOT EXISTS reporting_officer_email TEXT DEFAULT NULL;
