export interface ProfitLossRow {
  accountId: number;
  accountCode: string;
  accountName: string;
  amount: number;
}

export interface ProfitLossResponse {
  fromDate: string;

  toDate: string;

  revenues: ProfitLossRow[];
  totalRevenue: number;

  expenses: ProfitLossRow[];
  totalExpense: number;

  netProfit: number;
}
