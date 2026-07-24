export type RecurringFrequency = 'WEEKLY' | 'MONTHLY' | 'QUARTERLY' | 'YEARLY';
export type RecurringExpenseStatus = 'ACTIVE' | 'PAUSED' | 'ENDED';

export interface RecurringExpenseTemplateRequest {
  name: string;
  expenseAccountId: number;
  amount: number;
  paidImmediately: boolean;
  paymentAccountId?: number | null;
  partyId?: number | null;
  frequency: RecurringFrequency;
  startDate: string;
  endDate?: string | null;
  referenceNumber?: string;
  notes?: string;
}

export interface RecurringExpenseTemplateResponse {
  id: number;
  name: string;

  expenseAccountId: number;
  expenseAccountName: string;

  amount: number;
  paidImmediately: boolean;

  paymentAccountId: number | null;
  paymentAccountName: string | null;

  partyId: number | null;
  partyName: string | null;

  frequency: RecurringFrequency;
  startDate: string;
  nextRunDate: string;
  endDate: string | null;
  lastGeneratedDate: string | null;
  lastGeneratedExpenseId: number | null;

  status: RecurringExpenseStatus;
  referenceNumber: string | null;
  notes: string | null;
  lastRunError: string | null;
}