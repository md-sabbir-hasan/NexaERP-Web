-- Approval Workflow Phase 4: add Payment requests and their dedicated approval permission.
ALTER TABLE approval_requests
    MODIFY COLUMN entity_type ENUM('MANUAL_JOURNAL', 'VENDOR_BILL', 'INVOICE', 'PAYMENT') NOT NULL;

INSERT INTO permissions (code, name, module)
SELECT 'APPROVE_PAYMENT', 'Approve Payment', 'PAYMENT'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'APPROVE_PAYMENT');
