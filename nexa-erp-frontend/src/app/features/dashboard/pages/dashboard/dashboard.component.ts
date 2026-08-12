import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { PERMISSIONS, PermissionCode } from '../../../../core/constants/permission.constants';
import { TokenService } from '../../../../core/services/token.service';

import { NotificationResponse } from '../../../notifications/models/notification.model';
import { NotificationStore } from '../../../notifications/services/notification.store';
import { BankAccount } from '../../../banking/models/bank-account.model';
import { BankAccountService } from '../../../banking/services/bank-account.service';
import { DashboardSummary, DashboardWorkflowSummary, RecentActivity } from '../../models/dashboard.model';
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
  currentMonthRevenue: number;
  currentMonthExpense: number;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, DatePipe],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit {
  private readonly dashboardService = inject(DashboardService);
  private readonly tokenService = inject(TokenService);
  private readonly bankAccountService = inject(BankAccountService);
  private readonly notificationStore = inject(NotificationStore);
  private readonly router = inject(Router);

  readonly permissions = PERMISSIONS;
  readonly today = new Date();

  readonly summary = signal<DashboardSummary | null>(null);
  readonly workflowSummary = signal<DashboardWorkflowSummary | null>(null);

  readonly loading = signal(true);
  readonly refreshing = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly workflowError = signal<string | null>(null);
  readonly workflowLoading = signal(false);
  readonly lastUpdatedAt = signal<Date | null>(null);

  readonly selectedTrendIndex = signal<number | null>(null);
  readonly cashAccounts = signal<BankAccount[]>([]);
  readonly cashAccountsLoading = signal(false);

  
  readonly grantedPermissions = signal<ReadonlySet<string>>(new Set<string>());

  readonly displayValues = signal<DashboardDisplayValues>({
    cashPosition: 0,
    accountsReceivable: 0,
    accountsPayable: 0,
    postedThisMonthTotal: 0,
    currentMonthRevenue: 0,
    currentMonthExpense: 0,
  });

  // =========================================================
  // Permission-based widget visibility
  // =========================================================

  readonly canViewCashPosition = computed(() => 
    this.hasPermission(PERMISSIONS.VIEW_BANKING));

  readonly visibleCashAccounts = computed(() =>
    this.cashAccounts()
      .filter((account) => account.isActive)
      .sort((a, b) => b.currentBalance - a.currentBalance),
  );

  readonly canViewReceivable = computed(() => this.hasPermission(PERMISSIONS.VIEW_INVOICE));

  readonly canViewPayable = computed(() => this.hasPermission(PERMISSIONS.VIEW_VENDOR_BILL));

  /*
   * Current permission.constants.ts-এ আলাদা VIEW_BUDGET এবং
   * VIEW_EXPENSE permission নেই।
   *
   * তাই dashboard analytics আপাতত VIEW_REPORT permission-এর
   * অধীনে দেখানো হচ্ছে।
   */
  readonly canViewTrendChart = computed(() => this.hasPermission(PERMISSIONS.VIEW_REPORT));

  readonly canViewBudget = computed(() => this.hasPermission(PERMISSIONS.VIEW_BUDGET_REPORT));

  readonly canViewExpenseSummary = computed(() => this.hasPermission(PERMISSIONS.VIEW_EXPENSE));
  readonly canViewRecurringExpense = computed(() =>
    this.hasPermission(PERMISSIONS.VIEW_RECURRING_EXPENSE),
  );

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

  readonly canViewApprovalQueue = computed(() =>
    this.hasPermission(PERMISSIONS.VIEW_APPROVAL_QUEUE),
  );

  readonly latestUnreadNotifications = computed(() =>
    this.notificationStore.dashboardPreview().slice(0, 3),
  );
  readonly notificationError = this.notificationStore.dashboardPreviewError;
  readonly notificationLoading = this.notificationStore.dashboardPreviewLoading;
  readonly unreadNotificationCount = this.notificationStore.unreadCount;

  readonly canViewAttentionCenter = computed(
    () =>
      this.canViewReceivable() ||
      this.canViewPayable() ||
      this.canViewJournal() ||
      this.canViewExpenseSummary() ||
      this.workflowSummary()?.approvalEnabled === true ||
      this.unreadNotificationCount() > 0 ||
      this.notificationLoading() ||
      this.notificationError() !== null,
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

    const revenueTrend = dashboard.business?.revenueTrend ?? [];

    const expenseTrend = dashboard.business?.expenseTrend ?? [];

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
  // Attention center
  // =========================================================

  readonly attentionItems = computed<AttentionItem[]>(() => {
    const dashboard = this.summary();

    if (!dashboard) {
      return [];
    }

    const items: AttentionItem[] = [];

    const workflow = this.workflowSummary();
    if (workflow?.approvalEnabled) {
      if ((workflow.myPendingCount ?? 0) > 0) {
        const count = workflow.myPendingCount ?? 0;
        items.push({
          id: 'my-pending-requests', type: 'info', icon: 'bi-send-check',
          title: 'My pending requests',
          description: `${count} submitted ${count === 1 ? 'request is' : 'requests are'} awaiting a decision`,
          count, route: '/approvals/my-requests',
        });
      }
      if (this.canViewApprovalQueue() && (workflow.availablePendingCount ?? 0) > 0) {
        const count = workflow.availablePendingCount ?? 0;
        items.push({
          id: 'available-approvals', type: 'warning', icon: 'bi-check2-square',
          title: 'Approvals waiting for you',
          description: `${count} ${count === 1 ? 'request' : 'requests'} available${this.pendingAgeSuffix(workflow.oldestAvailableSubmittedAt)}`,
          count, route: '/approvals',
        });
      }
      if ((workflow.myReturnedCount ?? 0) > 0) {
        const count = workflow.myReturnedCount ?? 0;
        items.push({
          id: 'returned-requests', type: 'critical', icon: 'bi-arrow-return-left',
          title: 'Returned for correction',
          description: `${count} of your ${count === 1 ? 'request requires' : 'requests require'} attention`,
          count, route: '/approvals/my-requests',
        });
      }
      if ((workflow.myApprovedUnconsumedCount ?? 0) > 0) {
        const count = workflow.myApprovedUnconsumedCount ?? 0;
        items.push({
          id: 'approved-unposted', type: 'info', icon: 'bi-hourglass-split',
          title: 'Approved, awaiting posting',
          description: `${count} approved ${count === 1 ? 'request is' : 'requests are'} not yet consumed`,
          count, route: '/approvals/my-requests',
        });
      }
    }

    if (this.canViewReceivable() && (dashboard.business?.overdueInvoiceCount ?? 0) > 0) {
      const count = dashboard.business?.overdueInvoiceCount ?? 0;

      items.push({
        id: 'overdue-invoices',
        type: 'critical',
        icon: 'bi-receipt',
        title: 'Overdue receivables',
        description: `${count} overdue ${count === 1 ? 'invoice' : 'invoices'}`,
        count,
        amount: dashboard.business?.overdueInvoiceAmount,
        route: '/invoice',
      });
    }

    if (this.canViewPayable() && (dashboard.business?.overdueBillCount ?? 0) > 0) {
      const count = dashboard.business?.overdueBillCount ?? 0;

      items.push({
        id: 'overdue-vendor-bills',
        type: 'critical',
        icon: 'bi-file-earmark-text',
        title: 'Overdue vendor bills',
        description: `${count} overdue vendor ${count === 1 ? 'bill' : 'bills'}`,
        count,
        amount: dashboard.business?.overdueBillAmount,
        route: '/vendor-bill',
      });
    }

    if (this.canViewExpenseSummary() && (dashboard.expense?.draftCount ?? 0) > 0) {
      const count = dashboard.expense?.draftCount ?? 0;

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
        amount: dashboard.expense?.draftTotalAmount,
        route: '/expense',
      });
    }

    if (this.canViewJournal() && (dashboard.finance?.draftJournalEntries ?? 0) > 0) {
      const count = dashboard.finance?.draftJournalEntries ?? 0;

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

    if (this.unreadNotificationCount() > 0) {
      const count = this.unreadNotificationCount();
      items.push({
        id: 'unread-notifications', type: 'info', icon: 'bi-bell',
        title: 'Unread notifications',
        description: `${count} unread ${count === 1 ? 'notification' : 'notifications'}`,
        count, route: '/notifications',
      });
    }

    return items.slice(0, 6);
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
        id: 'create-expense',
        label: 'New expense',
        description: 'Record an expense',
        icon: 'bi-wallet2',
        route: '/expense/new',
        permission: PERMISSIONS.CREATE_EXPENSE,
        emphasis: 'standard',
      },
      {
        id: 'approval-queue',
        label: 'Approval queue',
        description: 'Review pending approvals',
        icon: 'bi-check2-square',
        route: '/approvals',
        permission: PERMISSIONS.VIEW_APPROVAL_QUEUE,
        emphasis: 'standard',
      },
      {
        id: 'view-banking',
        label: 'Banking',
        description: 'Review bank information',
        icon: 'bi-bank',
        route: '/banking',
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

    return actions.filter((action) => this.hasPermission(action.permission)).slice(0, 6);
  });

  // =========================================================
  // Component lifecycle
  // =========================================================

  ngOnInit(): void {
    this.refreshPermissions();
    this.loadDashboard();
    this.loadWorkflowSummary();
    this.loadNotifications();
    this.loadCashAccounts();
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

          this.syncSummaryValues(response.data);
        },

        error: (error: { status?: number }) => {
          if (error.status === 401) {
            this.errorMessage.set('Your session has expired. Please sign in again.');
          } else if (error.status === 403) {
            this.errorMessage.set('You are not authorized to load dashboard data.');
          } else {
            this.errorMessage.set('Dashboard service is unavailable. Please try again.');
          }
        },
      });
  }

  refreshDashboard(): void {
    this.refreshPermissions();
    this.loadDashboard(true);
    this.loadWorkflowSummary();
    this.loadNotifications(true);
    this.loadCashAccounts();
  }

  retryLoad(): void {
    this.loadDashboard();
  }

  retryWorkflow(): void {
    this.loadWorkflowSummary();
  }

  retryNotifications(): void {
    this.loadNotifications(true);
  }

  openNotification(notification: NotificationResponse): void {
    const navigate = () => {
      if (notification.route) void this.router.navigateByUrl(notification.route);
    };
    if (notification.read) {
      navigate();
      return;
    }
    this.notificationStore.markAsRead(notification).subscribe({ next: navigate });
  }

  private loadWorkflowSummary(): void {
    if (this.workflowLoading()) return;
    this.workflowLoading.set(true);
    this.workflowError.set(null);
    this.dashboardService.getWorkflowSummary().pipe(
      finalize(() => this.workflowLoading.set(false)),
    ).subscribe({
      next: (response) => {
        if (response?.data) this.workflowSummary.set(response.data);
        else this.workflowError.set('Workflow data was not returned.');
      },
      error: () => this.workflowError.set('Approval summary is temporarily unavailable.'),
    });
  }


  private loadCashAccounts(): void {
    if (!this.canViewCashPosition() || this.cashAccountsLoading()) return;
    this.cashAccountsLoading.set(true);
    this.bankAccountService
      .getAll()
      .pipe(finalize(() => this.cashAccountsLoading.set(false)))
      .subscribe({
        next: (response) => this.cashAccounts.set(response?.data ?? []),
        error: () => this.cashAccounts.set([]),
      });
  }

  private loadNotifications(force = false): void {
    this.notificationStore.loadUnreadCount();
    if (force || this.latestUnreadNotifications().length === 0) this.notificationStore.loadDashboardPreview();
  }

  private pendingAgeSuffix(submittedAt: string | null): string {
    if (!submittedAt) return ' to review';
    const submitted = new Date(submittedAt).getTime();
    if (!Number.isFinite(submitted)) return ' to review';
    const hours = Math.max(0, Math.floor((Date.now() - submitted) / 3_600_000));
    if (hours < 1) return '; oldest submitted less than an hour ago';
    if (hours < 24) return `; oldest pending ${hours}h`;
    return `; oldest pending ${Math.floor(hours / 24)}d`;
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

  private syncSummaryValues(dashboard: DashboardSummary): void {
    const targetValues: DashboardDisplayValues = {
      cashPosition: this.toSafeNumber(dashboard.business?.cashPosition),

      accountsReceivable: this.toSafeNumber(dashboard.business?.accountsReceivable),

      accountsPayable: this.toSafeNumber(dashboard.business?.accountsPayable),

      postedThisMonthTotal: this.toSafeNumber(dashboard.expense?.postedThisMonthTotal),
      currentMonthRevenue: this.toSafeNumber(dashboard.business?.currentMonthRevenue),
      currentMonthExpense: this.toSafeNumber(dashboard.business?.currentMonthExpense),
    };
    this.displayValues.set(targetValues);
  }

  private toSafeNumber(value: number | null | undefined): number {
    const numericValue = Number(value);

    return Number.isFinite(numericValue) ? numericValue : 0;
  }

  private clamp(value: number, minimum: number, maximum: number): number {
    return Math.min(Math.max(value, minimum), maximum);
  }
}
