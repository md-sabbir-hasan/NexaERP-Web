import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { PERMISSIONS, PermissionCode } from '../../../../core/constants/permission.constants';
import { TokenService } from '../../../../core/services/token.service';

import { DashboardSummary, RecentActivity } from '../../models/dashboard.model';
import { DashboardService } from '../../services/dashboard.service';

interface ChartPoint {
  x: number;
  y: number;
  value: number;
  month: string;
}

interface TrendChartItem {
  month: string;
  revenue: number;
  expense: number;
}

interface TrendChart {
  width: number;
  height: number;

  revenueLine: string;
  revenueArea: string;

  expenseLine: string;
  expenseArea: string;

  revenuePoints: ChartPoint[];
  expensePoints: ChartPoint[];

  months: string[];

  gridLines: {
    y: number;
    value: number;
    label: string;
  }[];

  plotLeft: number;
  plotRight: number;
  plotTop: number;
  plotBottom: number;
  zeroLineY: number;

  totalRevenue: number;
  totalExpense: number;

  hasData: boolean;
  hasNegativeValues: boolean;
}

interface BudgetView {
  activeBudgetId: number | null;
  activeBudgetName: string | null;

  totalExpenseBudget: number;
  totalExpenseActualYtd: number;
  expenseUtilizationPercent: number;
  expenseProgressPercent: number;
  expenseOverBudget: boolean;

  totalRevenueBudget: number;
  totalRevenueActualYtd: number;
  revenueAchievementPercent: number;
  revenueProgressPercent: number;
  revenueTargetExceeded: boolean;

  topAccounts: {
    accountName: string;
    budgetAmount: number;
    actualAmount: number;
    utilizationPercent: number;
    progressPercent: number;
    isOverBudget: boolean;
  }[];
}

interface AttentionItem {
  id: string;
  type: 'critical' | 'warning' | 'info';
  icon: string;
  title: string;
  description: string;
  count: number;
  amount?: number;
  route: string;
}

interface QuickAction {
  id: string;
  label: string;
  description: string;
  icon: string;
  route: string;
  permission: PermissionCode;
  emphasis: 'primary' | 'standard';
}

interface DashboardDisplayValues {
  cashPosition: number;
  accountsReceivable: number;
  accountsPayable: number;
  postedThisMonthTotal: number;
}

interface ServiceStatusItem {
  id: string;
  label: string;
  icon: string;
  status: string;
}

interface ExpenseDonutSegment {
  accountName: string;
  amount: number;
  percentage: number;
  strokeDasharray: string;
  strokeDashoffset: number;
  colorClass: string;
}

interface ExpenseDonutView {
  radius: number;
  circumference: number;
  totalAmount: number;
  segments: ExpenseDonutSegment[];
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, DatePipe],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit, OnDestroy {
  private readonly dashboardService = inject(DashboardService);
  private readonly tokenService = inject(TokenService);

  readonly permissions = PERMISSIONS;
  readonly today = new Date();

  readonly summary = signal<DashboardSummary | null>(null);

  readonly loading = signal(true);
  readonly refreshing = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly lastUpdatedAt = signal<Date | null>(null);

  readonly selectedTrendIndex = signal<number | null>(null);

  /*
   * Permission values JWT token থেকে load করা হবে।
   * Dashboard কোথাও role name check করবে না।
   */
  readonly grantedPermissions = signal<ReadonlySet<string>>(new Set<string>());

  readonly displayValues = signal<DashboardDisplayValues>({
    cashPosition: 0,
    accountsReceivable: 0,
    accountsPayable: 0,
    postedThisMonthTotal: 0,
  });

  private countAnimationFrameId: number | null = null;

  // =========================================================
  // Permission-based widget visibility
  // =========================================================

  readonly canViewCashPosition = computed(() => this.hasPermission(PERMISSIONS.VIEW_BANKING));

  readonly canViewReceivable = computed(() => this.hasPermission(PERMISSIONS.VIEW_INVOICE));

  readonly canViewPayable = computed(() => this.hasPermission(PERMISSIONS.VIEW_VENDOR_BILL));

  /*
   * Current permission.constants.ts-এ আলাদা VIEW_BUDGET এবং
   * VIEW_EXPENSE permission নেই।
   *
   * তাই dashboard analytics আপাতত VIEW_REPORT permission-এর
   * অধীনে দেখানো হচ্ছে।
   */
  readonly canViewTrendChart = computed(() =>
    this.hasAnyPermission(
      PERMISSIONS.VIEW_REPORT,
      PERMISSIONS.VIEW_LEDGER,
      PERMISSIONS.VIEW_TRIAL_BALANCE,
    ),
  );

  readonly canViewBudget = computed(() => this.hasPermission(PERMISSIONS.VIEW_REPORT));

  readonly canViewExpenseSummary = computed(() => this.hasPermission(PERMISSIONS.VIEW_REPORT));

  readonly canViewJournal = computed(() => this.hasPermission(PERMISSIONS.VIEW_JOURNAL));

  readonly canViewAccounts = computed(() => this.hasPermission(PERMISSIONS.VIEW_ACCOUNTS));

  readonly canViewUserAdministration = computed(() => this.hasPermission(PERMISSIONS.MANAGE_USERS));

  readonly canViewSecurityAdministration = computed(() =>
    this.hasAnyPermission(PERMISSIONS.MANAGE_ROLES, PERMISSIONS.MANAGE_PERMISSIONS),
  );

  readonly canViewServiceStatus = computed(() => this.hasPermission(PERMISSIONS.MANAGE_SETTINGS));

  readonly canViewRecentActivities = computed(() =>
    this.hasPermission(PERMISSIONS.VIEW_AUDIT_LOGS),
  );

  readonly canViewAttentionCenter = computed(
    () =>
      this.canViewReceivable() ||
      this.canViewPayable() ||
      this.canViewJournal() ||
      this.canViewExpenseSummary(),
  );

  readonly hasVisibleDashboardWidget = computed(
    () =>
      this.canViewCashPosition() ||
      this.canViewReceivable() ||
      this.canViewPayable() ||
      this.canViewTrendChart() ||
      this.canViewBudget() ||
      this.canViewExpenseSummary() ||
      this.canViewJournal() ||
      this.canViewAccounts() ||
      this.canViewUserAdministration() ||
      this.canViewSecurityAdministration() ||
      this.canViewServiceStatus() ||
      this.canViewRecentActivities() ||
      this.quickActions().length > 0,
  );

  // =========================================================
  // Revenue vs Expense chart
  // =========================================================

  readonly trendChartItems = computed<TrendChartItem[]>(() => {
    const dashboard = this.summary();

    if (!dashboard) {
      return [];
    }

    const revenueTrend = dashboard.business.revenueTrend ?? [];

    const expenseTrend = dashboard.business.expenseTrend ?? [];

    const months: string[] = [];

    const revenueByMonth = new Map<string, number>();
    const expenseByMonth = new Map<string, number>();

    revenueTrend.forEach((item) => {
      if (!months.includes(item.month)) {
        months.push(item.month);
      }

      revenueByMonth.set(item.month, this.toSafeNumber(item.amount));
    });

    expenseTrend.forEach((item) => {
      if (!months.includes(item.month)) {
        months.push(item.month);
      }

      expenseByMonth.set(item.month, this.toSafeNumber(item.amount));
    });

    return months.map((month) => ({
      month,
      revenue: revenueByMonth.get(month) ?? 0,
      expense: expenseByMonth.get(month) ?? 0,
    }));
  });

  readonly trendChart = computed<TrendChart | null>(() => {
    const items = this.trendChartItems();

    if (items.length === 0) {
      return null;
    }

    const width = 760;
    const height = 300;

    const paddingLeft = 68;
    const paddingRight = 24;
    const paddingTop = 24;
    const paddingBottom = 52;

    const plotLeft = paddingLeft;
    const plotRight = width - paddingRight;
    const plotTop = paddingTop;
    const plotBottom = height - paddingBottom;

    const plotWidth = plotRight - plotLeft;
    const plotHeight = plotBottom - plotTop;

    const allValues = items.flatMap((item) => [item.revenue, item.expense]);

    const rawMinimum = Math.min(...allValues, 0);
    const rawMaximum = Math.max(...allValues, 0);

    const hasNegativeValues = rawMinimum < 0;
    const hasData = allValues.some((value) => value !== 0);

    let minimumValue = rawMinimum;
    let maximumValue = rawMaximum;

    if (minimumValue === maximumValue) {
      if (minimumValue === 0) {
        maximumValue = 1;
      } else {
        const padding = Math.abs(minimumValue) * 0.1 || 1;

        minimumValue -= padding;
        maximumValue += padding;
      }
    } else {
      const rangePadding = (maximumValue - minimumValue) * 0.08;

      minimumValue -= rangePadding;
      maximumValue += rangePadding;
    }

    const valueRange = maximumValue - minimumValue;

    const stepX = items.length > 1 ? plotWidth / (items.length - 1) : 0;

    const getX = (index: number): number => {
      if (items.length === 1) {
        return plotLeft + plotWidth / 2;
      }

      return plotLeft + stepX * index;
    };

    const getY = (value: number): number =>
      plotTop + ((maximumValue - value) / valueRange) * plotHeight;

    const revenuePoints: ChartPoint[] = items.map((item, index) => ({
      x: getX(index),
      y: getY(item.revenue),
      value: item.revenue,
      month: item.month,
    }));

    const expensePoints: ChartPoint[] = items.map((item, index) => ({
      x: getX(index),
      y: getY(item.expense),
      value: item.expense,
      month: item.month,
    }));

    const revenueLine = this.createSmoothPath(revenuePoints);

    const expenseLine = this.createSmoothPath(expensePoints);

    const zeroLineY = this.clamp(getY(0), plotTop, plotBottom);

    const revenueArea = this.createAreaPath(revenuePoints, zeroLineY);

    const expenseArea = this.createAreaPath(expensePoints, zeroLineY);

    const gridStepCount = 4;

    const gridLines = Array.from({ length: gridStepCount + 1 }, (_, index) => {
      const fraction = index / gridStepCount;

      const value = maximumValue - fraction * (maximumValue - minimumValue);

      return {
        y: plotTop + fraction * plotHeight,
        value,
        label: this.formatCompact(value),
      };
    });

    return {
      width,
      height,

      revenueLine,
      revenueArea,

      expenseLine,
      expenseArea,

      revenuePoints,
      expensePoints,

      months: items.map((item) => item.month),

      gridLines,

      plotLeft,
      plotRight,
      plotTop,
      plotBottom,
      zeroLineY,

      totalRevenue: items.reduce((total, item) => total + item.revenue, 0),

      totalExpense: items.reduce((total, item) => total + item.expense, 0),

      hasData,
      hasNegativeValues,
    };
  });

  readonly selectedTrend = computed<TrendChartItem | null>(() => {
    const index = this.selectedTrendIndex();
    const items = this.trendChartItems();

    if (index === null || index < 0 || index >= items.length) {
      return null;
    }

    return items[index];
  });

  readonly selectedTrendX = computed<number | null>(() => {
    const index = this.selectedTrendIndex();
    const chart = this.trendChart();

    if (index === null || !chart || index < 0 || index >= chart.revenuePoints.length) {
      return null;
    }

    return chart.revenuePoints[index].x;
  });

  // =========================================================
  // Budget data
  // =========================================================

  readonly budgetView = computed<BudgetView | null>(() => {
    const budget = this.summary()?.budget;

    if (!budget?.hasActiveBudget) {
      return null;
    }

    const expenseUtilizationPercent = this.toSafeNumber(budget.expenseUtilizationPercent);

    const revenueAchievementPercent = this.toSafeNumber(budget.revenueAchievementPercent);

    return {
      activeBudgetId: budget.activeBudgetId,
      activeBudgetName: budget.activeBudgetName,

      totalExpenseBudget: this.toSafeNumber(budget.totalExpenseBudget),

      totalExpenseActualYtd: this.toSafeNumber(budget.totalExpenseActualYtd),

      expenseUtilizationPercent,

      expenseProgressPercent: this.progressPercent(expenseUtilizationPercent),

      expenseOverBudget: expenseUtilizationPercent > 100,

      totalRevenueBudget: this.toSafeNumber(budget.totalRevenueBudget),

      totalRevenueActualYtd: this.toSafeNumber(budget.totalRevenueActualYtd),

      revenueAchievementPercent,

      revenueProgressPercent: this.progressPercent(revenueAchievementPercent),

      revenueTargetExceeded: revenueAchievementPercent > 100,

      topAccounts: (budget.topAccounts ?? []).map((account) => {
        const utilizationPercent = this.toSafeNumber(account.utilizationPercent);

        return {
          accountName: account.accountName,

          budgetAmount: this.toSafeNumber(account.budgetAmount),

          actualAmount: this.toSafeNumber(account.actualAmount),

          utilizationPercent,

          progressPercent: this.progressPercent(utilizationPercent),

          isOverBudget: utilizationPercent > 100,
        };
      }),
    };
  });

  // =========================================================
  // Expense donut chart
  // =========================================================

  readonly expenseDonut = computed<ExpenseDonutView | null>(() => {
    const budget = this.summary()?.budget;

    if (!budget?.hasActiveBudget) {
      return null;
    }

    const accounts = (budget.topAccounts ?? [])
      .map((account) => ({
        accountName: account.accountName,
        amount: this.toSafeNumber(account.actualAmount),
      }))
      .filter((account) => account.amount > 0)
      .sort((first, second) => second.amount - first.amount)
      .slice(0, 6);

    const totalAmount = accounts.reduce((total, account) => total + account.amount, 0);

    if (totalAmount <= 0) {
      return null;
    }

    const radius = 68;
    const circumference = 2 * Math.PI * radius;

    let accumulatedPercentage = 0;

    const segments: ExpenseDonutSegment[] = accounts.map((account, index) => {
      const percentage = (account.amount / totalAmount) * 100;

      const segmentLength = (percentage / 100) * circumference;

      const strokeDashoffset = -((accumulatedPercentage / 100) * circumference);

      accumulatedPercentage += percentage;

      return {
        accountName: account.accountName,
        amount: account.amount,
        percentage,
        strokeDasharray: `${segmentLength} ${circumference - segmentLength}`,
        strokeDashoffset,
        colorClass: `donut-segment-${index + 1}`,
      };
    });

    return {
      radius,
      circumference,
      totalAmount,
      segments,
    };
  });

  // =========================================================
  // Attention center
  // =========================================================

  readonly attentionItems = computed<AttentionItem[]>(() => {
    const dashboard = this.summary();

    if (!dashboard) {
      return [];
    }

    const items: AttentionItem[] = [];

    if (this.canViewReceivable() && dashboard.business.overdueInvoiceCount > 0) {
      const count = dashboard.business.overdueInvoiceCount;

      items.push({
        id: 'overdue-invoices',
        type: 'critical',
        icon: 'bi-receipt',
        title: 'Overdue receivables',
        description: `${count} overdue ${count === 1 ? 'invoice' : 'invoices'}`,
        count,
        amount: dashboard.business.overdueInvoiceAmount,
        route: '/invoice',
      });
    }

    if (this.canViewPayable() && dashboard.business.overdueBillCount > 0) {
      const count = dashboard.business.overdueBillCount;

      items.push({
        id: 'overdue-vendor-bills',
        type: 'critical',
        icon: 'bi-file-earmark-text',
        title: 'Overdue vendor bills',
        description: `${count} overdue vendor ${count === 1 ? 'bill' : 'bills'}`,
        count,
        amount: dashboard.business.overdueBillAmount,
        route: '/vendor-bill',
      });
    }

    if (this.canViewExpenseSummary() && dashboard.expense.draftCount > 0) {
      const count = dashboard.expense.draftCount;

      /*
       * Current app.routes.ts-এ expense route নেই।
       * তাই broken link না দিয়ে reports route ব্যবহার করা হয়েছে।
       */
      items.push({
        id: 'draft-expenses',
        type: 'warning',
        icon: 'bi-wallet2',
        title: 'Draft expenses',
        description: `${count} draft ${count === 1 ? 'expense' : 'expenses'} pending`,
        count,
        amount: dashboard.expense.draftTotalAmount,
        route: '/reports',
      });
    }

    if (this.canViewJournal() && dashboard.finance.draftJournalEntries > 0) {
      const count = dashboard.finance.draftJournalEntries;

      items.push({
        id: 'draft-journals',
        type: 'info',
        icon: 'bi-journal-text',
        title: 'Draft journal entries',
        description: `${count} journal ${count === 1 ? 'entry' : 'entries'} waiting for posting`,
        count,
        route: '/journals',
      });
    }

    return items;
  });

  // =========================================================
  // Permission-based quick actions
  // =========================================================

  readonly quickActions = computed<QuickAction[]>(() => {
    const actions: QuickAction[] = [
      {
        id: 'create-invoice',
        label: 'Create invoice',
        description: 'Create a customer invoice',
        icon: 'bi-receipt',
        route: '/invoice/new',
        permission: PERMISSIONS.CREATE_INVOICE,
        emphasis: 'primary',
      },
      {
        id: 'create-vendor-bill',
        label: 'Vendor bill',
        description: 'Record a supplier bill',
        icon: 'bi-file-earmark-plus',
        route: '/vendor-bill/new',
        permission: PERMISSIONS.CREATE_VENDOR_BILL,
        emphasis: 'standard',
      },
      {
        id: 'create-journal',
        label: 'Journal entry',
        description: 'Create a manual journal',
        icon: 'bi-journal-plus',
        route: '/journals/new',
        permission: PERMISSIONS.CREATE_JOURNAL,
        emphasis: 'standard',
      },
      {
        id: 'record-payment',
        label: 'Record payment',
        description: 'Receive or make a payment',
        icon: 'bi-credit-card',
        route: '/payment/new',
        permission: PERMISSIONS.CREATE_PAYMENT,
        emphasis: 'standard',
      },
      {
        id: 'view-banking',
        label: 'Banking',
        description: 'Review bank information',
        icon: 'bi-bank',
        route: '/reports',
        permission: PERMISSIONS.VIEW_BANKING,
        emphasis: 'standard',
      },
      {
        id: 'view-accounts',
        label: 'Accounts',
        description: 'Open chart of accounts',
        icon: 'bi-diagram-3',
        route: '/accounts',
        permission: PERMISSIONS.VIEW_ACCOUNTS,
        emphasis: 'standard',
      },
      {
        id: 'view-reports',
        label: 'Reports',
        description: 'View financial reports',
        icon: 'bi-bar-chart-line',
        route: '/reports',
        permission: PERMISSIONS.VIEW_REPORT,
        emphasis: 'standard',
      },
      {
        id: 'manage-users',
        label: 'Users',
        description: 'Manage system users',
        icon: 'bi-people',
        route: '/users',
        permission: PERMISSIONS.MANAGE_USERS,
        emphasis: 'standard',
      },
      {
        id: 'manage-roles',
        label: 'Roles',
        description: 'Manage access roles',
        icon: 'bi-shield-lock',
        route: '/roles',
        permission: PERMISSIONS.MANAGE_ROLES,
        emphasis: 'standard',
      },
      {
        id: 'manage-permissions',
        label: 'Permissions',
        description: 'Review system permissions',
        icon: 'bi-key',
        route: '/permissions',
        permission: PERMISSIONS.MANAGE_PERMISSIONS,
        emphasis: 'standard',
      },
    ];

    return actions.filter((action) => this.hasPermission(action.permission));
  });

  // =========================================================
  // Service status
  // =========================================================

  readonly reportedServices = computed<ServiceStatusItem[]>(() => {
    const dashboard = this.summary();

    if (!dashboard) {
      return [];
    }

    return [
      {
        id: 'application',
        label: 'Application',
        icon: 'bi-box',
        status: dashboard.health.application,
      },
      {
        id: 'database',
        label: 'Database',
        icon: 'bi-database',
        status: dashboard.health.database,
      },
      {
        id: 'mail',
        label: 'Mail service',
        icon: 'bi-envelope',
        status: dashboard.health.mail,
      },
    ];
  });

  // =========================================================
  // Component lifecycle
  // =========================================================

  ngOnInit(): void {
    this.refreshPermissions();
    this.loadDashboard();
  }

  ngOnDestroy(): void {
    this.cancelCountAnimation();
  }

  // =========================================================
  // API loading
  // =========================================================

  loadDashboard(forceRefresh = false): void {
    if (this.refreshing()) {
      return;
    }

    const hasExistingData = this.summary() !== null;

    if (forceRefresh || hasExistingData) {
      this.refreshing.set(true);
    } else {
      this.loading.set(true);
    }

    this.errorMessage.set(null);

    this.dashboardService
      .getSummary()
      .pipe(
        finalize(() => {
          this.loading.set(false);
          this.refreshing.set(false);
        }),
      )
      .subscribe({
        next: (response) => {
          if (!response?.data) {
            this.errorMessage.set('Dashboard data was not returned by the server.');
            return;
          }

          this.summary.set(response.data);
          this.lastUpdatedAt.set(new Date());
          this.selectedTrendIndex.set(null);

          this.animateSummaryValues(response.data);
        },

        error: () => {
          this.errorMessage.set('Dashboard data could not be loaded. Please try again.');
        },
      });
  }

  refreshDashboard(): void {
    this.refreshPermissions();
    this.loadDashboard(true);
  }

  retryLoad(): void {
    this.loadDashboard();
  }

  // =========================================================
  // Permission methods
  // =========================================================

  refreshPermissions(): void {
    const permissions = this.tokenService.getPermissions();

    this.grantedPermissions.set(new Set<string>(permissions));
  }

  hasPermission(permission: string): boolean {
    return this.grantedPermissions().has(permission);
  }

  hasAnyPermission(...permissions: string[]): boolean {
    return permissions.some((permission) => this.hasPermission(permission));
  }

  // =========================================================
  // Chart interaction
  // =========================================================

  selectTrendPoint(index: number): void {
    this.selectedTrendIndex.set(index);
  }

  clearTrendPoint(): void {
    this.selectedTrendIndex.set(null);
  }

  // =========================================================
  // UI helpers
  // =========================================================

  minPct(value: number): number {
    return this.progressPercent(value);
  }

  progressPercent(value: number | null | undefined): number {
    return this.clamp(this.toSafeNumber(value), 0, 100);
  }

  healthClass(status: string | null | undefined): string {
    const normalizedStatus = (status ?? '').trim().toUpperCase();

    if (
      normalizedStatus.includes('UP') ||
      normalizedStatus.includes('OK') ||
      normalizedStatus.includes('HEALTHY') ||
      normalizedStatus.includes('CONNECTED')
    ) {
      return 'status-up';
    }

    if (normalizedStatus.includes('WARN') || normalizedStatus.includes('DEGRADED')) {
      return 'status-warning';
    }

    return 'status-down';
  }

  activityClass(action: string | null | undefined): string {
    const normalizedAction = (action ?? '').trim().toUpperCase();

    const actionClasses: Record<string, string> = {
      CREATED: 'activity-created',
      POSTED: 'activity-posted',
      ACTIVATED: 'activity-created',
      APPROVED: 'activity-approved',
      UPDATED: 'activity-updated',
      CLOSED: 'activity-closed',
      CANCELLED: 'activity-cancelled',
      DELETED: 'activity-deleted',
      REVERSED: 'activity-reversed',
      DEACTIVATED: 'activity-deactivated',
      LOGIN: 'activity-login',
    };

    return actionClasses[normalizedAction] ?? 'activity-default';
  }

  /*
   * Existing HTML compatibility-এর জন্য রাখা হয়েছে।
   */
  activityColor(action: string): string {
    const colorMap: Record<string, string> = {
      CREATED: 'green',
      POSTED: 'brass',
      ACTIVATED: 'green',
      APPROVED: 'green',
      CANCELLED: 'red',
      DELETED: 'red',
      REVERSED: 'red',
      DEACTIVATED: 'red',
      UPDATED: 'slate',
      CLOSED: 'slate',
    };

    return colorMap[action] ?? 'slate';
  }

  activityIcon(action: string | null | undefined): string {
    const normalizedAction = (action ?? '').trim().toUpperCase();

    const icons: Record<string, string> = {
      CREATED: 'bi-plus-lg',
      POSTED: 'bi-check2-circle',
      ACTIVATED: 'bi-person-check',
      APPROVED: 'bi-patch-check',
      UPDATED: 'bi-pencil',
      CLOSED: 'bi-lock',
      CANCELLED: 'bi-x-lg',
      DELETED: 'bi-trash3',
      REVERSED: 'bi-arrow-counterclockwise',
      DEACTIVATED: 'bi-person-dash',
      LOGIN: 'bi-box-arrow-in-right',
    };

    return icons[normalizedAction] ?? 'bi-clock-history';
  }

  formatEntityName(entityName: string | null | undefined): string {
    if (!entityName) {
      return 'Record';
    }

    return entityName
      .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
      .replace(/[_-]+/g, ' ')
      .replace(/\s+/g, ' ')
      .trim()
      .replace(/\b\w/g, (character) => character.toUpperCase());
  }

  activityIdentity(activity: RecentActivity, index: number): string {
    return [
      activity.action || 'ACTION',
      activity.entityName || 'ENTITY',
      activity.entityId,
      activity.createdAt || index,
      index,
    ].join('-');
  }

  formatCompact(value: number): string {
    const safeValue = this.toSafeNumber(value);
    const absoluteValue = Math.abs(safeValue);
    const sign = safeValue < 0 ? '-' : '';

    if (absoluteValue >= 10_000_000) {
      return `${sign}${(absoluteValue / 10_000_000).toFixed(1)}Cr`;
    }

    if (absoluteValue >= 100_000) {
      return `${sign}${(absoluteValue / 100_000).toFixed(1)}L`;
    }

    if (absoluteValue >= 1_000) {
      return `${sign}${(absoluteValue / 1_000).toFixed(1)}K`;
    }

    return safeValue.toFixed(0);
  }

  // =========================================================
  // Private chart helpers
  // =========================================================

  private createSmoothPath(points: ChartPoint[]): string {
    if (points.length === 0) {
      return '';
    }

    if (points.length === 1) {
      const point = points[0];

      return `M ${point.x.toFixed(2)} ` + `${point.y.toFixed(2)}`;
    }

    let path = `M ${points[0].x.toFixed(2)} ` + `${points[0].y.toFixed(2)}`;

    for (let index = 0; index < points.length - 1; index++) {
      const current = points[index];
      const next = points[index + 1];

      const controlOffset = (next.x - current.x) * 0.42;

      const controlPointOneX = current.x + controlOffset;

      const controlPointTwoX = next.x - controlOffset;

      path +=
        ` C ${controlPointOneX.toFixed(2)} ` +
        `${current.y.toFixed(2)}` +
        ` ${controlPointTwoX.toFixed(2)} ` +
        `${next.y.toFixed(2)}` +
        ` ${next.x.toFixed(2)} ` +
        `${next.y.toFixed(2)}`;
    }

    return path;
  }

  private createAreaPath(points: ChartPoint[], baselineY: number): string {
    if (points.length === 0) {
      return '';
    }

    const linePath = this.createSmoothPath(points);

    const firstPoint = points[0];
    const lastPoint = points[points.length - 1];

    return (
      `${linePath}` +
      ` L ${lastPoint.x.toFixed(2)} ` +
      `${baselineY.toFixed(2)}` +
      ` L ${firstPoint.x.toFixed(2)} ` +
      `${baselineY.toFixed(2)} Z`
    );
  }

  // =========================================================
  // Count-up animation
  // =========================================================

  private animateSummaryValues(dashboard: DashboardSummary): void {
    this.cancelCountAnimation();

    const startValues = this.displayValues();

    const targetValues: DashboardDisplayValues = {
      cashPosition: this.toSafeNumber(dashboard.business.cashPosition),

      accountsReceivable: this.toSafeNumber(dashboard.business.accountsReceivable),

      accountsPayable: this.toSafeNumber(dashboard.business.accountsPayable),

      postedThisMonthTotal: this.toSafeNumber(dashboard.expense.postedThisMonthTotal),
    };

    if (
      typeof window === 'undefined' ||
      typeof window.requestAnimationFrame !== 'function' ||
      this.prefersReducedMotion()
    ) {
      this.displayValues.set(targetValues);
      return;
    }

    const duration = 850;
    const startedAt = performance.now();

    const animate = (currentTime: number): void => {
      const elapsed = currentTime - startedAt;

      const progress = this.clamp(elapsed / duration, 0, 1);

      const easedProgress = 1 - Math.pow(1 - progress, 3);

      this.displayValues.set({
        cashPosition: this.interpolate(
          startValues.cashPosition,
          targetValues.cashPosition,
          easedProgress,
        ),

        accountsReceivable: this.interpolate(
          startValues.accountsReceivable,
          targetValues.accountsReceivable,
          easedProgress,
        ),

        accountsPayable: this.interpolate(
          startValues.accountsPayable,
          targetValues.accountsPayable,
          easedProgress,
        ),

        postedThisMonthTotal: this.interpolate(
          startValues.postedThisMonthTotal,
          targetValues.postedThisMonthTotal,
          easedProgress,
        ),
      });

      if (progress < 1) {
        this.countAnimationFrameId = window.requestAnimationFrame(animate);
      } else {
        this.displayValues.set(targetValues);
        this.countAnimationFrameId = null;
      }
    };

    this.countAnimationFrameId = window.requestAnimationFrame(animate);
  }

  private cancelCountAnimation(): void {
    if (this.countAnimationFrameId !== null && typeof window !== 'undefined') {
      window.cancelAnimationFrame(this.countAnimationFrameId);

      this.countAnimationFrameId = null;
    }
  }

  private prefersReducedMotion(): boolean {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
      return false;
    }

    return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  }

  private interpolate(start: number, end: number, progress: number): number {
    return start + (end - start) * progress;
  }

  private toSafeNumber(value: number | null | undefined): number {
    const numericValue = Number(value);

    return Number.isFinite(numericValue) ? numericValue : 0;
  }

  private clamp(value: number, minimum: number, maximum: number): number {
    return Math.min(Math.max(value, minimum), maximum);
  }
}
