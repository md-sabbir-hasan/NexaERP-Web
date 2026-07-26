import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { EXCEL_MIME_TYPE, triggerBlobDownload } from '../../../../core/utils/file-download.util';
import {
  BudgetReportAccountType,
  BudgetVsActualOption,
  BudgetVsActualResponse,
} from '../../models/budget.model';
import { ReportService } from '../../../reports/services/report.service';
import { BudgetService } from '../../services/budget.service';

@Component({
  selector: 'app-budget-variance',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './budget-variance.html',
  styleUrl: './budget-variance.scss',
})
export class BudgetVariance implements OnInit {
  readonly loadingOptions = signal(true);
  readonly loading = signal(false);
  readonly exporting = signal(false);
  readonly options = signal<BudgetVsActualOption[]>([]);
  readonly report = signal<BudgetVsActualResponse | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly forbidden = signal(false);
  readonly attempted = signal(false);

  selectedBudgetId: number | null = null;
  fromPeriodId: number | null = null;
  toPeriodId: number | null = null;
  accountType: BudgetReportAccountType | '' = '';

  readonly selectedBudget = computed(() =>
    this.options().find((item) => item.budgetId === this.selectedBudgetId) ?? null,
  );
  readonly availableBudgets = computed(() =>
    this.options().filter((item) => item.budgetStatus === 'ACTIVE' || item.budgetStatus === 'CLOSED'),
  );
  readonly noActualActivity = computed(() => {
    const value = this.report();
    return !!value && value.totalRevenueActual === 0 && value.totalExpenseActual === 0;
  });
  readonly emptyLines = computed(() => {
    const value = this.report();
    return !!value && value.revenueLines.length === 0 && value.expenseLines.length === 0;
  });

  constructor(
    private route: ActivatedRoute,
    private reportService: ReportService,
    private budgetService: BudgetService,
  ) {}

  ngOnInit(): void {
    const routeBudgetId = Number(this.route.snapshot.paramMap.get('id')) || null;
    const queryBudgetId = Number(this.route.snapshot.queryParamMap.get('budgetId')) || null;
    this.selectedBudgetId = routeBudgetId ?? queryBudgetId;
    this.loadOptions();
  }

  loadOptions(): void {
    this.loadingOptions.set(true);
    this.errorMessage.set(null);
    this.reportService.getBudgetVsActualOptions().subscribe({
      next: ({ data }) => {
        const options = data.map((option) => ({
          ...option,
          budgetId: Number(option.budgetId),
          periods: option.periods.map((period) => ({ ...period, id: Number(period.id) })),
        }));
        this.options.set(options);
        this.loadingOptions.set(false);
        this.initializeBudgetSelection(this.selectedBudgetId, true);
      },
      error: (error: HttpErrorResponse) => this.handleError(error, true),
    });
  }

  onBudgetChange(): void {
    this.report.set(null);
    this.attempted.set(false);
    const budgetId = Number(this.selectedBudgetId);
    this.selectedBudgetId = Number.isFinite(budgetId) && budgetId > 0 ? budgetId : null;
    const selected = this.selectedBudget();
    if (selected) {
      this.setFullRange(selected);
    } else {
      this.fromPeriodId = null;
      this.toPeriodId = null;
    }
  }

  generate(): void {
    if (!this.validate()) return;
    this.loading.set(true);
    this.attempted.set(true);
    this.errorMessage.set(null);
    this.forbidden.set(false);
    this.reportService.getBudgetVsActual(
      this.selectedBudgetId!, this.fromPeriodId!, this.toPeriodId!, this.accountType || undefined,
    ).subscribe({
      next: ({ data }) => { this.report.set(data); this.loading.set(false); },
      error: (error: HttpErrorResponse) => this.handleError(error, false),
    });
  }

  reset(): void {
    this.accountType = '';
    this.report.set(null);
    this.errorMessage.set(null);
    this.attempted.set(false);
    this.forbidden.set(false);
    this.initializeBudgetSelection(null, false);
  }

  exportExcel(): void {
    if (!this.validate()) return;
    this.exporting.set(true);
    this.reportService.downloadBudgetVsActualExcel(
      this.selectedBudgetId!, this.fromPeriodId!, this.toPeriodId!, this.accountType || undefined,
    ).subscribe({
      next: (blob) => {
        triggerBlobDownload(blob, `budget-vs-actual-${this.report()?.fromDate ?? 'report'}.xlsx`, EXCEL_MIME_TYPE);
        this.exporting.set(false);
      },
      error: (error: HttpErrorResponse) => { this.exporting.set(false); this.handleError(error, false); },
    });
  }

  printReport(): void { window.print(); }
  retry(): void { this.selectedBudgetId ? this.generate() : this.loadOptions(); }

  private setFullRange(option: BudgetVsActualOption): void {
    this.fromPeriodId = option.periods[0]?.id ?? null;
    this.toPeriodId = option.periods[option.periods.length - 1]?.id ?? null;
  }

  private initializeBudgetSelection(requestedBudgetId: number | null, generateReport: boolean): void {
    const budgets = this.availableBudgets();
    const requestedId = Number(requestedBudgetId);
    const selected = budgets.find((option) => option.budgetId === requestedId)
      ?? budgets.find((option) => option.budgetStatus === 'ACTIVE')
      ?? budgets[0]
      ?? null;

    if (!selected) {
      this.selectedBudgetId = null;
      this.fromPeriodId = null;
      this.toPeriodId = null;
      return;
    }

    this.selectedBudgetId = selected.budgetId;
    this.setFullRange(selected);

    if (!generateReport) return;
    if (selected.budgetStatus === 'DRAFT') this.loadLegacyDraftPreview(selected);
    else this.generate();
  }

  private validate(): boolean {
    const budget = this.selectedBudget();
    if (!budget) return this.fail('Select a budget before generating the report.');
    if (budget.budgetStatus === 'DRAFT') return this.fail(
      'Draft budgets remain available from the legacy budget preview; select an ACTIVE or CLOSED budget here.');
    if (!this.fromPeriodId || !this.toPeriodId) return this.fail('Select both From Period and To Period.');
    const from = budget.periods.findIndex((period) => period.id === this.fromPeriodId);
    const to = budget.periods.findIndex((period) => period.id === this.toPeriodId);
    if (from < 0 || to < 0 || from > to) return this.fail('Select a valid chronological period range.');
    this.errorMessage.set(null);
    return true;
  }

  private fail(message: string): false { this.errorMessage.set(message); return false; }

  private loadLegacyDraftPreview(option: BudgetVsActualOption): void {
    this.loading.set(true);
    this.attempted.set(true);
    this.budgetService.getVariance(option.budgetId).subscribe({
      next: ({ data }) => {
        const revenueLines = data.lines.filter((line) => line.accountType === 'REVENUE').map((line) => ({
          ...line, budgetLineId: line.accountId, accountType: 'REVENUE' as const,
          remainingAmount: line.remainingAmount,
        }));
        const expenseLines = data.lines.filter((line) => line.accountType === 'EXPENSE').map((line) => ({
          ...line, budgetLineId: line.accountId, accountType: 'EXPENSE' as const,
          remainingAmount: line.remainingAmount,
        }));
        this.report.set({
          budgetId: option.budgetId, budgetNumber: option.budgetNumber, budgetName: option.budgetName,
          budgetStatus: option.budgetStatus, fiscalYearId: option.fiscalYearId,
          fiscalYearName: option.fiscalYearName, currencyCode: 'BDT',
          fromPeriodId: option.periods[0]?.id ?? null,
          toPeriodId: option.periods[option.periods.length - 1]?.id ?? null,
          selectedPeriodIds: option.periods.map((period) => period.id),
          fromDate: data.fromDate, toDate: data.toDate,
          totalRevenueBudget: data.totalRevenueBudget, totalRevenueActual: data.totalRevenueActual,
          totalRevenueVariance: data.totalRevenueVariance,
          revenueAchievementPercent: data.revenueAchievementPercent,
          totalExpenseBudget: data.totalExpenseBudget, totalExpenseActual: data.totalExpenseActual,
          totalExpenseVariance: data.totalExpenseVariance,
          expenseUtilizationPercent: data.expenseUtilizationPercent,
          revenueLines, expenseLines, generatedAt: data.generatedAt,
        });
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => this.handleError(error, false),
    });
  }

  private handleError(error: HttpErrorResponse, options: boolean): void {
    this.loading.set(false);
    this.loadingOptions.set(false);
    this.forbidden.set(error.status === 403);
    const payload = error.error as { message?: string } | null;
    this.errorMessage.set(error.status === 403 ? 'You do not have permission to view this report.'
      : payload?.message ?? (options ? 'Unable to load report options.' : 'Unable to generate the report.'));
  }
}
