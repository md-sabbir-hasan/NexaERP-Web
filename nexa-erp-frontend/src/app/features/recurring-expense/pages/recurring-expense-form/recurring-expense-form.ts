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
import { RecurringExpenseTemplateRequest } from '../../models/recurring-expense.model';
import { RecurringExpenseService } from '../../services/recurring-expense.service';

@Component({
  selector: 'app-recurring-expense-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './recurring-expense-form.html',
  styleUrl: './recurring-expense-form.scss',
})
export class RecurringExpenseForm implements OnInit {
  readonly submitting = signal(false);

  readonly expenseAccounts = signal<Account[]>([]);
  readonly paymentAccounts = signal<Account[]>([]);
  readonly parties = signal<Party[]>([]);

  readonly frequencies = ['WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY'];

  readonly form: FormGroup;

  constructor(
    private readonly fb: FormBuilder,
    private readonly recurringExpenseService: RecurringExpenseService,
    private readonly accountService: AccountService,
    private readonly partyService: PartyService,
    private readonly router: Router,
    private readonly alert: AlertService,
  ) {
    this.form = this.fb.group({
      name: ['', [Validators.required]],
      expenseAccountId: [null, [Validators.required]],
      amount: [0, [Validators.required, Validators.min(0.01)]],
      frequency: ['MONTHLY', [Validators.required]],
      startDate: ['', [Validators.required]],
      endDate: [''],
      paidImmediately: [true, [Validators.required]],
      paymentAccountId: [null, [Validators.required]],
      partyId: [null],
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

  private setDefaultDate(): void {
    const today = new Date().toISOString().substring(0, 10);
    this.form.patchValue({ startDate: today });
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

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.alert.error('Please complete required fields');
      return;
    }

    const raw = this.form.getRawValue();
    const paidNow = raw.paidImmediately === true || raw.paidImmediately === 'true';

    const request: RecurringExpenseTemplateRequest = {
      name: raw.name.trim(),
      expenseAccountId: Number(raw.expenseAccountId),
      amount: Number(raw.amount),
      paidImmediately: paidNow,
      paymentAccountId: paidNow ? Number(raw.paymentAccountId) : null,
      partyId: raw.partyId ? Number(raw.partyId) : null,
      frequency: raw.frequency,
      startDate: raw.startDate,
      endDate: raw.endDate || null,
      referenceNumber: raw.referenceNumber?.trim() || undefined,
      notes: raw.notes?.trim() || undefined,
    };

    this.submitting.set(true);

    this.recurringExpenseService
      .create(request)
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (response) => {
          this.alert.success('Recurring expense template created');
          this.router.navigate(['/recurring-expense', response.data.id]);
        },
        error: (error) => {
          this.alert.error(error?.error?.message ?? 'Failed to create template');
        },
      });
  }
}