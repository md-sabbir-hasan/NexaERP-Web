import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AlertService } from '../../../../core/services/alert.service';
import { RecurringExpenseStatus, RecurringExpenseTemplateResponse } from '../../models/recurring-expense.model';
import { RecurringExpenseService } from '../../services/recurring-expense.service';

@Component({
  selector: 'app-recurring-expense-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './recurring-expense-list.html',
  styleUrl: './recurring-expense-list.scss',
})
export class RecurringExpenseList implements OnInit {
  readonly loading = signal(false);
  readonly templates = signal<RecurringExpenseTemplateResponse[]>([]);

  readonly search = signal('');
  readonly status = signal('');

  readonly statuses: RecurringExpenseStatus[] = ['ACTIVE', 'PAUSED', 'ENDED'];

  readonly filteredTemplates = computed(() => {
    let list = [...this.templates()];

    if (this.search()) {
      const keyword = this.search().toLowerCase();
      list = list.filter(
        (t) =>
          t.name.toLowerCase().includes(keyword) ||
          t.expenseAccountName.toLowerCase().includes(keyword) ||
          (t.partyName ?? '').toLowerCase().includes(keyword),
      );
    }

    if (this.status()) {
      list = list.filter((t) => t.status === this.status());
    }

    return list;
  });

  constructor(
    private recurringExpenseService: RecurringExpenseService,
    private alert: AlertService,
  ) {}

  ngOnInit(): void {
    this.loadTemplates();
  }

  loadTemplates(): void {
    this.loading.set(true);

    this.recurringExpenseService.getAll().subscribe({
      next: (res) => {
        this.templates.set([...res.data].sort((a, b) => b.id - a.id));
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.alert.error('Failed to load recurring expense templates');
      },
    });
  }

  clearFilter(): void {
    this.search.set('');
    this.status.set('');
  }

  activeCount(): number {
    return this.templates().filter((t) => t.status === 'ACTIVE').length;
  }

  monthlyTotal(): number {
    const multiplier: Record<string, number> = { WEEKLY: 4.33, MONTHLY: 1, QUARTERLY: 1 / 3, YEARLY: 1 / 12 };
    return this.templates()
      .filter((t) => t.status === 'ACTIVE')
      .reduce((sum, t) => sum + Number(t.amount) * (multiplier[t.frequency] ?? 1), 0);
  }

  async pause(id: number): Promise<void> {
    const confirmed = await this.alert.confirm('Pause this recurring template?');
    if (!confirmed) return;

    this.recurringExpenseService.pause(id).subscribe({
      next: () => {
        this.alert.success('Template paused');
        this.loadTemplates();
      },
      error: (err) => this.alert.error(err?.error?.message ?? 'Failed to pause'),
    });
  }

  async resume(id: number): Promise<void> {
    const confirmed = await this.alert.confirm('Resume this recurring template?');
    if (!confirmed) return;

    this.recurringExpenseService.resume(id).subscribe({
      next: () => {
        this.alert.success('Template resumed');
        this.loadTemplates();
      },
      error: (err) => this.alert.error(err?.error?.message ?? 'Failed to resume'),
    });
  }

  async runNow(id: number): Promise<void> {
    const confirmed = await this.alert.confirm(
      'Generate an expense from this template right now? It will be created as a DRAFT for your review.',
    );
    if (!confirmed) return;

    this.recurringExpenseService.runNow(id).subscribe({
      next: () => {
        this.alert.success('Draft expense generated — review and post it from the Expenses page');
        this.loadTemplates();
      },
      error: (err) => this.alert.error(err?.error?.message ?? 'Failed to generate'),
    });
  }

  getStatusClass(status: RecurringExpenseStatus): string {
    return status.toLowerCase();
  }
}