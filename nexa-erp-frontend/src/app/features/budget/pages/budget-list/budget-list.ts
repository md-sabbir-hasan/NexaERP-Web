import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AlertService } from '../../../../core/services/alert.service';
import { BudgetResponse, BudgetStatus } from '../../models/budget.model';
import { BudgetService } from '../../services/budget.service';

@Component({
  selector: 'app-budget-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './budget-list.html',
  styleUrl: './budget-list.scss',
})
export class BudgetList implements OnInit {
  readonly loading = signal(false);
  readonly budgets = signal<BudgetResponse[]>([]);

  readonly search = signal('');
  readonly status = signal('');

  readonly statuses: BudgetStatus[] = ['DRAFT', 'APPROVED', 'ACTIVE', 'CLOSED', 'CANCELLED'];

  readonly filteredBudgets = computed(() => {
    let list = [...this.budgets()];

    if (this.search()) {
      const keyword = this.search().toLowerCase();
      list = list.filter(
        (b) =>
          b.budgetNumber.toLowerCase().includes(keyword) ||
          b.name.toLowerCase().includes(keyword) ||
          b.fiscalYearName.toLowerCase().includes(keyword),
      );
    }

    if (this.status()) {
      list = list.filter((b) => b.status === this.status());
    }

    return list;
  });

  constructor(
    private budgetService: BudgetService,
    private alert: AlertService,
  ) {}

  ngOnInit(): void {
    this.loadBudgets();
  }

  loadBudgets(): void {
    this.loading.set(true);

    this.budgetService.getAll().subscribe({
      next: (res) => {
        this.budgets.set([...res.data].sort((a, b) => b.id - a.id));
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.alert.error('Failed to load budgets');
      },
    });
  }

  clearFilter(): void {
    this.search.set('');
    this.status.set('');
  }

  activeCount(): number {
    return this.budgets().filter((b) => b.status === 'ACTIVE').length;
  }

  draftCount(): number {
    return this.budgets().filter((b) => b.status === 'DRAFT').length;
  }

  totalExpenseBudget(): number {
    return this.budgets()
      .filter((b) => b.status === 'ACTIVE')
      .reduce((sum, b) => sum + Number(b.totalExpenseBudget ?? 0), 0);
  }

  totalRevenueBudget(): number {
    return this.budgets()
      .filter((b) => b.status === 'ACTIVE')
      .reduce((sum, b) => sum + Number(b.totalRevenueBudget ?? 0), 0);
  }

  getStatusClass(status: BudgetStatus): string {
    return status.toLowerCase();
  }
}