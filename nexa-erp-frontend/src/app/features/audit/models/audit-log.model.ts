export type AuditAction =
  | 'CREATED'
  | 'UPDATED'
  | 'DELETED'
  | 'POSTED'
  | 'APPROVED'
  | 'CANCELLED'
  | 'REVERSED'
  | 'ACTIVATED'
  | 'DEACTIVATED'
  | 'LOGIN'
  | 'LOGOUT'
  | 'PASSWORD_CHANGED'
  | 'UPLOADED';

// Must match the entityName strings used by auditLogService.log(...) on the backend
export const AUDIT_ENTITY_TYPES = [
  'ACCOUNT',
  'INVOICE',
  'JOURNAL_ENTRY',
  'VENDOR_BILL',
  'PARTY',
  'PAYMENT',
  'USER',
] as const;

export type AuditEntityType = (typeof AUDIT_ENTITY_TYPES)[number];

export interface AuditLog {
  id: number;
  userId: number | null;
  userName: string;
  entityName: string;
  entityId: number | null;
  action: AuditAction;
  oldValue: string | null;
  newValue: string | null;
  ipAddress: string;
  createdAt: string;
}

export interface AuditDiffRow {
  field: string;
  oldVal: string;
  newVal: string;
}
