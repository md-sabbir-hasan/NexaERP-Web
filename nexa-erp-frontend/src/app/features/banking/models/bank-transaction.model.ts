export type TransactionType = 'CREDIT' | 'DEBIT';

export type TransactionSourceType = 'MANUAL' | 'PAYMENT' | 'JOURNAL' | 'TRANSFER';

export interface BankTransaction {
  id: number;
  transactionNumber: string;
  bankAccountId: number;
  bankAccountName: string;
  transactionDate: string;
  transactionType: TransactionType;
  amount: number;
  description: string | null;
  referenceNumber: string | null;
  contraAccountId: number | null;
  contraAccountName: string | null;
  reconciled: boolean;
  reconciledAt: string | null;
  reconciliationId: number | null;
  voided: boolean;
  voidedAt: string | null;
  sourceType: TransactionSourceType;
  createdAt: string;
}

export interface BankTransactionRequest {
  bankAccountId: number;
  transactionDate: string;
  transactionType: TransactionType;
  amount: number;
  description: string | null;
  referenceNumber: string | null;
  contraAccountId: number;
}

export interface BankTransferRequest {
  fromBankAccountId: number;
  toBankAccountId: number;
  transactionDate: string;
  amount: number;
  description: string | null;
  referenceNumber: string | null;
}

export interface BankTransferResponse {
  debitTransaction: BankTransaction;
  creditTransaction: BankTransaction;
}
