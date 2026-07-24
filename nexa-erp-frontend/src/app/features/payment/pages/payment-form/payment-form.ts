import { CommonModule, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import {
  FormArray,
  FormGroup,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';

import { AlertService } from '../../../../core/services/alert.service';
import { Account } from '../../../accounts/models/account.model';
import { AccountService } from '../../../accounts/services/account.service';
import { Invoice } from '../../../invoice/models/invoice.model';
import { InvoiceService } from '../../../invoice/services/invoice.service';
import { Party } from '../../../party/models/party.model';
import { PartyService } from '../../../party/services/party.service';
import { VendorBill } from '../../../vendor-bill/models/vendor-bill.model';
import { VendorBillService } from '../../../vendor-bill/services/vendor-bill.service';

import {
  PartyOutstandingSummary,
  PaymentAllocationRequest,
  PaymentMethod,
  PaymentReferenceType,
  PaymentRequest,
  PaymentType,
} from '../../models/payment.model';
import { PaymentService } from '../../services/payment.service';

@Component({
  selector: 'app-payment-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, DecimalPipe],
  templateUrl: './payment-form.html',
  styleUrl: './payment-form.scss',
})
export class PaymentForm implements OnInit {
  private readonly destroyRef = inject(DestroyRef);

  readonly submitting = signal(false);
  readonly summaryLoading = signal(false);

  readonly parties = signal<Party[]>([]);
  readonly accounts = signal<Account[]>([]);
  readonly dueInvoices = signal<Invoice[]>([]);
  readonly dueBills = signal<VendorBill[]>([]);

  readonly outstandingSummary = signal<PartyOutstandingSummary | null>(null);

  readonly paymentTypes: PaymentType[] = ['RECEIPT', 'PAYMENT'];

  readonly paymentMethods: PaymentMethod[] = [
    'CASH',
    'BANK_TRANSFER',
    'CHEQUE',
    'BKASH',
    'NAGAD',
    'ROCKET',
    'CARD',
  ];

  readonly form: FormGroup;

  constructor(
    private readonly fb: NonNullableFormBuilder,
    private readonly paymentService: PaymentService,
    private readonly partyService: PartyService,
    private readonly accountService: AccountService,
    private readonly invoiceService: InvoiceService,
    private readonly vendorBillService: VendorBillService,
    private readonly router: Router,
    private readonly alert: AlertService,
  ) {
    this.form = this.fb.group({
      paymentType: ['RECEIPT' as PaymentType, [Validators.required]],

      partyId: [null as number | null, [Validators.required]],

      accountId: [null as number | null, [Validators.required]],

      paymentDate: ['', [Validators.required]],

      amount: [0, [Validators.required, Validators.min(0.01)]],

      currencyCode: ['BDT', [Validators.required]],

      paymentMethod: ['BANK_TRANSFER' as PaymentMethod, [Validators.required]],

      transactionRef: [''],
      notes: [''],
      autoAllocate: [true],

      allocations: this.fb.array([]),
    });
  }

  ngOnInit(): void {
    this.setDefaultPaymentDate();
    this.loadParties();
    this.loadAccounts();
    this.listenToPaymentTypeChanges();
    this.listenToAutoAllocateChanges();
  }

  get allocations(): FormArray {
    return this.form.get('allocations') as FormArray;
  }

  /**
   * Reactive Form values are not Signals, so these are getters
   * instead of computed() values.
   */
  get allocationTotal(): number {
    return this.allocations.controls.reduce(
      (sum, control) => sum + Number(control.get('allocatedAmount')?.value ?? 0),
      0,
    );
  }

  get unallocatedAmount(): number {
    const paymentAmount = Number(this.form.get('amount')?.value ?? 0);

    return Math.max(paymentAmount - this.allocationTotal, 0);
  }

  get selectedPaymentType(): PaymentType {
    return this.form.get('paymentType')?.value as PaymentType;
  }

  get selectedPartyId(): number | null {
    const value = this.form.get('partyId')?.value;

    if (value === null || value === undefined || value === '') {
      return null;
    }

    const id = Number(value);

    return Number.isFinite(id) && id > 0 ? id : null;
  }

  get paymentExceedsOutstanding(): boolean {
    const amount = Number(this.form.get('amount')?.value ?? 0);

    const dueAmount = Number(this.outstandingSummary()?.dueAmount ?? 0);

    return dueAmount > 0 && amount > dueAmount;
  }

  private setDefaultPaymentDate(): void {
    const today = new Date().toISOString().substring(0, 10);

    this.form.patchValue({
      paymentDate: today,
    });
  }

  private listenToPaymentTypeChanges(): void {
    this.form
      .get('paymentType')
      ?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.form.patchValue(
          {
            partyId: null,
          },
          {
            emitEvent: false,
          },
        );

        this.clearPartyRelatedData();
        this.loadParties();
      });
  }

  private listenToAutoAllocateChanges(): void {
    this.form
      .get('autoAllocate')
      ?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((autoAllocate) => {
        this.allocations.clear();

        if (autoAllocate) {
          this.dueInvoices.set([]);
          this.dueBills.set([]);
          return;
        }

        this.loadDueDocuments();
      });
  }

  private clearPartyRelatedData(): void {
    this.outstandingSummary.set(null);
    this.summaryLoading.set(false);
    this.dueInvoices.set([]);
    this.dueBills.set([]);
    this.allocations.clear();
  }

  createAllocation(
    referenceType: PaymentReferenceType,
    referenceId: number,
    allocatedAmount = 0,
  ): FormGroup {
    return this.fb.group({
      referenceType: [referenceType, [Validators.required]],

      referenceId: [referenceId, [Validators.required]],

      allocatedAmount: [allocatedAmount, [Validators.min(0)]],
    });
  }

  loadParties(): void {
    const partyType = this.selectedPaymentType === 'RECEIPT' ? 'CUSTOMER' : 'VENDOR';

    this.partyService
      .getByType(partyType)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          const activeParties = response.data.filter((party) => party.isActive);

          this.parties.set(activeParties);
        },

        error: (error) => {
          this.parties.set([]);

          this.alert.error(error?.error?.message ?? 'Failed to load parties');
        },
      });
  }

  loadAccounts(): void {
    this.accountService
      .search('', 'ASSET', true)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          /*
           * Ideally the backend should return only posting-level
           * cash and bank accounts. For now, active ASSET accounts
           * are loaded according to the existing API.
           */
          this.accounts.set(response.data);
        },

        error: (error) => {
          this.accounts.set([]);

          this.alert.error(error?.error?.message ?? 'Failed to load payment accounts');
        },
      });
  }

  onPartyChange(): void {
    const partyId = this.selectedPartyId;

    this.outstandingSummary.set(null);
    this.dueInvoices.set([]);
    this.dueBills.set([]);
    this.allocations.clear();

    if (!partyId) {
      return;
    }

    const selectedParty = this.parties().find((party) => party.id === partyId);

    if (selectedParty) {
      this.form.patchValue({
        currencyCode: selectedParty.currency ?? 'BDT',
      });
    }

    this.loadOutstandingSummary(partyId, this.selectedPaymentType);

    if (!this.form.get('autoAllocate')?.value) {
      this.loadDueDocuments();
    }
  }

  loadOutstandingSummary(partyId: number, paymentType: PaymentType): void {
    this.summaryLoading.set(true);

    this.paymentService
      .getOutstandingSummary(partyId, paymentType)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.summaryLoading.set(false)),
      )
      .subscribe({
        next: (response) => {
          this.outstandingSummary.set(response);
        },

        error: (error) => {
          this.outstandingSummary.set(null);

          this.alert.error(error?.error?.message ?? 'Failed to load outstanding summary');
        },
      });
  }

  loadDueDocuments(): void {
    const partyId = this.selectedPartyId;
    const paymentType = this.selectedPaymentType;

    this.allocations.clear();
    this.dueInvoices.set([]);
    this.dueBills.set([]);

    if (!partyId) {
      return;
    }

    if (paymentType === 'RECEIPT') {
      this.loadDueInvoices(partyId);
      return;
    }

    this.loadDueVendorBills(partyId);
  }

  private loadDueInvoices(partyId: number): void {
    this.invoiceService
      .getByParty(partyId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          const invoices = response.data.filter(
            (invoice) =>
              (invoice.status === 'POSTED' || invoice.status === 'PARTIAL') &&
              Number(invoice.dueAmount ?? 0) > 0,
          );

          this.dueInvoices.set(invoices);

          invoices.forEach((invoice) => {
            this.allocations.push(this.createAllocation('INVOICE', invoice.id, 0));
          });
        },

        error: (error) => {
          this.dueInvoices.set([]);

          this.alert.error(error?.error?.message ?? 'Failed to load due invoices');
        },
      });
  }

  private loadDueVendorBills(partyId: number): void {
    this.vendorBillService
      .getByParty(partyId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          const bills = response.data.filter(
            (bill) =>
              (bill.status === 'POSTED' || bill.status === 'PARTIAL') &&
              Number(bill.dueAmount ?? 0) > 0,
          );

          this.dueBills.set(bills);

          bills.forEach((bill) => {
            this.allocations.push(this.createAllocation('VENDOR_BILL', bill.id, 0));
          });
        },

        error: (error) => {
          this.dueBills.set([]);

          this.alert.error(error?.error?.message ?? 'Failed to load due vendor bills');
        },
      });
  }

  remainingDue(dueAmount: number, allocatedAmount: number): number {
    return Math.max(Number(dueAmount) - Number(allocatedAmount), 0);
  }

  isAllocationInvalid(index: number): boolean {
    const group = this.allocations.at(index) as FormGroup;

    const referenceId = Number(group.get('referenceId')?.value);

    const allocatedAmount = Number(group.get('allocatedAmount')?.value ?? 0);

    const dueAmount =
      this.selectedPaymentType === 'RECEIPT'
        ? Number(this.getInvoiceById(referenceId)?.dueAmount ?? 0)
        : Number(this.getBillById(referenceId)?.dueAmount ?? 0);

    return allocatedAmount > dueAmount;
  }

  hasInvalidAllocation(): boolean {
    return this.allocations.controls.some((_, index) => this.isAllocationInvalid(index));
  }

  getInvoiceById(id: number): Invoice | undefined {
    return this.dueInvoices().find((invoice) => invoice.id === id);
  }

  getBillById(id: number): VendorBill | undefined {
    return this.dueBills().find((bill) => bill.id === id);
  }

  getPaymentTitle(): string {
    return this.selectedPaymentType === 'RECEIPT' ? 'Customer Receipt' : 'Vendor Payment';
  }

  getPartyLabel(): string {
    return this.selectedPaymentType === 'RECEIPT' ? 'Customer' : 'Vendor';
  }

  getOutstandingTitle(): string {
    return this.selectedPaymentType === 'RECEIPT' ? 'Customer Outstanding' : 'Vendor Outstanding';
  }

  getDocumentLabel(): string {
    return this.selectedPaymentType === 'RECEIPT' ? 'Open Invoices' : 'Open Bills';
  }

  getMethodLabel(method: string): string {
    switch (method) {
      case 'BANK_TRANSFER':
        return 'Bank Transfer';

      case 'BKASH':
        return 'bKash';

      case 'NAGAD':
        return 'Nagad';

      default:
        return method.replaceAll('_', ' ');
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();

      this.alert.error('Please complete required fields');

      return;
    }

    const raw = this.form.getRawValue();

    const allocations: PaymentAllocationRequest[] = raw.autoAllocate
      ? []
      : raw.allocations
          .filter((allocation: PaymentAllocationRequest) => Number(allocation.allocatedAmount) > 0)
          .map((allocation: PaymentAllocationRequest) => ({
            referenceType: allocation.referenceType,

            referenceId: Number(allocation.referenceId),

            allocatedAmount: Number(allocation.allocatedAmount),
          }));

    const totalAllocated = allocations.reduce(
      (sum, allocation) => sum + Number(allocation.allocatedAmount),
      0,
    );

    if (!raw.autoAllocate && this.hasInvalidAllocation()) {
      this.alert.error('Allocation amount cannot exceed document due amount');

      return;
    }

    if (!raw.autoAllocate && totalAllocated > Number(raw.amount)) {
      this.alert.error('Allocated amount cannot exceed payment amount');

      return;
    }

    /*
     * We do not reject amount > outstanding due because the
     * backend supports unallocated amounts as customer/vendor
     * advances.
     */

    const request: PaymentRequest = {
      partyId: Number(raw.partyId),
      accountId: Number(raw.accountId),
      paymentDate: raw.paymentDate,
      paymentType: raw.paymentType,
      amount: Number(raw.amount),
      currencyCode: raw.currencyCode || 'BDT',
      paymentMethod: raw.paymentMethod,
      transactionRef: raw.transactionRef?.trim() ?? '',
      notes: raw.notes?.trim() ?? '',
      autoAllocate: Boolean(raw.autoAllocate),
      allocations,
    };

    this.submitting.set(true);

    this.paymentService
      .create(request)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.submitting.set(false)),
      )
      .subscribe({
        next: (response) => {
          this.alert.success('Payment saved as draft');

          this.router.navigate(['/payment', response.data.id]);
        },

        error: (error) => {
          this.alert.error(error?.error?.message ?? 'Failed to save payment');
        },
      });
  }
}
