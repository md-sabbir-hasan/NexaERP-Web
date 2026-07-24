export type JournalEntryType =
  | 'GENERAL'
  | 'SALES'
  | 'PURCHASE'
  | 'CASH'
  | 'BANK'
  | 'PAYROLL';

export type JournalStatus = 'DRAFT' | 'POSTED' | 'REVERSED';

export type JournalSourceType =
  | 'MANUAL'
  | 'INVOICE'
  | 'VENDOR_BILL'
  | 'PAYMENT'
  | 'BANK_TRANSACTION'
  | 'EXPENSE_CLAIM';

export interface JournalLine {
  id: number;
  accountId: number;
  accountCode: string;
  accountName: string;
  debit: number;
  credit: number;
  description: string | null;
}

export interface JournalEntry {
  id: number;
  entryNumber: string;
  date: string;
  description: string | null;
  type: JournalEntryType;
  status: JournalStatus;
  sourceType: JournalSourceType;
  totalAmount: number;
  lines: JournalLine[];
}

export interface JournalLineRequest {
  accountId: number | null;
  debit: number;
  credit: number;
  description: string;
}

export interface JournalEntryRequest {
  date: string;
  description: string;
  type: JournalEntryType;
  lines: JournalLineRequest[];
}