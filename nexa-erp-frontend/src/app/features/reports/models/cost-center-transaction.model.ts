export interface CostCenterTransactionLine {
  journalEntryId: number;
  journalNumber: string;
  date: string;
  source: string;
  sourceId: number | null;
  accountCode: string;
  accountName: string;
  debit: number;
  credit: number;
  description: string | null;
}

export interface CostCenterTransactionReport {
  costCenterId: number;
  costCenterCode: string;
  costCenterName: string;
  fromDate: string;
  toDate: string;
  rows: CostCenterTransactionLine[];
  totalDebit: number;
  totalCredit: number;
  netAmount: number;
}
