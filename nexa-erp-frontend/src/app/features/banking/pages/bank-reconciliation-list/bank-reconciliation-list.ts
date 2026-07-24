import { CommonModule, DecimalPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AlertService } from '../../../../core/services/alert.service';
import { BankAccount } from '../../models/bank-account.model';
import { BankReconciliation, BankReconciliationStartRequest } from '../../models/bank-reconciliation.model';
import { BankAccountService } from '../../services/bank-account.service';
import { BankReconciliationService } from '../../services/bank-reconciliation.service';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-bank-reconciliation-list',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, DecimalPipe, RouterLink, HasPermissionDirective],
  templateUrl: './bank-reconciliation-list.html',
  styleUrl: './bank-reconciliation-list.scss',
})
export class BankReconciliationList implements OnInit {
  readonly bankAccounts = signal<BankAccount[]>([]);
  readonly reconciliations = signal<BankReconciliation[]>([]);

  readonly loading = signal(false);
  readonly submitting = signal(false);
  readonly showModal = signal(false);

  selectedAccountId: number | '' = '';

  readonly startForm;

  constructor(
    private bankAccountService: BankAccountService,
    private reconciliationService: BankReconciliationService,
    private alert: AlertService,
    private router: Router,
    private fb: NonNullableFormBuilder,
  ) {
    this.startForm = this.fb.group({
      statementDate: [this.today(), [Validators.required]],
      statementBalance: [0, [Validators.required]],
      notes: [''],
    });
  }

  ngOnInit(): void {
    this.loadBankAccounts();
  }

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }

  loadBankAccounts(): void {
    this.bankAccountService.getAll().subscribe({
      next: (res) => {
        const active = res.data.filter((a) => a.isActive);
        this.bankAccounts.set(active);
        if (active.length && this.selectedAccountId === '') {
          this.selectedAccountId = active[0].id;
          this.loadReconciliations();
        }
      },
      error: () => this.alert.error('Failed to load bank accounts'),
    });
  }

  onAccountChange(): void {
    this.loadReconciliations();
  }

  loadReconciliations(): void {
    if (this.selectedAccountId === '') return;
    this.loading.set(true);

    this.reconciliationService.getByAccount(this.selectedAccountId).subscribe({
      next: (res) => {
        this.reconciliations.set(res.data);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.alert.error('Failed to load reconciliation history');
      },
    });
  }

  get selectedAccount(): BankAccount | undefined {
    return this.bankAccounts().find((a) => a.id === this.selectedAccountId);
  }

  get hasInProgress(): boolean {
    return this.reconciliations().some((r) => r.status === 'IN_PROGRESS');
  }

  openStartModal(): void {
    if (this.hasInProgress) {
      this.alert.warning(
        'This account already has an in-progress reconciliation. Open it to continue, or complete it first.',
      );
      return;
    }

    this.startForm.reset({
      statementDate: this.today(),
      statementBalance: this.selectedAccount?.currentBalance ?? 0,
      notes: '',
    });
    this.showModal.set(true);
  }

  closeModal(): void {
    this.showModal.set(false);
  }

  submitStart(): void {
    if (this.startForm.invalid || this.selectedAccountId === '') {
      this.startForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    const raw = this.startForm.getRawValue();

    const request: BankReconciliationStartRequest = {
      bankAccountId: this.selectedAccountId,
      statementDate: raw.statementDate,
      statementBalance: raw.statementBalance,
      notes: raw.notes || null,
    };

    this.reconciliationService.start(request).subscribe({
      next: (res) => {
        this.submitting.set(false);
        this.showModal.set(false);
        this.alert.success('Reconciliation started');
        this.router.navigate(['/banking/reconciliation', res.data.id]);
      },
      error: (error) => {
        this.submitting.set(false);
        this.alert.error(error?.error?.message ?? 'Failed to start reconciliation');
      },
    });
  }

  openReconciliation(r: BankReconciliation): void {
    this.router.navigate(['/banking/reconciliation', r.id]);
  }
}
