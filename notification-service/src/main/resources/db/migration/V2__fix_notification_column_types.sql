ALTER TABLE notifications
DROP COLUMN channel,
    DROP COLUMN status;

DROP TYPE IF EXISTS notification_channel;
DROP TYPE IF EXISTS notification_status;

ALTER TABLE notifications
    ADD COLUMN channel VARCHAR(20) NOT NULL DEFAULT 'EMAIL',
    ADD COLUMN status  VARCHAR(20) NOT NULL DEFAULT 'SENT';