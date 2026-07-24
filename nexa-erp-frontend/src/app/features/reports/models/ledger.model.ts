export interface LedgerRequest {
  accountId: number;
  fromDate: string;
  toDate: string;
}

export interface LedgerEntry {
  journalEntryId: number;
  date: string;
  journalEntryNumber: string;
  sourceType: string;
  sourceId: number;
  referenceNumber: string;
  description: string;
  debit: number;
  credit: number;
  runningBalance: number;
}

export interface LedgerResponse {
  accountId: number;
  accountCode: string;
  accountName: string;
  accountType: string;
  fromDate: string;
  toDate: string;
  openingBalance: number;
  closingBalance: number;
  totalDebit: number;
  totalCredit: number;
  entries: LedgerEntry[];
}
