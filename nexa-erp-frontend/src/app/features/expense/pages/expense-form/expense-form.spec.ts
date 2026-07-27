import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { AlertService } from '../../../../core/services/alert.service';
import { AccountService } from '../../../accounts/services/account.service';
import { CostCenterService } from '../../../cost-center/services/cost-center.service';
import { PartyService } from '../../../party/services/party.service';
import { ExpenseService } from '../../services/expense.service';
import { ExpenseForm } from './expense-form';

describe('ExpenseForm', () => {
  let component: ExpenseForm;
  let expenseService: { create: ReturnType<typeof vi.fn>; uploadReceipt: ReturnType<typeof vi.fn>; attachReceipt: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    expenseService = { create: vi.fn(), uploadReceipt: vi.fn(), attachReceipt: vi.fn() };
    await TestBed.configureTestingModule({
      imports: [ExpenseForm],
      providers: [
        provideRouter([]),
        { provide: ExpenseService, useValue: expenseService },
        { provide: AccountService, useValue: { search: vi.fn(() => of({ data: [] })) } },
        { provide: PartyService, useValue: { getByType: vi.fn(() => of({ data: [] })) } },
        { provide: CostCenterService, useValue: { lookup: vi.fn(() => of({ data: [{ id: 2, code: 'ADMIN', name: 'Administration' }] })) } },
        { provide: AlertService, useValue: { error: vi.fn(), success: vi.fn(), warning: vi.fn() } },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(ExpenseForm);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('keeps cost center optional and loads its lookup safely', () => {
    expect(component.form.get('costCenterId')?.hasValidator).toBeDefined();
    expect(component.form.get('costCenterId')?.valid).toBe(true);
    expect(component.costCenters()).toEqual([{ id: 2, code: 'ADMIN', name: 'Administration' }]);
    expect(component.costCentersLoading()).toBe(false);
  });

  it('maps a null cost center to null and a selected cost center to its id', () => {
    const response = { data: { id: 1, budgetWarnings: [] } };
    expenseService.create.mockReturnValue(of(response));
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    component.form.patchValue({ expenseDate: '2026-07-27', expenseAccountId: 10,
      costCenterId: null, paidImmediately: true, paymentAccountId: 20, amount: 50 });
    component.submit();
    expect(expenseService.create.mock.calls[0][0].costCenterId).toBeNull();

    component.form.patchValue({ costCenterId: 2 });
    component.submit();
    expect(expenseService.create.mock.calls[1][0].costCenterId).toBe(2);
  });

  it('handles a failed lookup without changing form validity', () => {
    const service = TestBed.inject(CostCenterService);
    vi.spyOn(service, 'lookup').mockReturnValue(throwError(() => new Error('offline')));
    component.loadCostCenters();
    expect(component.costCentersError()).toBe(true);
    expect(component.form.get('costCenterId')?.valid).toBe(true);
  });
});
