export type StatementEntryType =
  'INVOICE' | 'VENDOR_BILL' | 'PAYMENT' | 'OPENING_BALANCE' | 'JOURNAL';

export interface PartyStatementEntry {
  date: string;
  type: StatementEntryType;
  referenceId: number;
  referenceNumber: string;
  description: string;
  debit: number;
  credit: number;
  runningBalance: number;
}

export interface PartyStatementResponse {
  partyId: number;
  partyName: string;
  partyType: string;
  fromDate: string;
  toDate: string;
  openingBalance: number;
  entries: PartyStatementEntry[];
  closingBalance: number;
}
