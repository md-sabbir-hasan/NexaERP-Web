import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AlertService } from '../../../../core/services/alert.service';
import { EXCEL_MIME_TYPE, extractBlobErrorMessage, triggerBlobDownload } from '../../../../core/utils/file-download.util';
import { CashFlowActivitySection, CashFlowStatementResponse } from '../../models/cash-flow.model';
import { ReportService } from '../../services/report.service';

@Component({
  selector: 'app-cash-flow-report',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './cash-flow-report.html',
  styleUrl: './cash-flow-report.scss',
})
export class CashFlowReport implements OnInit {
  readonly loading = signal(false);
  readonly exporting = signal(false);
  readonly report = signal<CashFlowStatementResponse | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly errorStatus = signal<number | null>(null);
  readonly fromDate = signal('');
  readonly toDate = signal('');
  readonly hasPeriodMovement = computed(() => (this.report()?.netChangeInCash ?? 0) !== 0);
  readonly hasAnyBalance = computed(() => {
    const data = this.report();
    return !!data && (data.openingCashBalance !== 0 || data.ledgerClosingCashBalance !== 0);
  });
  readonly transferOnly = computed(() => {
    const data = this.report();
    return !!data && !this.hasPeriodMovement() && data.cashAccounts.some((a) => a.periodMovement !== 0);
  });

  constructor(private readonly reportService: ReportService, private readonly alert: AlertService) {}

  ngOnInit(): void { this.resetDates(); }

  generateReport(): void {
    if (!this.fromDate() || !this.toDate()) { this.alert.error('Please select a date range'); return; }
    if (this.fromDate() > this.toDate()) { this.alert.error('From date cannot be after To date'); return; }
    this.loading.set(true); this.errorMessage.set(null); this.errorStatus.set(null); this.report.set(null);
    this.reportService.getCashFlow(this.fromDate(), this.toDate()).subscribe({
      next: (response) => { this.report.set(response.data); this.loading.set(false); },
      error: (error) => {
        this.loading.set(false); this.errorStatus.set(error?.status ?? null);
        this.errorMessage.set(error?.status === 403 ? 'You do not have permission to view this report.'
          : error?.error?.message ?? 'Failed to generate Cash Flow Statement');
      },
    });
  }

  reset(): void { this.report.set(null); this.errorMessage.set(null); this.resetDates(); }
  retry(): void { this.generateReport(); }
  printReport(): void { window.print(); }

  exportExcel(): void {
    if (!this.report()) { this.alert.error('Generate the report before exporting'); return; }
    this.exporting.set(true);
    this.reportService.downloadCashFlowExcel(this.fromDate(), this.toDate()).subscribe({
      next: (blob) => {
        this.exporting.set(false);
        if (!blob.size) { this.alert.error('Generated Excel file is empty'); return; }
        triggerBlobDownload(blob, `cash-flow-${this.fromDate()}-${this.toDate()}.xlsx`, EXCEL_MIME_TYPE);
      },
      error: async (error) => {
        this.exporting.set(false);
        this.alert.error(await extractBlobErrorMessage(error, 'Failed to export Cash Flow Statement'));
      },
    });
  }

  trackSection(section: CashFlowActivitySection): string { return section.activity; }
  private resetDates(): void {
    const today = new Date();
    this.fromDate.set(this.formatDate(new Date(today.getFullYear(), today.getMonth(), 1)));
    this.toDate.set(this.formatDate(today));
  }
  private formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
