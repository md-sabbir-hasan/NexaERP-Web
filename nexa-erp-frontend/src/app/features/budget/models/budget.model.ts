export type BudgetStatus = 'DRAFT' | 'APPROVED' | 'ACTIVE' | 'CLOSED' | 'CANCELLED';
export type BudgetAllocationMethod = 'EQUAL' | 'MANUAL';
export type VarianceStatus = 'FAVORABLE' | 'UNFAVORABLE' | 'ON_TARGET';

export interface BudgetCreateRequest {
  fiscalYearId: number;
  name: string;
  description?: string;
}

export interface BudgetUpdateRequest {
  name: string;
  description?: string;
}

export interface BudgetPeriodAmountRequest {
  accountingPeriodId: number;
  amount: number;
}

export interface BudgetLineRequest {
  accountId: number;
  annualAmount: number;
  allocationMethod: BudgetAllocationMethod;
  periodAmounts?: BudgetPeriodAmountRequest[];
  notes?: string;
}

export interface BudgetPeriodAllocationResponse {
  accountingPeriodId: number;
  periodName: string;
  periodNumber: number;
  startDate: string;
  endDate: string;
  budgetAmount: number;
}

export interface BudgetLineResponse {
  id: number;
  accountId: number;
  accountCode: string;
  accountName: string;
  accountType: string;
  annualAmount: number;
  allocationMethod: BudgetAllocationMethod;
  notes: string | null;
  periodAllocations: BudgetPeriodAllocationResponse[];
}

export interface BudgetResponse {
  id: number;
  budgetNumber: string;

  fiscalYearId: number;
  fiscalYearName: string;

  versionNumber: number;
  revisedFromBudgetId: number | null;

  name: string;
  description: string | null;
  status: BudgetStatus;

  totalRevenueBudget: number;
  totalExpenseBudget: number;

  activatedAt: string | null;
  closedAt: string | null;
  createdAt: string;

  lines: BudgetLineResponse[];
}

export interface BudgetVarianceLine {
  accountId: number;
  accountCode: string;
  accountName: string;
  accountType: string;

  budgetAmount: number;
  actualAmount: number;
  varianceAmount: number;
  variancePercent: number;
  varianceStatus: VarianceStatus;
  utilizationPercent: number;
}

export interface BudgetVarianceResponse {
  budgetId: number;
  budgetName: string;

  fiscalYearId: number;
  fiscalYearName: string;

  fromDate: string;
  toDate: string;

  totalRevenueBudget: number;
  totalRevenueActual: number;
  totalExpenseBudget: number;
  totalExpenseActual: number;

  lines: BudgetVarianceLine[];
}