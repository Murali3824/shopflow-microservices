CREATE TABLE audit_logs (
                            id           UUID                     DEFAULT gen_random_uuid() PRIMARY KEY,
                            admin_id     UUID                     NOT NULL,
                            action       VARCHAR(100)             NOT NULL,
                            target_type  VARCHAR(50)              NOT NULL,
                            target_id    UUID,
                            details      TEXT,
                            performed_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE INDEX idx_audit_logs_admin_id      ON audit_logs (admin_id);
CREATE INDEX idx_audit_logs_performed_at  ON audit_logs (performed_at);