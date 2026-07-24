import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AlertService } from '../../../../core/services/alert.service';
import { Account } from '../../../accounts/models/account.model';
import { AccountService } from '../../../accounts/services/account.service';
import { AccountingPeriod } from '../../../accounting-period/models/accounting-period.model';
import { AccountingPeriodService } from '../../../accounting-period/services/accounting-period.service';
import {
  BudgetAllocationMethod,
  BudgetLineRequest,
  BudgetPeriodAmountRequest,
  BudgetResponse,
} from '../../models/budget.model';
import { BudgetService } from '../../services/budget.service';

@Component({
  selector: 'app-budget-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './budget-detail.html',
  styleUrl: './budget-detail.scss',
})
export class BudgetDetail implements OnInit {
  readonly loading = signal(false);
  readonly budget = signal<BudgetResponse | null>(null);

  readonly budgetableAccounts = signal<Account[]>([]);
  readonly periods = signal<AccountingPeriod[]>([]);

  readonly showLineForm = signal(false);
  readonly editingLineId = signal<number | null>(null);
  readonly savingLine = signal(false);

  // add/edit line form state
  selectedAccountId: number | null = null;
  annualAmount = 0;
  allocationMethod: BudgetAllocationMethod = 'EQUAL';
  notes = '';
  periodAmounts: Record<number, number> = {};

  readonly manualTotal = computed(() => {
    return Object.values(this.periodAmounts).reduce((sum, v) => sum + Number(v || 0), 0);
  });

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private budgetService: BudgetService,
    private accountService: AccountService,
    private accountingPeriodService: AccountingPeriodService,
    private alert: AlertService,
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (id) {
      this.loadBudget(id);
    }
  }

  loadBudget(id: number): void {
    this.loading.set(true);

    this.budgetService.getById(id).subscribe({
      next: (res) => {
        this.budget.set(res.data);
        this.loading.set(false);
        this.loadAccounts();
        this.loadPeriods(res.data.fiscalYearId);
      },
      error: () => {
        this.loading.set(false);
        this.alert.error('Failed to load budget');
      },
    });
  }

  loadAccounts(): void {
    this.accountService.search('', 'EXPENSE', true).subscribe({
      next: (res1) => {
        this.accountService.search('', 'REVENUE', true).subscribe({
          next: (res2) => {
            const merged = [...res1.data, ...res2.data].sort((a, b) => a.code.localeCompare(b.code));
            this.budgetableAccounts.set(merged);
          },
        });
      },
    });
  }

  loadPeriods(fiscalYearId: number): void {
    this.accountingPeriodService.getAll(fiscalYearId).subscribe({
      next: (res) => {
        this.periods.set([...res.data].sort((a, b) => a.periodNumber - b.periodNumber));
      },
      error: () => this.alert.error('Failed to load accounting periods'),
    });
  }

  get isDraft(): boolean {
    return this.budget()?.status === 'DRAFT';
  }

  openAddLine(): void {
    this.editingLineId.set(null);
    this.selectedAccountId = null;
    this.annualAmount = 0;
    this.allocationMethod = 'EQUAL';
    this.notes = '';
    this.periodAmounts = {};
    this.showLineForm.set(true);
  }

  openEditLine(lineId: number): void {
    const line = this.budget()?.lines.find((l) => l.id === lineId);
    if (!line) return;

    this.editingLineId.set(lineId);
    this.selectedAccountId = line.accountId;
    this.annualAmount = line.annualAmount;
    this.allocationMethod = line.allocationMethod;
    this.notes = line.notes ?? '';

    this.periodAmounts = {};
    for (const alloc of line.periodAllocations) {
      this.periodAmounts[alloc.accountingPeriodId] = alloc.budgetAmount;
    }

    this.showLineForm.set(true);
  }

  cancelLineForm(): void {
    this.showLineForm.set(false);
    this.editingLineId.set(null);
  }

  onAllocationMethodChange(): void {
    if (this.allocationMethod === 'EQUAL' && this.periods().length > 0 && this.annualAmount > 0) {
      // just a UI preview split, real split happens server-side with correct rounding
      const equalShare = Math.floor((this.annualAmount / this.periods().length) * 100) / 100;
      this.periodAmounts = {};
      for (const p of this.periods()) {
        this.periodAmounts[p.id] = equalShare;
      }
    }
  }

  saveLine(): void {
    const budget = this.budget();
    if (!budget) return;

    if (!this.selectedAccountId) {
      this.alert.warning('Select an account');
      return;
    }
    if (!this.annualAmount || this.annualAmount <= 0) {
      this.alert.warning('Annual amount must be greater than 0');
      return;
    }

    const request: BudgetLineRequest = {
      accountId: this.selectedAccountId,
      annualAmount: this.annualAmount,
      allocationMethod: this.allocationMethod,
      notes: this.notes?.trim() || undefined,
    };

    if (this.allocationMethod === 'MANUAL') {
      const periodAmounts: BudgetPeriodAmountRequest[] = this.periods().map((p) => ({
        accountingPeriodId: p.id,
        amount: Number(this.periodAmounts[p.id] || 0),
      }));

      const total = periodAmounts.reduce((sum, pa) => sum + pa.amount, 0);
      if (total !== this.annualAmount) {
        this.alert.warning(
          `Sum of period amounts (${total.toFixed(2)}) must equal the annual amount (${this.annualAmount.toFixed(2)})`,
        );
        return;
      }

      request.periodAmounts = periodAmounts;
    }

    this.savingLine.set(true);

    const lineId = this.editingLineId();
    const request$ = lineId
      ? this.budgetService.updateLine(budget.id, lineId, request)
      : this.budgetService.addLine(budget.id, request);

    request$.subscribe({
      next: () => {
        this.savingLine.set(false);
        this.alert.success(lineId ? 'Budget line updated' : 'Budget line added');
        this.showLineForm.set(false);
        this.loadBudget(budget.id);
      },
      error: (error) => {
        this.savingLine.set(false);
        this.alert.error(error?.error?.message ?? 'Failed to save budget line');
      },
    });
  }

  async deleteLine(lineId: number): Promise<void> {
    const budget = this.budget();
    if (!budget) return;

    const confirmed = await this.alert.confirm('Delete this budget line?');
    if (!confirmed) return;

    this.budgetService.deleteLine(budget.id, lineId).subscribe({
      next: () => {
        this.alert.success('Budget line deleted');
        this.loadBudget(budget.id);
      },
      error: (error) => this.alert.error(error?.error?.message ?? 'Failed to delete line'),
    });
  }

  async activateBudget(): Promise<void> {
    const budget = this.budget();
    if (!budget) return;

    const confirmed = await this.alert.confirm(`Activate ${budget.budgetNumber}? This will apply spending checks.`);
    if (!confirmed) return;

    this.budgetService.activate(budget.id).subscribe({
      next: (res) => {
        this.budget.set(res.data);
        this.alert.success('Budget activated');
      },
      error: (error) => this.alert.error(error?.error?.message ?? 'Failed to activate budget'),
    });
  }

  async closeBudget(): Promise<void> {
    const budget = this.budget();
    if (!budget) return;

    const confirmed = await this.alert.confirm(`Close ${budget.budgetNumber}?`);
    if (!confirmed) return;

    this.budgetService.close(budget.id).subscribe({
      next: (res) => {
        this.budget.set(res.data);
        this.alert.success('Budget closed');
      },
      error: (error) => this.alert.error(error?.error?.message ?? 'Failed to close budget'),
    });
  }

  async deleteBudget(): Promise<void> {
    const budget = this.budget();
    if (!budget) return;

    const confirmed = await this.alert.confirm(`Delete ${budget.budgetNumber}? This cannot be undone.`);
    if (!confirmed) return;

    this.budgetService.delete(budget.id).subscribe({
      next: () => {
        this.alert.success('Budget deleted');
        this.router.navigate(['/budget']);
      },
      error: (error) => this.alert.error(error?.error?.message ?? 'Failed to delete budget'),
    });
  }
}