import { CommonModule, DecimalPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AlertService } from '../../../../core/services/alert.service';
import { Account } from '../../../accounts/models/account.model';
import { AccountService } from '../../../accounts/services/account.service';
import { LedgerResponse } from '../../models/ledger.model';
import { ReportService } from '../../services/report.service';
import {
  EXCEL_MIME_TYPE,
  extractBlobErrorMessage,
  triggerBlobDownload,
} from '../../../../core/utils/file-download.util';

@Component({
  selector: 'app-ledger',
  standalone: true,
  imports: [CommonModule, FormsModule, DecimalPipe],
  templateUrl: './ledger.html',
  styleUrl: './ledger.scss',
})
export class Ledger implements OnInit {
  readonly accounts = signal<Account[]>([]);
  readonly ledger = signal<LedgerResponse | null>(null);
  readonly loading = signal(false);

  accountId: number | null = null;
  fromDate = '';
  toDate = '';

  constructor(
    private accountService: AccountService,
    private reportService: ReportService,
    private alert: AlertService,
  ) {}

  ngOnInit(): void {
    const today = new Date().toISOString().substring(0, 10);

    this.fromDate = today.substring(0, 8) + '01';
    this.toDate = today;

    this.loadAccounts();
  }

  loadAccounts(): void {
    this.accountService.search('', '', true).subscribe({
      next: (res) => {
        this.accounts.set(res.data);

        if (res.data.length > 0) {
          this.accountId = res.data[0].id;
        }
      },
      error: () => {
        this.alert.error('Failed to load accounts');
      },
    });
  }

  generateReport(): void {
    if (!this.accountId) {
      this.alert.error('Please select an account');
      return;
    }

    if (!this.fromDate || !this.toDate) {
      this.alert.error('Please select date range');
      return;
    }

    if (this.fromDate > this.toDate) {
      this.alert.error('From date cannot be after To date');
      return;
    }

    this.loading.set(true);

    this.reportService.getLedger(this.accountId, this.fromDate, this.toDate).subscribe({
      next: (res) => {
        this.ledger.set(res.data);
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.alert.error(error?.error?.message ?? 'Failed to load ledger');
      },
    });
  }

  clearReport(): void {
    this.ledger.set(null);
  }

  getBalanceSide(amount: number): 'Dr' | 'Cr' {
    return Number(amount) >= 0 ? 'Dr' : 'Cr';
  }

  abs(value: number): number {
    return Math.abs(Number(value ?? 0));
  }

  printReport(): void {
    window.print();
  }

  exportReport(): void {
    if (!this.accountId || !this.ledger()) {
      this.alert.error('Generate the ledger before exporting');
      return;
    }

    this.reportService.downloadLedgerExcel(this.accountId, this.fromDate, this.toDate).subscribe({
      next: (blob) => {
        if (!blob || blob.size === 0) {
          this.alert.error('Generated Excel file is empty');
          return;
        }

        triggerBlobDownload(
          blob,
          `ledger-${this.accountId}-${this.fromDate}-${this.toDate}.xlsx`,
          EXCEL_MIME_TYPE,
        );
      },
      error: async (error) => {
        this.alert.error(await extractBlobErrorMessage(error, 'Failed to export ledger to Excel'));
      },
    });
  }

  refreshReport(): void {
    if (this.ledger()) {
      this.generateReport();
    }
  }
}
