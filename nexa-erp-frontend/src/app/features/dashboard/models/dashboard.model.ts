export interface DashboardSummary {
  users: UserSummary | null;
  security: SecuritySummary | null;
  finance: FinanceSummary | null;
  business: BusinessSummary | null;
  system: SystemSummary | null;
  recentActivities: RecentActivity[] | null;
  budget: BudgetDashboard | null;
  expense: ExpenseDashboard | null;
}

export interface BudgetTopAccount {
  accountId: number;
  accountCode: string;
  accountName: string;
  budgetAmount: number;
  actualAmount: number;
  utilizationPercent: number;
}

export interface BudgetDashboard {
  hasActiveBudget: boolean;
  activeBudgetId: number | null;
  activeBudgetName: string | null;
  unavailableReason: string | null;
  fromDate: string | null;
  toDate: string | null;
  currencyCode: string | null;

  totalExpenseBudget: number;
  totalExpenseActualYtd: number;
  expenseUtilizationPercent: number;

  totalRevenueBudget: number;
  totalRevenueActualYtd: number;
  revenueAchievementPercent: number;

  topAccounts: BudgetTopAccount[];
}

export interface ExpenseDashboard {
  draftCount: number;
  draftTotalAmount: number;
  postedThisMonthTotal: number;
  recurringActiveCount: number;
  recurringDueSoonCount: number;
  outstandingDue: number;
}

export interface BusinessSummary {
  cashPosition: number | null;
  cashConfigured: boolean | null;
  asOfDate: string;
  currencyCode: string | null;

  accountsReceivable: number;
  overdueInvoiceCount: number;
  overdueInvoiceAmount: number;

  accountsPayable: number;
  overdueBillCount: number;
  overdueBillAmount: number;

  revenueTrend: MonthlyTrend[];
  expenseTrend: MonthlyTrend[];
  currentMonthRevenue: number | null;
  currentMonthExpense: number | null;
  trendFromDate: string | null;
  trendToDate: string | null;
}

export interface DashboardWorkflowSummary {
  approvalEnabled: boolean;
  availablePendingCount: number | null;
  oldestAvailableSubmittedAt: string | null;
  myPendingCount: number | null;
  myReturnedCount: number | null;
  myApprovedUnconsumedCount: number | null;
}

export interface MonthlyTrend {
  month: string;
  amount: number;
}

export interface UserSummary {
  total: number;
  active: number;
  pending: number;
  inactive: number;
  locked: number;
}

export interface SecuritySummary {
  totalRoles: number;
  totalPermissions: number;
}

export interface FinanceSummary {
  totalAccounts: number;
  totalJournalEntries: number;
  postedJournalEntries: number;
  draftJournalEntries: number;
  reversedJournalEntries: number;
}

export interface SystemSummary {
  applicationVersion: string;
  serverTime: string;
  serverTimezone: string;
  environment: string;
  javaVersion: string;
}

export interface RecentActivity {
  action: string;
  entityName: string;
  entityId: number;
  userName: string;
  createdAt: string;
  description?: string;
}
