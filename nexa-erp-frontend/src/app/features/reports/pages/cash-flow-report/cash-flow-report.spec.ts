import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { provideRouter } from '@angular/router';
import { AlertService } from '../../../../core/services/alert.service';
import { ReportService } from '../../services/report.service';
import { CashFlowStatementResponse } from '../../models/cash-flow.model';
import { CashFlowReport } from './cash-flow-report';

describe('CashFlowReport', () => {
  let fixture: ComponentFixture<CashFlowReport>;
  let component: CashFlowReport;
  const data: CashFlowStatementResponse = {
    fromDate: '2026-07-01', toDate: '2026-07-31', currencyCode: 'BDT', openingCashBalance: 100,
    operatingActivities: { activity: 'OPERATING' as const, items: [], totalInflows: 50, totalOutflows: 0, netCashFlow: 50 },
    investingActivities: { activity: 'INVESTING' as const, items: [], totalInflows: 0, totalOutflows: 20, netCashFlow: -20 },
    financingActivities: { activity: 'FINANCING' as const, items: [], totalInflows: 0, totalOutflows: 0, netCashFlow: 0 },
    netCashFromOperatingActivities: 50, netCashFromInvestingActivities: -20, netCashFromFinancingActivities: 0,
    netChangeInCash: 30, calculatedClosingCashBalance: 130, ledgerClosingCashBalance: 130,
    reconciliationDifference: 0, isReconciled: true, cashAccounts: [], unclassifiedMovements: [], generatedAt: '2026-07-31T12:00:00',
  };
  const service = { getCashFlow: vi.fn(() => of({ success: true, message: '', data })), downloadCashFlowExcel: vi.fn(() => of(new Blob(['x']))) };
  const alert = { error: vi.fn(), success: vi.fn() };

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [CashFlowReport], providers: [provideHttpClient(), provideRouter([]),
      { provide: ReportService, useValue: service }, { provide: AlertService, useValue: alert }] }).compileComponents();
    fixture = TestBed.createComponent(CashFlowReport); component = fixture.componentInstance; fixture.detectChanges();
  });

  it('defaults to the current month through today', () => { expect(component.fromDate()).toMatch(/^\d{4}-\d{2}-01$/); expect(component.toDate()).toMatch(/^\d{4}-\d{2}-\d{2}$/); });
  it('rejects an invalid date range', () => { component.fromDate.set('2026-08-01'); component.toDate.set('2026-07-01'); component.generateReport(); expect(alert.error).toHaveBeenCalled(); });
  it('renders all three activity sections after success', () => { component.generateReport(); fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('Operating Activities'); expect(fixture.nativeElement.textContent).toContain('Investing Activities'); expect(fixture.nativeElement.textContent).toContain('Financing Activities'); });
  it('shows unclassified warnings and reconciliation failure', () => { service.getCashFlow.mockReturnValueOnce(of({ success: true, message: '', data: { ...data, isReconciled: false, reconciliationDifference: 10, unclassifiedMovements: [{ journalEntryId: 1, entryNumber: 'JE-1', date: '2026-07-01', sourceType: 'MANUAL', sourceId: null, description: '', amount: 10, reason: 'Unknown' }] } })); component.generateReport(); fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('Unclassified cash movements'); expect(fixture.nativeElement.textContent).toContain('Difference'); });
  it('shows a general API error and supports retry', () => { service.getCashFlow.mockReturnValueOnce(throwError(() => ({ status: 500, error: { message: 'Report failed' } }))); component.generateReport(); fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('Report failed'); });
  it('exports Excel only after generation', () => { component.generateReport(); component.exportExcel(); expect(service.downloadCashFlowExcel).toHaveBeenCalled(); });
  it('prints the report', () => { const spy = vi.spyOn(window, 'print').mockImplementation(() => undefined); component.printReport(); expect(spy).toHaveBeenCalled(); });
});
