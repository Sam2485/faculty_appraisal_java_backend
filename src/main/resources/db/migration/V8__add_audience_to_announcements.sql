-- Migration 008: Add audience column to announcements

ALTER TABLE announcements
    ADD COLUMN IF NOT EXISTS audience VARCHAR(50) NOT NULL DEFAULT 'all';
