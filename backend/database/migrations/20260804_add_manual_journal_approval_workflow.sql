-- NexaERP Approval Workflow Phase 1: MANUAL JOURNAL only.

CREATE TABLE approval_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    entity_type VARCHAR(40) NOT NULL,
    entity_id BIGINT NOT NULL,
    document_number VARCHAR(100) NOT NULL,
    document_title VARCHAR(255) NULL,
    maker_user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    required_permission VARCHAR(100) NOT NULL,
    document_updated_at DATETIME(6) NOT NULL,
    submitted_at DATETIME(6) NOT NULL,
    decided_at DATETIME(6) NULL,
    decided_by BIGINT NULL,
    decision_comment VARCHAR(500) NULL,
    consumed_at DATETIME(6) NULL,
    consumed_by BIGINT NULL,
    active_marker INT NULL,
    supersedes_request_id BIGINT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_approval_request_active UNIQUE (entity_type, entity_id, active_marker),
    CONSTRAINT fk_approval_request_maker FOREIGN KEY (maker_user_id) REFERENCES users(id),
    CONSTRAINT fk_approval_request_supersedes FOREIGN KEY (supersedes_request_id) REFERENCES approval_requests(id),
    INDEX idx_approval_status_permission_submitted (status, required_permission, submitted_at),
    INDEX idx_approval_maker_submitted (maker_user_id, submitted_at),
    INDEX idx_approval_entity (entity_type, entity_id),
    INDEX idx_approval_decider_decided (decided_by, decided_at)
);

CREATE TABLE approval_actions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    approval_request_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL,
    actor_user_id BIGINT NOT NULL,
    actor_name_snapshot VARCHAR(150) NOT NULL,
    from_status VARCHAR(20) NULL,
    to_status VARCHAR(20) NULL,
    comment VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_approval_action_request FOREIGN KEY (approval_request_id) REFERENCES approval_requests(id),
    INDEX idx_approval_action_history (approval_request_id, created_at, id)
);

INSERT INTO permissions (code, name, module)
SELECT 'VIEW_APPROVAL_QUEUE', 'View Approval Queue', 'APPROVAL'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'VIEW_APPROVAL_QUEUE');

INSERT INTO permissions (code, name, module)
SELECT 'APPROVE_JOURNAL', 'Approve Journal', 'JOURNAL'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'APPROVE_JOURNAL');

ALTER TABLE notifications
    MODIFY COLUMN type ENUM(
        'SYSTEM','USER_INVITATION','INVOICE_OVERDUE','INVOICE_PAYMENT','INVOICE_POSTED','INVOICE_CANCELLED',
        'VENDOR_BILL_DUE','VENDOR_BILL_OVERDUE','VENDOR_BILL_PAYMENT','VENDOR_BILL_POSTED','VENDOR_BILL_CANCELLED',
        'BUDGET_WARNING','BUDGET_EXCEEDED','ACCOUNTING_PERIOD','EXPENSE','RECURRING_EXPENSE','PAYMENT','PAYMENT_POSTED',
        'BANKING','FIXED_ASSET','JOURNAL_DRAFT_PENDING','RECURRING_EXPENSE_DRAFT_PENDING',
        'ACCOUNTING_PERIOD_CLOSED','ACCOUNTING_PERIOD_LOCKED','APPROVAL_SUBMITTED','APPROVAL_APPROVED',
        'APPROVAL_REJECTED','APPROVAL_RETURNED'
    ) NOT NULL;

-- Apply only where audit_logs.action is maintained as a native MySQL ENUM.
ALTER TABLE audit_logs
    MODIFY COLUMN action ENUM(
        'CREATED','UPDATED','DELETED','POSTED','APPROVED','CANCELLED','REVERSED','ACTIVATED','DEACTIVATED',
        'LOGIN','LOGOUT','PASSWORD_CHANGED','UPLOADED','CLOSED','OPENED','LOCKED','SUBMITTED','REJECTED','RETURNED','CONSUMED'
    ) NOT NULL;
