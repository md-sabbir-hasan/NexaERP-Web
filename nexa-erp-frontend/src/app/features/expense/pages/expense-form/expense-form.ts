import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { AlertService } from '../../../../core/services/alert.service';
import { Account } from '../../../accounts/models/account.model';
import { AccountService } from '../../../accounts/services/account.service';
import { Party } from '../../../party/models/party.model';
import { PartyService } from '../../../party/services/party.service';
import { ExpenseRequest } from '../../models/expense.model';
import { ExpenseService } from '../../services/expense.service';

@Component({
  selector: 'app-expense-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './expense-form.html',
  styleUrl: './expense-form.scss',
})
export class ExpenseForm implements OnInit {
  readonly submitting = signal(false);

  readonly expenseAccounts = signal<Account[]>([]);
  readonly paymentAccounts = signal<Account[]>([]);
  readonly parties = signal<Party[]>([]);

  selectedFile: File | null = null;

  readonly form: FormGroup;

  constructor(
    private readonly fb: FormBuilder,
    private readonly expenseService: ExpenseService,
    private readonly accountService: AccountService,
    private readonly partyService: PartyService,
    private readonly router: Router,
    private readonly alert: AlertService,
  ) {
    this.form = this.fb.group({
      expenseDate: ['', [Validators.required]],
      expenseAccountId: [null, [Validators.required]],
      paidImmediately: [true, [Validators.required]],
      paymentAccountId: [null, [Validators.required]],
      partyId: [null],
      amount: [0, [Validators.required, Validators.min(0.01)]],
      referenceNumber: [''],
      notes: [''],
    });
  }

  ngOnInit(): void {
    this.setDefaultDate();
    this.loadExpenseAccounts();
    this.loadPaymentAccounts();
    this.loadParties();
    this.listenToPaidImmediatelyChanges();
  }

  get paidImmediately(): boolean {
    const value = this.form.get('paidImmediately')?.value;
    return value === true || value === 'true';
  }

  get previewExpenseAccountName(): string {
    const id = Number(this.form.get('expenseAccountId')?.value);
    const acc = this.expenseAccounts().find((a) => a.id === id);
    return acc ? acc.name : 'Select category';
  }

  get previewCreditAccountName(): string {
    if (this.paidImmediately) {
      const id = Number(this.form.get('paymentAccountId')?.value);
      const acc = this.paymentAccounts().find((a) => a.id === id);
      return acc ? acc.name : 'Select account';
    }
    return 'Accounts Payable (default)';
  }

  get previewAmount(): number {
    return Number(this.form.get('amount')?.value ?? 0);
  }

  private setDefaultDate(): void {
    const today = new Date().toISOString().substring(0, 10);
    this.form.patchValue({ expenseDate: today });
  }

  private listenToPaidImmediatelyChanges(): void {
    this.form.get('paidImmediately')?.valueChanges.subscribe((value) => {
      const paidNow = value === true || value === 'true';

      const paymentAccountCtrl = this.form.get('paymentAccountId');
      const partyCtrl = this.form.get('partyId');

      if (paidNow) {
        paymentAccountCtrl?.setValidators([Validators.required]);
        partyCtrl?.clearValidators();
      } else {
        paymentAccountCtrl?.clearValidators();
        partyCtrl?.setValidators([Validators.required]);
      }

      paymentAccountCtrl?.updateValueAndValidity();
      partyCtrl?.updateValueAndValidity();
    });
  }

  loadExpenseAccounts(): void {
    this.accountService.search('', 'EXPENSE', true).subscribe({
      next: (res) => this.expenseAccounts.set(res.data),
      error: () => this.alert.error('Failed to load expense categories'),
    });
  }

  loadPaymentAccounts(): void {
    this.accountService.search('', 'ASSET', true).subscribe({
      next: (res) => this.paymentAccounts.set(res.data),
      error: () => this.alert.error('Failed to load payment accounts'),
    });
  }

  loadParties(): void {
    this.partyService.getByType('VENDOR').subscribe({
      next: (res) => this.parties.set(res.data.filter((p) => p.isActive)),
      error: () => this.alert.error('Failed to load parties'),
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files && input.files.length > 0 ? input.files[0] : null;
  }

  submit(): void {
  if (this.form.invalid) {
    this.form.markAllAsTouched();
    this.alert.error('Please complete required fields');
    return;
  }

  const raw = this.form.getRawValue();
  const paidNow = raw.paidImmediately === true || raw.paidImmediately === 'true';

  const request: ExpenseRequest = {
    expenseDate: raw.expenseDate,
    expenseAccountId: Number(raw.expenseAccountId),
    paidImmediately: paidNow,
    paymentAccountId: paidNow ? Number(raw.paymentAccountId) : null,
    partyId: raw.partyId ? Number(raw.partyId) : null,
    amount: Number(raw.amount),
    referenceNumber: raw.referenceNumber?.trim() || undefined,
    notes: raw.notes?.trim() || undefined,
  };

  this.submitting.set(true);

  this.expenseService
    .create(request)
    .pipe(finalize(() => this.submitting.set(false)))
    .subscribe({
      next: (response) => {
        const expense = response.data;

        if (this.selectedFile) {
          this.expenseService.uploadReceipt(this.selectedFile, expense.id).subscribe({
            next: (uploadRes) => {
              this.expenseService.attachReceipt(expense.id, uploadRes.data.fileUrl).subscribe();
            },
            error: () => this.alert.warning('Expense saved, but receipt upload failed'),
          });
        }

        if (expense.budgetWarnings && expense.budgetWarnings.length > 0) {
          for (const w of expense.budgetWarnings) {
            this.alert.warning(w.message);
          }
        }

        this.alert.success('Expense recorded');
        this.router.navigate(['/expense', expense.id]);
      },
      error: (error) => {
        this.alert.error(error?.error?.message ?? 'Failed to save expense');
      },
    });
}
}
