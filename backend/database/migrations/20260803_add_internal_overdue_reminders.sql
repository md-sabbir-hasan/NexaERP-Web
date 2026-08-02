-- NexaERP internal milestone overdue reminders.

ALTER TABLE notifications
    MODIFY COLUMN type ENUM(
        'SYSTEM',
        'USER_INVITATION',
        'INVOICE_OVERDUE',
        'INVOICE_PAYMENT',
        'INVOICE_POSTED',
        'INVOICE_CANCELLED',
        'VENDOR_BILL_DUE',
        'VENDOR_BILL_OVERDUE',
        'VENDOR_BILL_PAYMENT',
        'VENDOR_BILL_POSTED',
        'VENDOR_BILL_CANCELLED',
        'BUDGET_WARNING',
        'BUDGET_EXCEEDED',
        'ACCOUNTING_PERIOD',
        'EXPENSE',
        'RECURRING_EXPENSE',
        'PAYMENT',
        'PAYMENT_POSTED',
        'BANKING',
        'FIXED_ASSET',
        'JOURNAL_DRAFT_PENDING',
        'RECURRING_EXPENSE_DRAFT_PENDING',
        'ACCOUNTING_PERIOD_CLOSED',
        'ACCOUNTING_PERIOD_LOCKED'
    ) NOT NULL;

CREATE TABLE overdue_reminder_deliveries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    document_type ENUM('INVOICE', 'VENDOR_BILL') NOT NULL,
    document_id BIGINT NOT NULL,
    milestone_days INT NOT NULL,
    channel ENUM('IN_APP', 'EMAIL') NOT NULL,
    recipient_user_id BIGINT NOT NULL,
    status ENUM('PENDING', 'PROCESSING', 'SENT', 'FAILED', 'SKIPPED') NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    last_error VARCHAR(500) NULL,
    next_attempt_at DATETIME(6) NULL,
    processing_started_at DATETIME(6) NULL,
    sent_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_overdue_delivery_identity UNIQUE (
        document_type, document_id, milestone_days, channel, recipient_user_id
    ),
    INDEX idx_overdue_delivery_retry (status, next_attempt_at),
    INDEX idx_overdue_delivery_document (document_type, document_id)
);

CREATE INDEX idx_invoice_overdue_eligibility
    ON invoices (status, due_date);

CREATE INDEX idx_vendor_bill_overdue_eligibility
    ON vendor_bills (status, due_date);
