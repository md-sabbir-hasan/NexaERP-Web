export type NotificationType =
  | 'SYSTEM'
  | 'USER_INVITATION'
  | 'INVOICE_OVERDUE'
  | 'INVOICE_PAYMENT'
  | 'INVOICE_POSTED'
  | 'INVOICE_CANCELLED'
  | 'VENDOR_BILL_DUE'
  | 'VENDOR_BILL_OVERDUE'
  | 'VENDOR_BILL_PAYMENT'
  | 'VENDOR_BILL_POSTED'
  | 'VENDOR_BILL_CANCELLED'
  | 'BUDGET_WARNING'
  | 'BUDGET_EXCEEDED'
  | 'ACCOUNTING_PERIOD'
  | 'EXPENSE'
  | 'RECURRING_EXPENSE'
  | 'PAYMENT'
  | 'PAYMENT_POSTED'
  | 'BANKING'
  | 'FIXED_ASSET'
  | 'JOURNAL_DRAFT_PENDING'
  | 'RECURRING_EXPENSE_DRAFT_PENDING'
  | 'ACCOUNTING_PERIOD_CLOSED'
  | 'ACCOUNTING_PERIOD_LOCKED'
  | 'APPROVAL_SUBMITTED'
  | 'APPROVAL_APPROVED'
  | 'APPROVAL_REJECTED'
  | 'APPROVAL_RETURNED';

export type NotificationPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type NotificationModule =
  | 'SYSTEM'
  | 'BUDGET'
  | 'JOURNAL'
  | 'EXPENSE'
  | 'ACCOUNTING_PERIOD'
  | 'INVOICE'
  | 'VENDOR_BILL'
  | 'PAYMENT'
  | 'BANKING'
  | 'FIXED_ASSET'
  | 'APPROVAL';

export interface NotificationResponse {
  id: number;
  type: NotificationType;
  priority?: NotificationPriority | null;
  module?: NotificationModule | null;
  title: string;
  message: string;
  route: string | null;
  entityType: string | null;
  entityId: number | null;
  read: boolean;
  readAt: string | null;
  expiresAt: string | null;
  createdAt: string;
}
