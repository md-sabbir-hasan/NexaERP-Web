export type ApprovalEntityType = 'MANUAL_JOURNAL' | 'VENDOR_BILL' | 'INVOICE' | 'PAYMENT';
export type ApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'RETURNED' | 'CANCELLED';
export type ApprovalActionType =
  'SUBMITTED' | 'APPROVED' | 'REJECTED' | 'RETURNED' | 'CONSUMED' | 'CANCELLED';

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
export interface ApprovalAction {
  id: number;
  approvalRequestId: number;
  action: ApprovalActionType;
  actorUserId: number;
  actorName: string;
  fromStatus: ApprovalStatus | null;
  toStatus: ApprovalStatus | null;
  comment: string | null;
  createdAt: string;
}
export interface ApprovalRequest {
  id: number;
  entityType: ApprovalEntityType;
  entityId: number;
  documentNumber: string;
  documentTitle: string | null;
  entityLabel: string;
  documentUrl: string;
  makerUserId: number;
  makerName: string;
  status: ApprovalStatus;
  requiredPermission: string;
  rejectPermission: string;
  returnPermission: string;
  submittedAt: string;
  decidedAt: string | null;
  decidedBy: number | null;
  decisionComment: string | null;
  consumedAt: string | null;
  consumedBy: number | null;
  supersedesRequestId: number | null;
  canDecide: boolean;
  canApprove: boolean;
  canReject: boolean;
  canReturn: boolean;
  actions: ApprovalAction[];
}