export interface BalanceSheetRow {
  accountId: number;
  accountCode: string;
  accountName: string;
  amount: number;
}

export interface BalanceSheetResponse {
  asOfDate: string;

  assets: BalanceSheetRow[];
  totalAssets: number;

  liabilities: BalanceSheetRow[];
  totalLiabilities: number;

  equity: BalanceSheetRow[];
  totalEquityExcludingProfit: number;

  netProfit: number;
  totalEquity: number;

  totalLiabilitiesAndEquity: number;
  isBalanced: boolean;
}
