-- Approval Workflow Phase 3: add Invoice requests and their dedicated approval permission.
ALTER TABLE approval_requests
    MODIFY COLUMN entity_type ENUM('MANUAL_JOURNAL', 'VENDOR_BILL', 'INVOICE') NOT NULL;

INSERT INTO permissions (code, name, module)
SELECT 'APPROVE_INVOICE', 'Approve Invoice', 'INVOICE'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'APPROVE_INVOICE');
