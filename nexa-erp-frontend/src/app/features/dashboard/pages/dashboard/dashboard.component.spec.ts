import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { TokenService } from '../../../../core/services/token.service';
import { DashboardSummary } from '../../models/dashboard.model';
import { DashboardService } from '../../services/dashboard.service';
import { DashboardComponent } from './dashboard.component';

describe('DashboardComponent', () => {
  let fixture: ComponentFixture<DashboardComponent>;
  let component: DashboardComponent;
  const service = { getSummary: vi.fn() };
  const token = { getPermissions: vi.fn(() => ['VIEW_BANKING', 'VIEW_REPORT', 'VIEW_BUDGET_REPORT', 'VIEW_EXPENSE', 'MANAGE_SETTINGS']) };
  const summary: DashboardSummary = {
    users: null, security: null, finance: null, recentActivities: null,
    system: { applicationVersion: '1.0', serverTime: '2026-07-26T10:00:00', serverTimezone: 'Asia/Dhaka', environment: 'test', javaVersion: '21' },
    business: { cashPosition: 0, cashConfigured: true, asOfDate: '2026-07-26', currencyCode: 'BDT',
      accountsReceivable: 0, overdueInvoiceCount: 0, overdueInvoiceAmount: 0, accountsPayable: 0,
      overdueBillCount: 0, overdueBillAmount: 0, trendFromDate: '2026-02-01', trendToDate: '2026-07-26',
      revenueTrend: [], expenseTrend: [] },
    budget: { hasActiveBudget: false, activeBudgetId: null, activeBudgetName: null,
      unavailableReason: 'No active budget', fromDate: null, toDate: null, currencyCode: null,
      totalExpenseBudget: 0, totalExpenseActualYtd: 0, expenseUtilizationPercent: 0,
      totalRevenueBudget: 0, totalRevenueActualYtd: 0, revenueAchievementPercent: 0, topAccounts: [] },
    expense: { draftCount: 0, draftTotalAmount: 0, postedThisMonthTotal: -25,
      recurringActiveCount: 0, recurringDueSoonCount: 0, outstandingDue: 0 },
  };

  beforeEach(async () => {
    service.getSummary.mockReturnValue(of({ success: true, message: '', data: summary }));
    await TestBed.configureTestingModule({ imports: [DashboardComponent], providers: [provideRouter([]),
      { provide: DashboardService, useValue: service }, { provide: TokenService, useValue: token }] }).compileComponents();
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
    expect(component.quickActions().find((action) => action.id === 'view-banking')?.route).toBe('/banking');
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
});
