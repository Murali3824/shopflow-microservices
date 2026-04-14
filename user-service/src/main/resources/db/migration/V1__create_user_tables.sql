-- ============================================================
-- V1__create_user_tables.sql
-- Flyway migration for user-service
-- Creates: user_profile, user_address
-- ============================================================


-- ── user_profile ────────────────────────────────────────────
-- One profile per user. user_id is a reference ID only —
-- no FK to auth-service because each service owns its own DB.
-- unique constraint on user_id enforces one profile per user.

CREATE TABLE user_profile (
                              id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
                              user_id       UUID          NOT NULL UNIQUE,
                              full_name     VARCHAR(100),
                              phone         VARCHAR(20),
                              avatar_url    TEXT,
                              date_of_birth DATE
);


-- ── user_address ─────────────────────────────────────────────
-- A user can have many addresses (home, work, etc).
-- user_id is a reference ID only — no cross-DB foreign key.
-- is_default flags which address is pre-selected at checkout.
-- Only one address should have is_default = true per user —
-- this is enforced in service layer, not DB, to keep it simple.

CREATE TABLE user_address (
                              id         UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
                              user_id    UUID          NOT NULL,
                              label      VARCHAR(50),
                              street     TEXT,
                              city       VARCHAR(100),
                              state      VARCHAR(100),
                              pincode    VARCHAR(20),
                              is_default BOOLEAN       NOT NULL DEFAULT FALSE
);


-- ── indexes ──────────────────────────────────────────────────
-- user_id is queried on every request ("get my profile", "get my addresses")
-- so an index on user_id in both tables avoids full table scans.

CREATE INDEX idx_user_profile_user_id  ON user_profile (user_id);
CREATE INDEX idx_user_address_user_id  ON user_address (user_id);