import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { AlertService } from '../../../../core/services/alert.service';
import { AccountingPeriod } from '../../../accounting-period/models/accounting-period.model';
import { AccountingPeriodService } from '../../../accounting-period/services/accounting-period.service';
import { BudgetVarianceResponse } from '../../models/budget.model';
import { BudgetService } from '../../services/budget.service';

@Component({
  selector: 'app-budget-variance',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './budget-variance.html',
  styleUrl: './budget-variance.scss',
})
export class BudgetVariance implements OnInit {
  readonly loading = signal(false);
  readonly variance = signal<BudgetVarianceResponse | null>(null);
  readonly periods = signal<AccountingPeriod[]>([]);

  budgetId!: number;
  selectedPeriodId: number | null = null;
  fromDate = '';
  toDate = '';

  constructor(
    private route: ActivatedRoute,
    private budgetService: BudgetService,
    private accountingPeriodService: AccountingPeriodService,
    private alert: AlertService,
  ) {}

  ngOnInit(): void {
    this.budgetId = Number(this.route.snapshot.paramMap.get('id'));
    if (this.budgetId) {
      this.loadVariance();
    }
  }

  loadVariance(): void {
    this.loading.set(true);

    this.budgetService
      .getVariance(this.budgetId, {
        periodId: this.selectedPeriodId ?? undefined,
        fromDate: this.selectedPeriodId ? undefined : this.fromDate || undefined,
        toDate: this.selectedPeriodId ? undefined : this.toDate || undefined,
      })
      .subscribe({
        next: (res) => {
          this.variance.set(res.data);
          this.loading.set(false);

          if (this.periods().length === 0) {
            this.accountingPeriodService.getAll(res.data.fiscalYearId).subscribe({
              next: (periodRes) => {
                this.periods.set([...periodRes.data].sort((a, b) => a.periodNumber - b.periodNumber));
              },
            });
          }
        },
        error: () => {
          this.loading.set(false);
          this.alert.error('Failed to load variance report');
        },
      });
  }

  onPeriodChange(): void {
    this.fromDate = '';
    this.toDate = '';
    this.loadVariance();
  }

  applyDateRange(): void {
    if (this.fromDate && this.toDate) {
      this.selectedPeriodId = null;
      this.loadVariance();
    } else {
      this.alert.warning('Select both from and to dates');
    }
  }

  clearFilters(): void {
    this.selectedPeriodId = null;
    this.fromDate = '';
    this.toDate = '';
    this.loadVariance();
  }

  getStatusClass(status: string): string {
    return status.toLowerCase().replace('_', '-');
  }
}