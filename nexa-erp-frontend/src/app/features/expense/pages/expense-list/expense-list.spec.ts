import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { AlertService } from '../../../../core/services/alert.service';
import { TokenService } from '../../../../core/services/token.service';
import { CostCenterService } from '../../../cost-center/services/cost-center.service';
import { ExpenseResponse } from '../../models/expense.model';
import { ExpenseService } from '../../services/expense.service';
import { ExpenseList } from './expense-list';

const baseExpense: ExpenseResponse = {
  id: 1, expenseNumber: 'EXP-001', expenseDate: '2026-07-27',
  expenseAccountId: 10, expenseAccountName: 'Office Supplies',
  costCenterId: 2, costCenterCode: 'ADMIN', costCenterName: 'Administration',
  paidImmediately: true, paymentAccountId: 20, paymentAccountName: 'Cash',
  partyId: null, partyName: null, amount: 100, paidAmount: 100, dueAmount: 0,
  paymentStatus: 'PAID', referenceNumber: null, attachmentUrl: null, notes: null,
  status: 'POSTED', cancelledAt: null, cancelReason: null,
  createdAt: '2026-07-27T10:00:00', budgetWarnings: [],
};

describe('ExpenseList', () => {
  let fixture: ComponentFixture<ExpenseList>;
  let component: ExpenseList;
  const expenses = [
    baseExpense,
    { ...baseExpense, id: 2, expenseNumber: 'EXP-002', costCenterId: null,
      costCenterCode: null, costCenterName: null, dueAmount: 25, paymentStatus: 'UNPAID' as const },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExpenseList],
      providers: [
        provideRouter([]),
        { provide: ExpenseService, useValue: { getAll: vi.fn(() => of({ data: expenses })), post: vi.fn() } },
        { provide: CostCenterService, useValue: { lookup: vi.fn(() => of({ data: [{ id: 2, code: 'ADMIN', name: 'Administration' }] })) } },
        { provide: AlertService, useValue: { error: vi.fn(), success: vi.fn(), warning: vi.fn(), confirm: vi.fn() } },
        { provide: TokenService, useValue: { hasPermission: vi.fn(() => true) } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ExpenseList);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('renders assigned and unassigned cost centers', () => {
    expect(fixture.nativeElement.textContent).toContain('ADMIN');
    expect(fixture.nativeElement.textContent).toContain('Administration');
    expect(fixture.nativeElement.textContent).toContain('Not Assigned');
  });

  it('filters by cost center and unassigned selection', () => {
    component.costCenter.set('2');
    expect(component.filteredExpenses().map((item) => item.id)).toEqual([1]);
    component.costCenter.set('UNASSIGNED');
    expect(component.filteredExpenses().map((item) => item.id)).toEqual([2]);
  });

  it('searches cost center code and name', () => {
    component.search.set('administration');
    expect(component.filteredExpenses()).toEqual([baseExpense]);
    component.search.set('admin');
    expect(component.filteredExpenses()).toEqual([baseExpense]);
  });

  it('emphasizes only positive due amounts', () => {
    fixture.detectChanges();
    const dueCells = [...fixture.nativeElement.querySelectorAll('.due-text')] as HTMLElement[];
    const zeroDue = dueCells.find((cell) => cell.textContent?.trim() === '0.00');
    const positiveDue = dueCells.find((cell) => cell.textContent?.trim() === '25.00');
    expect(zeroDue?.classList.contains('outstanding')).toBe(false);
    expect(positiveDue?.classList.contains('outstanding')).toBe(true);
  });
});
