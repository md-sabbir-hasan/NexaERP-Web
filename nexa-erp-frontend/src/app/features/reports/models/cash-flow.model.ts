export type CashFlowActivity = 'OPERATING' | 'INVESTING' | 'FINANCING';

export interface CashFlowLineItem {
  lineItem: string;
  label: string;
  inflow: number;
  outflow: number;
  netAmount: number;
}

export interface CashFlowActivitySection {
  activity: CashFlowActivity;
  items: CashFlowLineItem[];
  totalInflows: number;
  totalOutflows: number;
  netCashFlow: number;
}

export interface CashFlowAccountBalance {
  accountId: number;
  accountCode: string;
  accountName: string;
  openingBalance: number;
  periodMovement: number;
  closingBalance: number;
}

export interface UnclassifiedCashMovement {
  journalEntryId: number;
  entryNumber: string;
  date: string;
  sourceType: string;
  sourceId: number | null;
  description: string | null;
  amount: number;
  reason: string;
}

export interface CashFlowStatementResponse {
  fromDate: string;
  toDate: string;
  currencyCode: string;
  openingCashBalance: number;
  operatingActivities: CashFlowActivitySection;
  netCashFromOperatingActivities: number;
  investingActivities: CashFlowActivitySection;
  netCashFromInvestingActivities: number;
  financingActivities: CashFlowActivitySection;
  netCashFromFinancingActivities: number;
  netChangeInCash: number;
  calculatedClosingCashBalance: number;
  ledgerClosingCashBalance: number;
  reconciliationDifference: number;
  isReconciled: boolean;
  cashAccounts: CashFlowAccountBalance[];
  unclassifiedMovements: UnclassifiedCashMovement[];
  generatedAt: string;
}
