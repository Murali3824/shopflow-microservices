-- ─────────────────────────────────────────────────────────────
-- V1__create_auth_tables.sql
-- Creates: users, refresh_tokens
-- ─────────────────────────────────────────────────────────────

-- ── 1. ENUM type for user roles ──────────────────────────────
CREATE TYPE user_role AS ENUM ('USER', 'SELLER', 'ADMIN');

-- ── 2. users ─────────────────────────────────────────────────
CREATE TABLE users (
                       id            UUID         NOT NULL DEFAULT gen_random_uuid(),
                       email         VARCHAR(255) NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       role          user_role    NOT NULL,
                       is_verified   BOOLEAN      NOT NULL DEFAULT FALSE,
                       is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
                       created_at    TIMESTAMP    NOT NULL DEFAULT now(),

                       CONSTRAINT pk_users PRIMARY KEY (id),
                       CONSTRAINT uq_users_email UNIQUE (email)
);

-- ── 3. refresh_tokens ────────────────────────────────────────
CREATE TABLE refresh_tokens (
                                id         UUID      NOT NULL DEFAULT gen_random_uuid(),
                                user_id    UUID      NOT NULL,
                                token      TEXT      NOT NULL,
                                expires_at TIMESTAMP NOT NULL,
                                revoked    BOOLEAN   NOT NULL DEFAULT FALSE,

                                CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
                                CONSTRAINT fk_refresh_tokens_user
                                    FOREIGN KEY (user_id)
                                        REFERENCES users (id)
                                        ON DELETE CASCADE
);

-- ── 4. Indexes ───────────────────────────────────────────────
CREATE INDEX idx_users_email
    ON users (email);

CREATE INDEX idx_refresh_tokens_user_id
    ON refresh_tokens (user_id);

CREATE INDEX idx_refresh_tokens_token
    ON refresh_tokens (token);