import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  EXCEL_MIME_TYPE,
  extractBlobErrorMessage,
  triggerBlobDownload,
} from '../../../../core/utils/file-download.util';
import { AlertService } from '../../../../core/services/alert.service';
import { ProfitLossResponse } from '../../models/profit-loss.model';
import { ReportService } from '../../services/report.service';

@Component({
  selector: 'app-profit-loss-report',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './profit-loss-report.html',
  styleUrl: './profit-loss-report.scss',
})
export class ProfitLossReport implements OnInit {
  readonly loading = signal(false);

  readonly report = signal<ProfitLossResponse | null>(null);

  readonly fromDate = signal('');
  readonly toDate = signal('');

  readonly revenueCount = computed(() => this.report()?.revenues.length ?? 0);

  readonly expenseCount = computed(() => this.report()?.expenses.length ?? 0);

  constructor(
    private readonly reportService: ReportService,
    private readonly alert: AlertService,
  ) {}

  ngOnInit(): void {
    this.setDefaultDates();
  }

  private setDefaultDates(): void {
    const today = new Date();
    const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);

    this.fromDate.set(this.formatDate(firstDay));
    this.toDate.set(this.formatDate(today));
  }

  private formatDate(date: Date): string {
    return date.toISOString().split('T')[0];
  }

  generateReport(): void {
    if (!this.fromDate() || !this.toDate()) {
      this.alert.error('Please select date range');
      return;
    }

    if (this.fromDate() > this.toDate()) {
      this.alert.error('From date cannot be after To date');
      return;
    }

    this.loading.set(true);
    this.report.set(null);

    this.reportService.getProfitLoss(this.fromDate(), this.toDate()).subscribe({
      next: (res) => {
        this.report.set(res.data);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);

        this.alert.error(err?.error?.message ?? 'Failed to generate Profit & Loss report');
      },
    });
  }

  clearReport(): void {
    this.report.set(null);
    this.setDefaultDates();
  }

  printReport(): void {
    window.print();
  }

  getNetProfitClass(): string {
    const value = this.report()?.netProfit ?? 0;

    if (value > 0) {
      return 'profit';
    }

    if (value < 0) {
      return 'loss';
    }

    return 'neutral';
  }
  exportReport(): void {
    if (!this.report()) {
      this.alert.error('Generate the report before exporting');
      return;
    }

    this.reportService.downloadProfitLossExcel(this.fromDate(), this.toDate()).subscribe({
      next: (blob) => {
        if (!blob || blob.size === 0) {
          this.alert.error('Generated Excel file is empty');
          return;
        }

        triggerBlobDownload(
          blob,
          `profit-and-loss-${this.fromDate()}-${this.toDate()}.xlsx`,
          EXCEL_MIME_TYPE,
        );
      },
      error: async (error) => {
        this.alert.error(await extractBlobErrorMessage(error, 'Failed to export P&L to Excel'));
      },
    });
  }
}
