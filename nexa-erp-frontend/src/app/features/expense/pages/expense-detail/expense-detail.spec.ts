import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import Swal from 'sweetalert2';
import { vi } from 'vitest';
import { AlertService } from '../../../../core/services/alert.service';
import { TokenService } from '../../../../core/services/token.service';
import { ExpenseResponse } from '../../models/expense.model';
import { ExpenseService } from '../../services/expense.service';
import { ExpenseDetail } from './expense-detail';

const expense: ExpenseResponse = {
  id: 1, expenseNumber: 'EXP-001', expenseDate: '2026-07-27',
  expenseAccountId: 10, expenseAccountName: 'Travel',
  costCenterId: null, costCenterCode: null, costCenterName: null,
  paidImmediately: true, paymentAccountId: 20, paymentAccountName: 'Cash',
  partyId: null, partyName: null, amount: 100, paidAmount: 100, dueAmount: 0,
  paymentStatus: 'PAID', referenceNumber: null, attachmentUrl: null, notes: null,
  status: 'POSTED', cancelledAt: null, cancelReason: null,
  createdAt: '2026-07-27T10:00:00', budgetWarnings: [],
};

describe('ExpenseDetail', () => {
  let fixture: ComponentFixture<ExpenseDetail>;
  let component: ExpenseDetail;
  let service: { getById: ReturnType<typeof vi.fn>; cancel: ReturnType<typeof vi.fn>; post: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    service = {
      getById: vi.fn(() => of({ data: expense })),
      cancel: vi.fn(() => of({ data: { ...expense, status: 'CANCELLED' } })),
      post: vi.fn(),
    };
    await TestBed.configureTestingModule({
      imports: [ExpenseDetail],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => '1' } } } },
        { provide: ExpenseService, useValue: service },
        { provide: AlertService, useValue: { error: vi.fn(), success: vi.fn(), warning: vi.fn(), confirm: vi.fn() } },
        { provide: TokenService, useValue: { hasPermission: vi.fn(() => true) } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ExpenseDetail);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => vi.restoreAllMocks());

  it('always renders a cost center and notes fallback', () => {
    expect(fixture.nativeElement.textContent).toContain('Not Assigned');
    expect(fixture.nativeElement.textContent).toContain('No notes provided');
  });

  it('renders an assigned cost center label', () => {
    component.expense.set({ ...expense, costCenterId: 2, costCenterCode: 'ADMIN', costCenterName: 'Administration' });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('ADMIN - Administration');
  });

  it('does not cancel when the SweetAlert confirmation is dismissed', async () => {
    vi.spyOn(Swal, 'fire').mockResolvedValue({ isConfirmed: false } as never);
    await component.cancelExpense();
    expect(service.cancel).not.toHaveBeenCalled();
  });

  it('cancels exactly once, shows success, and refreshes details', async () => {
    vi.spyOn(Swal, 'fire')
      .mockResolvedValueOnce({ isConfirmed: true, value: 'Duplicate entry' } as never)
      .mockResolvedValueOnce({} as never)
      .mockResolvedValueOnce({ isConfirmed: true } as never);
    await component.cancelExpense();
    await Promise.resolve();
    expect(service.cancel).toHaveBeenCalledOnce();
    expect(service.cancel).toHaveBeenCalledWith(1, { reason: 'Duplicate entry' });
    expect(service.getById).toHaveBeenCalledTimes(2);
  });

  it('prevents duplicate cancellation while the request is pending', async () => {
    const pending = new Subject<{ data: ExpenseResponse }>();
    service.cancel.mockReturnValue(pending);
    vi.spyOn(Swal, 'fire')
      .mockResolvedValueOnce({ isConfirmed: true, value: 'Correction' } as never)
      .mockResolvedValue({} as never);
    await component.cancelExpense();
    await component.cancelExpense();
    expect(service.cancel).toHaveBeenCalledOnce();
    pending.complete();
  });

  it('shows the backend failure text', async () => {
    service.cancel.mockReturnValue(throwError(() => ({ error: { message: 'Period is closed' } })));
    const fire = vi.spyOn(Swal, 'fire')
      .mockResolvedValueOnce({ isConfirmed: true, value: 'Correction' } as never)
      .mockResolvedValue({} as never);
    await component.cancelExpense();
    expect(service.cancel).toHaveBeenCalledOnce();
    expect(fire.mock.calls.some(([options]) =>
      typeof options === 'object' && (options as { text?: string })?.text === 'Period is closed')).toBe(true);
  });
});
