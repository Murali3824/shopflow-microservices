-- ─────────────────────────────────────────────────────────────
-- V3__add_fullname_to_users.sql
-- Adds full_name column to existing users table
-- ─────────────────────────────────────────────────────────────

ALTER TABLE users
    ADD COLUMN full_name VARCHAR(100);

UPDATE users
SET full_name = 'Unknown'
WHERE full_name IS NULL;

ALTER TABLE users
    ALTER COLUMN full_name SET NOT NULL;