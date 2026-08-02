-- NexaERP Payment posted notification type.
-- Required when notifications.type is stored as a native MySQL ENUM.

ALTER TABLE notifications
    MODIFY COLUMN type ENUM(
        'SYSTEM',
        'USER_INVITATION',
        'INVOICE_OVERDUE',
        'INVOICE_PAYMENT',
        'INVOICE_POSTED',
        'INVOICE_CANCELLED',
        'VENDOR_BILL_DUE',
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
