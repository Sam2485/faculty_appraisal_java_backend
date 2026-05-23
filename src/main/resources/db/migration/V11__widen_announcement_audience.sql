-- Migration 011: Widen announcement audience column to VARCHAR(500)

ALTER TABLE public.announcements
  ALTER COLUMN audience TYPE VARCHAR(500);
