import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AlertService } from '../../../../core/services/alert.service';
import { ExpensePaymentStatus, ExpenseResponse, ExpenseStatus } from '../../models/expense.model';
import { ExpenseService } from '../../services/expense.service';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-expense-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, HasPermissionDirective],
  templateUrl: './expense-list.html',
  styleUrl: './expense-list.scss',
})
export class ExpenseList implements OnInit {
  readonly loading = signal(false);
  readonly expenses = signal<ExpenseResponse[]>([]);

  readonly search = signal('');
  readonly paymentStatus = signal('');
  readonly status = signal('');

  readonly paymentStatuses: ExpensePaymentStatus[] = ['UNPAID', 'PARTIAL', 'PAID'];
  readonly statuses: ExpenseStatus[] = ['DRAFT', 'POSTED', 'CANCELLED'];

  readonly filteredExpenses = computed(() => {
    let list = [...this.expenses()];

    if (this.search()) {
      const keyword = this.search().toLowerCase();
      list = list.filter(
        (e) =>
          e.expenseNumber.toLowerCase().includes(keyword) ||
          e.expenseAccountName.toLowerCase().includes(keyword) ||
          (e.partyName ?? '').toLowerCase().includes(keyword),
      );
    }

    if (this.paymentStatus()) {
      list = list.filter((e) => e.paymentStatus === this.paymentStatus());
    }

    if (this.status()) {
      list = list.filter((e) => e.status === this.status());
    }

    return list;
  });

  constructor(
    private expenseService: ExpenseService,
    private alert: AlertService,
  ) {}

  ngOnInit(): void {
    this.loadExpenses();
  }

  loadExpenses(): void {
    this.loading.set(true);

    this.expenseService.getAll().subscribe({
      next: (res) => {
        this.expenses.set([...res.data].sort((a, b) => (a.expenseDate < b.expenseDate ? 1 : -1)));
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.alert.error('Failed to load expenses');
      },
    });
  }

  clearFilter(): void {
    this.search.set('');
    this.paymentStatus.set('');
    this.status.set('');
  }

  totalAmount(): number {
    return this.expenses()
      .filter((e) => e.status === 'POSTED')
      .reduce((sum, e) => sum + Number(e.amount ?? 0), 0);
  }

  unpaidTotal(): number {
    return this.expenses()
      .filter((e) => e.status === 'POSTED')
      .reduce((sum, e) => sum + Number(e.dueAmount ?? 0), 0);
  }

  postedCount(): number {
    return this.expenses().filter((e) => e.status === 'POSTED').length;
  }

  draftCount(): number {
    return this.expenses().filter((e) => e.status === 'DRAFT').length;
  }

  cancelledCount(): number {
    return this.expenses().filter((e) => e.status === 'CANCELLED').length;
  }

  getPaymentStatusClass(status: ExpensePaymentStatus): string {
    return status.toLowerCase();
  }

  getStatusClass(status: ExpenseStatus): string {
    return status.toLowerCase();
  }

  async postExpense(id: number): Promise<void> {
    const confirmed = await this.alert.confirm('Post this expense? Journal entry will be created and cannot be undone by simply editing.');
    if (!confirmed) return;

    this.expenseService.post(id).subscribe({
      next: (res) => {
        this.alert.success('Expense posted');
        if (res.data.budgetWarnings && res.data.budgetWarnings.length > 0) {
          for (const w of res.data.budgetWarnings) {
            this.alert.warning(w.message);
          }
        }
        this.loadExpenses();
      },
      error: (error) => this.alert.error(error?.error?.message ?? 'Failed to post expense'),
    });
  }
}