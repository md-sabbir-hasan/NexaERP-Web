import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import Swal from 'sweetalert2';

import { AlertService } from '../../../../core/services/alert.service';
import { ExpenseResponse } from '../../models/expense.model';
import { ExpenseService } from '../../services/expense.service';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-expense-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, HasPermissionDirective],
  templateUrl: './expense-detail.html',
  styleUrl: './expense-detail.scss',
})
export class ExpenseDetail implements OnInit {
  private expenseId = 0;
  readonly loading = signal(false);
  readonly cancelling = signal(false);
  readonly error = signal<string | null>(null);
  readonly expense = signal<ExpenseResponse | null>(null);

  constructor(
    private route: ActivatedRoute,
    private expenseService: ExpenseService,
    private alert: AlertService,
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.expenseId = id;
      this.loadExpense(id);
    }
  }

  loadExpense(id: number): void {
    if (!id) id = this.expenseId;
    if (!id) return;
    this.loading.set(true);
    this.error.set(null);

    this.expenseService.getById(id).subscribe({
      next: (res) => {
        this.expense.set(res.data);
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.error.set(error?.error?.message ?? 'Unable to load this expense. Please try again.');
      },
    });
  }

  async cancelExpense(): Promise<void> {
    const expense = this.expense();
    if (!expense || expense.status !== 'POSTED' || this.cancelling()) return;

    const result = await Swal.fire<string>({
      title: 'Cancel this expense?',
      text: 'This action will reverse the posted accounting entry. The original expense will remain in the audit history.',
      icon: 'warning',
      input: 'textarea',
      inputLabel: 'Cancellation reason',
      inputPlaceholder: 'Enter the reason for cancellation',
      inputAttributes: { 'aria-label': 'Cancellation reason' },
      showCancelButton: true,
      focusCancel: true,
      reverseButtons: true,
      confirmButtonText: 'Yes, cancel expense',
      cancelButtonText: 'Keep expense',
      inputValidator: (value) => value?.trim() ? undefined : 'Cancellation reason is required',
    });
    if (!result.isConfirmed || !result.value?.trim()) return;

    this.cancelling.set(true);
    void Swal.fire({
      title: 'Cancelling expense…',
      text: 'Please wait while the accounting entry is reversed.',
      allowOutsideClick: false,
      allowEscapeKey: false,
      showConfirmButton: false,
      didOpen: () => Swal.showLoading(),
    });

    this.expenseService.cancel(expense.id, { reason: result.value.trim() }).subscribe({
      next: async () => {
        this.cancelling.set(false);
        await Swal.fire({
          title: 'Expense cancelled',
          text: 'The expense was cancelled and its accounting entry was reversed successfully.',
          icon: 'success',
          confirmButtonText: 'OK',
        });
        this.loadExpense(expense.id);
      },
      error: (err) => {
        this.cancelling.set(false);
        void Swal.fire({
          title: 'Unable to cancel expense',
          text: err?.error?.message ?? 'Unable to cancel the expense. Please try again.',
          icon: 'error',
        });
      },
    });
  }

  costCenterLabel(expense: ExpenseResponse): string {
    return expense.costCenterCode && expense.costCenterName
      ? `${expense.costCenterCode} - ${expense.costCenterName}`
      : 'Not Assigned';
  }

  isDueOutstanding(expense: ExpenseResponse): boolean {
    return Number(expense.dueAmount) > 0;
  }

  async postExpense(): Promise<void> {
    const expense = this.expense();
    if (!expense) return;

    const confirmed = await this.alert.confirm(`Post ${expense.expenseNumber}? This will create the journal entry.`);
    if (!confirmed) return;

    this.expenseService.post(expense.id).subscribe({
      next: (res) => {
        this.expense.set(res.data);
        this.alert.success('Expense posted');

        if (res.data.budgetWarnings && res.data.budgetWarnings.length > 0) {
          for (const w of res.data.budgetWarnings) {
            this.alert.warning(w.message);
          }
        }
      },
      error: (error) => this.alert.error(error?.error?.message ?? 'Failed to post expense'),
    });
  }

}
