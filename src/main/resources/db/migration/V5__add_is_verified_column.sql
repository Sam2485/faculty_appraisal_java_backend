-- Migration 005: Add is_verified column to faculty_profiles
-- Column exists in the SQLAlchemy model but was missing from the production DB schema.

ALTER TABLE faculty_profiles
ADD COLUMN IF NOT EXISTS is_verified BOOLEAN NOT NULL DEFAULT FALSE;
