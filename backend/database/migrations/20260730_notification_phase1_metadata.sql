-- NexaERP Notification Phase 1 metadata
-- Backward-compatible with existing notification rows and API consumers.

ALTER TABLE notifications
    ADD COLUMN priority VARCHAR(20) NULL DEFAULT 'MEDIUM' AFTER type,
    ADD COLUMN module VARCHAR(30) NULL DEFAULT 'SYSTEM' AFTER priority;

UPDATE notifications
SET priority = 'MEDIUM'
WHERE priority IS NULL;

UPDATE notifications
SET module = 'SYSTEM'
WHERE module IS NULL;

CREATE INDEX idx_notification_event_identity
    ON notifications (user_id, type, entity_type, entity_id);
