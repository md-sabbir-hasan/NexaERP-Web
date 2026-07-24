export type ReconciliationStatus = 'IN_PROGRESS' | 'COMPLETED';

export interface BankReconciliation {
  id: number;
  bankAccountId: number;
  bankAccountName: string;
  statementDate: string;
  statementBalance: number;
  bookBalance: number;
  depositsInTransit: number;
  outstandingCheques: number;
  adjustedBankBalance: number;
  difference: number;
  status: ReconciliationStatus;
  notes: string | null;
  completedAt: string | null;
  createdAt: string;
  unmatchedTransactionIds: number[];
}

export interface BankReconciliationStartRequest {
  bankAccountId: number;
  statementDate: string;
  statementBalance: number;
  notes: string | null;
}

export type StatementLineStatus = 'UNMATCHED' | 'MATCHED';

export interface BankStatementLine {
  id: number;
  reconciliationId: number;
  lineDate: string;
  description: string | null;
  amount: number;
  transactionType: 'CREDIT' | 'DEBIT';
  referenceNumber: string | null;
  status: StatementLineStatus;
  matchedTransactionId: number | null;
  matchedTransactionNumber: string | null;
}

export interface StatementImportResult {
  totalLines: number;
  autoMatchedCount: number;
  unmatchedCount: number;
  lines: BankStatementLine[];
  reconciliation: BankReconciliation;
}
