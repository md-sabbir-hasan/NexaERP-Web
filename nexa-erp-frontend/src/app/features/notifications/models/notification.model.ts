export type NotificationType =
  | 'SYSTEM'
  | 'USER_INVITATION'
  | 'INVOICE_OVERDUE'
  | 'INVOICE_PAYMENT'
  | 'VENDOR_BILL_DUE'
  | 'VENDOR_BILL_PAYMENT'
  | 'BUDGET_WARNING'
  | 'BUDGET_EXCEEDED'
  | 'ACCOUNTING_PERIOD'
  | 'EXPENSE'
  | 'RECURRING_EXPENSE'
  | 'PAYMENT'
  | 'BANKING'
  | 'FIXED_ASSET';

export interface NotificationResponse {
  id: number;
  type: NotificationType;
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
