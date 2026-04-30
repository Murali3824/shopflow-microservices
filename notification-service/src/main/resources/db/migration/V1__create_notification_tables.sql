CREATE TYPE notification_channel AS ENUM ('EMAIL');
CREATE TYPE notification_status AS ENUM ('SENT', 'FAILED');

CREATE TABLE notifications
(
    id           UUID                     DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id      UUID                     NOT NULL,
    event_type   VARCHAR(50)              NOT NULL,
    channel      notification_channel     NOT NULL DEFAULT 'EMAIL',
    subject      VARCHAR(255)             NOT NULL,
    status       notification_status      NOT NULL DEFAULT 'SENT',
    retry_count  INT                      NOT NULL DEFAULT 0,
    is_read      BOOLEAN                  NOT NULL DEFAULT FALSE,
    sent_at      TIMESTAMP WITH TIME ZONE DEFAULT now()
);