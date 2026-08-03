export type PaymentType = 'RECEIPT' | 'PAYMENT';

export type PaymentStatus = 'DRAFT' | 'POSTED' | 'CANCELLED';

export type PaymentMethod =
  'CASH' | 'BANK_TRANSFER' | 'CHEQUE' | 'BKASH' | 'NAGAD' | 'ROCKET' | 'CARD';

export type PaymentReferenceType = 'INVOICE' | 'VENDOR_BILL';

export interface PaymentAllocationRequest {
  referenceType: PaymentReferenceType;
  referenceId: number;
  allocatedAmount: number;
}

export interface PaymentAllocationResponse {
  id: number;
  referenceType: PaymentReferenceType;
  referenceId: number;
  allocatedAmount: number;
  createdAt: string;
}

export interface PaymentRequest {
  partyId: number;
  accountId: number;
  paymentDate: string;

  paymentType: PaymentType;

  amount: number;

  currencyCode: string;

  paymentMethod: PaymentMethod;

  transactionRef?: string;

  notes?: string;

  autoAllocate: boolean;

  allocations: PaymentAllocationRequest[];
}

export interface PaymentResponse {
  id: number;

  paymentNumber: string;

  paymentDate: string;

  paymentType: PaymentType;

  partyId: number;
  partyName: string;

  accountId: number;
  accountName: string;

  amount: number;

  allocatedAmount: number;

  unallocatedAmount: number;

  currencyCode: string;

  exchangeRate: number;

  paymentMethod: PaymentMethod;

  transactionRef: string;

  notes: string;

  status: PaymentStatus;

  postedAt: string | null;

  createdAt: string;

  updatedAt: string;

  createdBy?: number | null;

  approvalFeatureEnabled?: boolean;

  latestApprovalId?: number | null;

  activeApprovalId?: number | null;

  approvalStatus?: 'PENDING' | 'APPROVED' | 'REJECTED' | 'RETURNED' | 'CANCELLED' | null;

  approvalConsumed?: boolean | null;

  allocations: PaymentAllocationResponse[];
}

export interface PartyOutstandingSummary {
  partyId: number;
  partyName: string;
  totalAmount: number;
  paidAmount: number;
  dueAmount: number;
  documentCount: number;
}
