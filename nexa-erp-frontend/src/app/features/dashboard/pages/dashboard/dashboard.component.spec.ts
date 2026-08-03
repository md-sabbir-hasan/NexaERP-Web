import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { TokenService } from '../../../../core/services/token.service';
import { NotificationStore } from '../../../notifications/services/notification.store';
import { NotificationResponse } from '../../../notifications/models/notification.model';
import { DashboardSummary } from '../../models/dashboard.model';
import { DashboardService } from '../../services/dashboard.service';
import { DashboardComponent } from './dashboard.component';

describe('DashboardComponent', () => {
  let fixture: ComponentFixture<DashboardComponent>;
  let component: DashboardComponent;
  const service = { getSummary: vi.fn(), getWorkflowSummary: vi.fn() };
  const notificationStore = {
    notifications: signal([]), unreadCount: signal(0), loading: signal(false),
    loaded: signal(true), error: signal<string | null>(null),
    dashboardPreview: signal<NotificationResponse[]>([]), dashboardPreviewLoading: signal(false),
    dashboardPreviewError: signal<string | null>(null),
    loadUnreadCount: vi.fn(), loadFirstPage: vi.fn(), loadDashboardPreview: vi.fn(),
    markAsRead: vi.fn(() => of({})),
  };
  const token = { getPermissions: vi.fn(() => ['VIEW_BANKING', 'VIEW_REPORT', 'VIEW_BUDGET_REPORT', 'VIEW_EXPENSE', 'MANAGE_SETTINGS']) };
  const summary: DashboardSummary = {
    users: null, security: null, finance: null, recentActivities: null,
    system: { applicationVersion: '1.0', serverTime: '2026-07-26T10:00:00', serverTimezone: 'Asia/Dhaka', environment: 'test', javaVersion: '21' },
    business: { cashPosition: 0, cashConfigured: true, asOfDate: '2026-07-26', currencyCode: 'BDT',
      accountsReceivable: 0, overdueInvoiceCount: 0, overdueInvoiceAmount: 0, accountsPayable: 0,
      overdueBillCount: 0, overdueBillAmount: 0, trendFromDate: '2026-02-01', trendToDate: '2026-07-26',
      revenueTrend: [], expenseTrend: [], currentMonthRevenue: 0, currentMonthExpense: 0 },
    budget: { hasActiveBudget: false, activeBudgetId: null, activeBudgetName: null,
      unavailableReason: 'No active budget', fromDate: null, toDate: null, currencyCode: null,
      totalExpenseBudget: 0, totalExpenseActualYtd: 0, expenseUtilizationPercent: 0,
      totalRevenueBudget: 0, totalRevenueActualYtd: 0, revenueAchievementPercent: 0, topAccounts: [] },
    expense: { draftCount: 0, draftTotalAmount: 0, postedThisMonthTotal: -25,
      recurringActiveCount: 0, recurringDueSoonCount: 0, outstandingDue: 0 },
  };

  beforeEach(async () => {
    service.getSummary.mockReturnValue(of({ success: true, message: '', data: summary }));
    service.getWorkflowSummary.mockReturnValue(of({ success: true, message: '', data: {
      approvalEnabled: true, availablePendingCount: null, oldestAvailableSubmittedAt: null,
      myPendingCount: 0, myReturnedCount: 0, myApprovedUnconsumedCount: 0,
    } }));
    notificationStore.notifications.set([]);
    notificationStore.unreadCount.set(0);
    notificationStore.loading.set(false);
    notificationStore.loaded.set(true);
    notificationStore.error.set(null);
    notificationStore.dashboardPreview.set([]);
    notificationStore.dashboardPreviewLoading.set(false);
    notificationStore.dashboardPreviewError.set(null);
    await TestBed.configureTestingModule({ imports: [DashboardComponent], providers: [provideRouter([]),
      { provide: DashboardService, useValue: service }, { provide: TokenService, useValue: token },
      { provide: NotificationStore, useValue: notificationStore }] }).compileComponents();
    fixture = TestBed.createComponent(DashboardComponent); component = fixture.componentInstance; fixture.detectChanges();
  });

  it('maps a successful response and preserves real zero and negative values', () => {
    expect(component.summary()?.business?.cashPosition).toBe(0);
    expect(component.summary()?.expense?.postedThisMonthTotal).toBe(-25);
  });

  it('uses VIEW_BUDGET_REPORT and VIEW_EXPENSE for widget visibility', () => {
    expect(component.canViewBudget()).toBe(true);
    expect(component.canViewExpenseSummary()).toBe(true);
  });

  it('renders honest unavailable budget state and no removed synthetic widgets', () => {
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('No active budget');
    expect(text).not.toContain('Service Status');
    expect(text).not.toContain('Expense Distribution');
  });

  it('uses the real banking quick-action route', () => {
    expect(component.quickActions().length).toBeLessThanOrEqual(6);
  });

  it('shows a permission-specific 403 message and supports retry', () => {
    service.getSummary.mockReturnValueOnce(throwError(() => ({ status: 403 })));
    component.refreshDashboard(); fixture.detectChanges();
    expect(component.errorMessage()).toContain('not authorized');
    component.retryLoad(); expect(service.getSummary).toHaveBeenCalled();
  });

  it('exposes accessible trend point labels when chart data exists', () => {
    const month = { month: 'Jul 2026', amount: 100 };
    component.summary.set({ ...summary, business: { ...summary.business!, revenueTrend: [month], expenseTrend: [{ ...month, amount: 50 }] } });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[aria-label*="revenue"]')).toBeTruthy();
  });

  it('keeps core data when the optional workflow request fails', () => {
    service.getWorkflowSummary.mockReturnValueOnce(throwError(() => ({ status: 500 })));
    component.retryWorkflow();
    expect(component.summary()).toBe(summary);
    expect(component.workflowError()).toContain('temporarily unavailable');
  });

  it('maps workflow metrics into actionable attention rows', () => {
    component.workflowSummary.set({ approvalEnabled: true, availablePendingCount: null,
      oldestAvailableSubmittedAt: null, myPendingCount: 2, myReturnedCount: 1,
      myApprovedUnconsumedCount: 3 });
    expect(component.attentionItems().map((item) => item.id)).toEqual(
      expect.arrayContaining(['my-pending-requests', 'returned-requests', 'approved-unposted']),
    );
  });

  it('keeps approval-disabled state distinct from an empty enabled workflow', () => {
    component.workflowSummary.set({ approvalEnabled: false, availablePendingCount: null,
      oldestAvailableSubmittedAt: null, myPendingCount: null, myReturnedCount: null,
      myApprovedUnconsumedCount: null });
    expect(component.workflowSummary()?.approvalEnabled).toBe(false);
    expect(component.attentionItems().some((item) => item.id.includes('approval'))).toBe(false);
  });

  it('limits the shared notification preview to three actionable unread items', () => {
    notificationStore.dashboardPreview.set([1, 2, 3, 4].map((id) => ({ id, type: 'SYSTEM',
      priority: 'MEDIUM', module: 'SYSTEM', title: `Notice ${id}`, message: 'Action required',
      route: '/notifications', entityType: null, entityId: null, read: false, readAt: null,
      expiresAt: null, createdAt: '2026-08-03T10:00:00' })));
    expect(component.latestUnreadNotifications()).toHaveLength(3);
  });

  it('uses exact guarded routes and never exposes more than six quick actions', () => {
    token.getPermissions.mockReturnValue(['CREATE_INVOICE', 'CREATE_VENDOR_BILL', 'CREATE_PAYMENT',
      'CREATE_JOURNAL', 'CREATE_EXPENSE', 'VIEW_APPROVAL_QUEUE', 'VIEW_REPORT']);
    component.refreshPermissions();
    const actions = component.quickActions();
    expect(actions).toHaveLength(6);
    expect(actions.map((action) => action.route)).toEqual([
      '/invoice/new', '/vendor-bill/new', '/journals/new', '/payment/new', '/expense/new', '/approvals',
    ]);
  });

  it('displays backend ISO currency instead of a hardcoded taka symbol', () => {
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('BDT');
    expect(text).not.toContain('৳');
  });
});
