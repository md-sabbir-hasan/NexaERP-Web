import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { provideRouter } from '@angular/router';
import { BudgetService } from '../../services/budget.service';
import { ReportService } from '../../../reports/services/report.service';
import { BudgetVsActualOption, BudgetVsActualResponse } from '../../models/budget.model';
import { BudgetVariance } from './budget-variance';

describe('BudgetVariance', () => {
  let fixture: ComponentFixture<BudgetVariance>;
  let component: BudgetVariance;
  const option: BudgetVsActualOption = {
    budgetId: 1, budgetNumber: 'BUD-0001', budgetName: 'Annual Budget', budgetStatus: 'ACTIVE',
    fiscalYearId: 10, fiscalYearName: 'FY 2026', periods: [
      { id: 21, name: 'January 2026', periodNumber: 1, startDate: '2026-01-01', endDate: '2026-01-31' },
      { id: 22, name: 'February 2026', periodNumber: 2, startDate: '2026-02-01', endDate: '2026-02-28' },
    ],
  };
  const secondOption: BudgetVsActualOption = {
    ...option,
    budgetId: 2,
    budgetNumber: 'BUD-0002',
    budgetName: 'Second Budget',
    periods: [
      { id: 31, name: 'March 2026', periodNumber: 3, startDate: '2026-03-01', endDate: '2026-03-31' },
      { id: 32, name: 'April 2026', periodNumber: 4, startDate: '2026-04-01', endDate: '2026-04-30' },
    ],
  };
  const report: BudgetVsActualResponse = {
    budgetId: 1, budgetNumber: 'BUD-0001', budgetName: 'Annual Budget', budgetStatus: 'ACTIVE',
    fiscalYearId: 10, fiscalYearName: 'FY 2026', currencyCode: 'BDT', fromPeriodId: 21,
    toPeriodId: 22, selectedPeriodIds: [21, 22], fromDate: '2026-01-01', toDate: '2026-02-28',
    totalRevenueBudget: 100, totalRevenueActual: 120, totalRevenueVariance: 20,
    revenueAchievementPercent: 120, totalExpenseBudget: 100, totalExpenseActual: 80,
    totalExpenseVariance: 20, expenseUtilizationPercent: 80,
    revenueLines: [{ budgetLineId: 11, accountId: 101, accountCode: '4000', accountName: 'Sales',
      accountType: 'REVENUE', budgetAmount: 100, actualAmount: 120, varianceAmount: 20,
      variancePercent: 20, utilizationPercent: 120, remainingAmount: -20, varianceStatus: 'FAVORABLE' }],
    expenseLines: [{ budgetLineId: 12, accountId: 201, accountCode: '5000', accountName: 'Rent',
      accountType: 'EXPENSE', budgetAmount: 100, actualAmount: 80, varianceAmount: 20,
      variancePercent: null, utilizationPercent: null, remainingAmount: 20, varianceStatus: 'ON_TARGET' }],
    generatedAt: '2026-03-01T12:00:00',
  };
  const reportService = {
    getBudgetVsActualOptions: vi.fn(() => of({ success: true, message: '', data: [option, secondOption] })),
    getBudgetVsActual: vi.fn(() => of({ success: true, message: '', data: report })),
    downloadBudgetVsActualExcel: vi.fn(() => of(new Blob(['xlsx']))),
  };
  const budgetService = { getVariance: vi.fn() };
  const routeState: { budgetId: string | null } = { budgetId: '1' };

  beforeEach(async () => {
    vi.clearAllMocks();
    routeState.budgetId = '1';
    await TestBed.configureTestingModule({
      imports: [BudgetVariance],
      providers: [provideRouter([]), { provide: ReportService, useValue: reportService },
        { provide: BudgetService, useValue: budgetService }, { provide: ActivatedRoute, useValue: {
          snapshot: { paramMap: { get: () => null }, queryParamMap: {
            get: (key: string) => key === 'budgetId' ? routeState.budgetId : null,
          } },
        } }],
    }).compileComponents();
    fixture = TestBed.createComponent(BudgetVariance);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('selects the first active budget and enables its full period range without a query parameter', () => {
    fixture.destroy();
    routeState.budgetId = null;
    reportService.getBudgetVsActual.mockClear();
    fixture = TestBed.createComponent(BudgetVariance);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const selects = fixture.nativeElement.querySelectorAll('select') as NodeListOf<HTMLSelectElement>;
    expect(component.selectedBudgetId).toBe(1);
    expect(component.fromPeriodId).toBe(21);
    expect(component.toPeriodId).toBe(22);
    expect(selects[0].value).not.toBe('');
    expect(selects[1].disabled).toBe(false);
    expect(selects[2].disabled).toBe(false);
    expect(reportService.getBudgetVsActual).toHaveBeenCalledWith(1, 21, 22, undefined);
  });

  it('preselects the query-parameter budget and loads its full period range', () => {
    expect(component.selectedBudgetId).toBe(1);
    expect(component.fromPeriodId).toBe(21);
    expect(component.toPeriodId).toBe(22);
    expect(reportService.getBudgetVsActual).toHaveBeenCalled();
  });

  it('selects a valid numeric query-parameter budget and loads its periods', () => {
    fixture.destroy();
    routeState.budgetId = '2';
    reportService.getBudgetVsActual.mockClear();
    fixture = TestBed.createComponent(BudgetVariance);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.selectedBudgetId).toBe(2);
    expect(component.fromPeriodId).toBe(31);
    expect(component.toPeriodId).toBe(32);
    expect(reportService.getBudgetVsActual).toHaveBeenCalledWith(2, 31, 32, undefined);
  });

  it('falls back to the first active budget for an invalid query parameter', () => {
    fixture.destroy();
    routeState.budgetId = '999';
    fixture = TestBed.createComponent(BudgetVariance);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.selectedBudgetId).toBe(1);
    expect(component.fromPeriodId).toBe(21);
    expect(component.toPeriodId).toBe(22);
  });

  it('reset restores the first budget and an enabled full period range consistently', () => {
    component.selectedBudgetId = 2;
    component.fromPeriodId = 31;
    component.toPeriodId = 32;
    component.accountType = 'EXPENSE';
    component.reset();
    fixture.detectChanges();

    const selects = fixture.nativeElement.querySelectorAll('select') as NodeListOf<HTMLSelectElement>;
    expect(component.selectedBudgetId).toBe(1);
    expect(component.fromPeriodId).toBe(21);
    expect(component.toPeriodId).toBe(22);
    expect(component.accountType).toBe('');
    expect(component.report()).toBeNull();
    expect(selects[1].disabled).toBe(false);
    expect(selects[2].disabled).toBe(false);
  });

  it('rejects reversed period ranges before requesting the report', () => {
    reportService.getBudgetVsActual.mockClear();
    component.fromPeriodId = 22; component.toPeriodId = 21; component.generate();
    expect(component.errorMessage()).toContain('chronological');
    expect(reportService.getBudgetVsActual).not.toHaveBeenCalled();
  });

  it('passes the account-type filter and renders Revenue and Expense sections', () => {
    component.accountType = 'REVENUE'; component.generate(); fixture.detectChanges();
    expect(reportService.getBudgetVsActual).toHaveBeenLastCalledWith(1, 21, 22, 'REVENUE');
    expect(fixture.nativeElement.textContent).toContain('Revenue');
    expect(fixture.nativeElement.textContent).toContain('Expense');
  });

  it('displays null percentages as an em dash and status text', () => {
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('—');
    expect(fixture.nativeElement.textContent).toContain('FAVORABLE');
    expect(fixture.nativeElement.textContent).toContain('ON_TARGET');
  });

  it('shows the no-activity state including fully reversed guidance', () => {
    reportService.getBudgetVsActual.mockReturnValueOnce(of({ success: true, message: '', data: {
      ...report, totalRevenueActual: 0, totalExpenseActual: 0,
    } }));
    component.generate(); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('fully reversed');
  });

  it('shows API errors and retries', () => {
    reportService.getBudgetVsActual.mockReturnValueOnce(throwError(() => ({ status: 500,
      error: { message: 'Report failed' } })));
    component.generate(); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Report failed');
    reportService.getBudgetVsActual.mockReturnValueOnce(of({ success: true, message: '', data: report }));
    component.retry(); expect(reportService.getBudgetVsActual).toHaveBeenCalled();
  });

  it('downloads Excel with the selected filters', () => {
    component.exportExcel();
    expect(reportService.downloadBudgetVsActualExcel).toHaveBeenCalledWith(1, 21, 22, undefined);
  });

  it('prints using the browser', () => {
    const spy = vi.spyOn(window, 'print').mockImplementation(() => undefined);
    component.printReport(); expect(spy).toHaveBeenCalled();
  });
});
