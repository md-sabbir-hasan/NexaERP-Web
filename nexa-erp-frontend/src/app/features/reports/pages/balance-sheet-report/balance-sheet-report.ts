import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AlertService } from '../../../../core/services/alert.service';
import { BalanceSheetResponse } from '../../models/balance-sheet.model';
import { ReportService } from '../../services/report.service';
import {
  EXCEL_MIME_TYPE,
  extractBlobErrorMessage,
  triggerBlobDownload,
} from '../../../../core/utils/file-download.util';

@Component({
  selector: 'app-balance-sheet-report',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './balance-sheet-report.html',
  styleUrl: './balance-sheet-report.scss',
})
export class BalanceSheetReport implements OnInit {
  readonly loading = signal(false);
  readonly report = signal<BalanceSheetResponse | null>(null);
  readonly asOfDate = signal('');

  readonly assetCount = computed(() => this.report()?.assets.length ?? 0);

  readonly liabilityCount = computed(() => this.report()?.liabilities.length ?? 0);

  readonly equityCount = computed(() => this.report()?.equity.length ?? 0);

  constructor(
    private readonly reportService: ReportService,
    private readonly alert: AlertService,
  ) {}

  ngOnInit(): void {
    this.asOfDate.set(this.formatDate(new Date()));
  }

  private formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
  }

  generateReport(): void {
    const date = this.asOfDate();

    if (!date) {
      this.alert.error('Please select an as-of date');
      return;
    }

    this.loading.set(true);
    this.report.set(null);

    this.reportService.getBalanceSheet(date).subscribe({
      next: (res) => {
        this.report.set(res.data);
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.alert.error(error?.error?.message ?? 'Failed to generate Balance Sheet');
      },
    });
  }

  clearReport(): void {
    this.report.set(null);
    this.asOfDate.set(this.formatDate(new Date()));
  }

  printReport(): void {
    window.print();
  }

  getBalanceStatusClass(): string {
    return this.report()?.isBalanced ? 'balanced' : 'unbalanced';
  }

  getDifference(): number {
    const report = this.report();

    if (!report) {
      return 0;
    }

    return Number(report.totalAssets) - Number(report.totalLiabilitiesAndEquity);
  }

  exportReport(): void {
    if (!this.report()) {
      this.alert.error('Generate the balance sheet before exporting');
      return;
    }

    this.reportService.downloadBalanceSheetExcel(this.asOfDate()).subscribe({
      next: (blob) => {
        if (!blob || blob.size === 0) {
          this.alert.error('Generated Excel file is empty');
          return;
        }

        triggerBlobDownload(blob, `balance-sheet-${this.asOfDate()}.xlsx`, EXCEL_MIME_TYPE);
      },
      error: async (error) => {
        this.alert.error(
          await extractBlobErrorMessage(error, 'Failed to export balance sheet to Excel'),
        );
      },
    });
  }
}
