export type ExpensePaymentStatus = 'UNPAID' | 'PARTIAL' | 'PAID';
export type ExpenseStatus = 'DRAFT' | 'POSTED' | 'CANCELLED';

export interface ExpenseRequest {
  expenseDate: string;
  expenseAccountId: number;
  paidImmediately: boolean;
  paymentAccountId?: number | null;
  partyId?: number | null;
  amount: number;
  referenceNumber?: string;
  attachmentUrl?: string;
  notes?: string;
}

export interface ExpenseResponse {
  id: number;
  expenseNumber: string;
  expenseDate: string;

  expenseAccountId: number;
  expenseAccountName: string;

  paidImmediately: boolean;

  paymentAccountId: number | null;
  paymentAccountName: string | null;

  partyId: number | null;
  partyName: string | null;

  amount: number;
  paidAmount: number;
  dueAmount: number;
  paymentStatus: ExpensePaymentStatus;

  referenceNumber: string | null;
  attachmentUrl: string | null;
  notes: string | null;

  status: ExpenseStatus;
  cancelledAt: string | null;
  cancelReason: string | null;

  createdAt: string;
  budgetWarnings: { message: string }[] | null;
}

export interface ExpenseCancelRequest {
  reason: string;
}