-- ─────────────────────────────────────────────────────────────
-- V2__fix_role_column_type.sql
-- Converts role column from native PostgreSQL ENUM to VARCHAR.
-- Hibernate handles enum validation on the Java side.
-- ─────────────────────────────────────────────────────────────

ALTER TABLE users
ALTER COLUMN role TYPE VARCHAR(20)
    USING role::TEXT;

DROP TYPE user_role;



