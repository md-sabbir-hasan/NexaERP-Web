import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

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
  readonly loading = signal(false);
  readonly expense = signal<ExpenseResponse | null>(null);

  constructor(
    private route: ActivatedRoute,
    private expenseService: ExpenseService,
    private alert: AlertService,
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.loadExpense(id);
    }
  }

  loadExpense(id: number): void {
    this.loading.set(true);

    this.expenseService.getById(id).subscribe({
      next: (res) => {
        this.expense.set(res.data);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.alert.error('Failed to load expense');
      },
    });
  }

  async cancelExpense(): Promise<void> {
    const expense = this.expense();
    if (!expense) return;

    const reason = window.prompt('Cancel reason?', '');
    if (reason === null) return;

    if (!reason.trim()) {
      this.alert.warning('Cancel reason is required');
      return;
    }

    const confirmed = await this.alert.confirm(`Cancel ${expense.expenseNumber}?`);
    if (!confirmed) return;

    this.expenseService.cancel(expense.id, { reason: reason.trim() }).subscribe({
      next: (res) => {
        this.expense.set(res.data);
        this.alert.success('Expense cancelled');
      },
      error: (err) => {
        this.alert.error(err?.error?.message ?? 'Failed to cancel expense');
      },
    });
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