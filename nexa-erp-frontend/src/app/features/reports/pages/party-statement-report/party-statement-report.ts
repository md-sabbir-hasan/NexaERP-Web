import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  EXCEL_MIME_TYPE,
  extractBlobErrorMessage,
  toSafeFilenamePart,
  triggerBlobDownload,
} from '../../../../core/utils/file-download.util';
import { AlertService } from '../../../../core/services/alert.service';
import { Party } from '../../../party/models/party.model';
import { PartyService } from '../../../party/services/party.service';
import { PartyStatementResponse } from '../../models/party-statement.model';
import { ReportService } from '../../services/report.service';

@Component({
  selector: 'app-party-statement-report',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './party-statement-report.html',
  styleUrl: './party-statement-report.scss',
})
export class PartyStatementReport implements OnInit {
  readonly loading = signal(false);
  readonly parties = signal<Party[]>([]);
  readonly statement = signal<PartyStatementResponse | null>(null);

  readonly selectedPartyId = signal<number | null>(null);
  readonly fromDate = signal('');
  readonly toDate = signal('');

  readonly totalDebit = computed(
    () => this.statement()?.entries.reduce((sum, entry) => sum + Number(entry.debit ?? 0), 0) ?? 0,
  );

  readonly totalCredit = computed(
    () => this.statement()?.entries.reduce((sum, entry) => sum + Number(entry.credit ?? 0), 0) ?? 0,
  );

  constructor(
    private readonly reportService: ReportService,
    private readonly partyService: PartyService,
    private readonly alert: AlertService,
  ) {}

  ngOnInit(): void {
    this.setDefaultDates();
    this.loadParties();
  }

  private setDefaultDates(): void {
    const today = new Date();
    const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);

    this.fromDate.set(this.formatDate(firstDay));
    this.toDate.set(this.formatDate(today));
  }

  private formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
  }

  loadParties(): void {
    this.partyService.getAll().subscribe({
      next: (res) => {
        this.parties.set(
          res.data.filter((party) => party.isActive).sort((a, b) => a.name.localeCompare(b.name)),
        );
      },
      error: () => {
        this.alert.error('Failed to load parties');
      },
    });
  }

  generateReport(): void {
    const partyId = this.selectedPartyId();
    const fromDate = this.fromDate();
    const toDate = this.toDate();

    if (!partyId) {
      this.alert.error('Please select a party');
      return;
    }

    if (!fromDate || !toDate) {
      this.alert.error('Please select a valid date range');
      return;
    }

    if (fromDate > toDate) {
      this.alert.error('From date cannot be after to date');
      return;
    }

    this.loading.set(true);
    this.statement.set(null);

    this.reportService.getPartyStatement(partyId, fromDate, toDate).subscribe({
      next: (res) => {
        this.statement.set(res.data);
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.alert.error(error?.error?.message ?? 'Failed to generate party statement');
      },
    });
  }

  clearReport(): void {
    this.selectedPartyId.set(null);
    this.statement.set(null);
    this.setDefaultDates();
  }

  printReport(): void {
    window.print();
  }

  getEntryTypeLabel(type: string): string {
    switch (type) {
      case 'VENDOR_BILL':
        return 'Vendor Bill';
      case 'OPENING_BALANCE':
        return 'Opening Balance';
      default:
        return type
          .toLowerCase()
          .replaceAll('_', ' ')
          .replace(/\b\w/g, (char) => char.toUpperCase());
    }
  }

  getEntryTypeClass(type: string): string {
    return type.toLowerCase().replaceAll('_', '-');
  }

  // Download Party Statement as PDF
  downloadPdf(): void {
    const partyId = this.selectedPartyId();
    const fromDate = this.fromDate();
    const toDate = this.toDate();
    const statement = this.statement();

    if (!partyId || !fromDate || !toDate || !statement) {
      this.alert.error('Generate the statement before downloading PDF');
      return;
    }

    this.reportService.downloadPartyStatementPdf(partyId, fromDate, toDate).subscribe({
      next: (blob) => {
        if (!blob || blob.size === 0) {
          this.alert.error('Generated PDF is empty');
          return;
        }

        const objectUrl = URL.createObjectURL(new Blob([blob], { type: 'application/pdf' }));

        const anchor = document.createElement('a');

        const safePartyName = statement.partyName
          .trim()
          .replace(/[^a-zA-Z0-9-_]+/g, '-')
          .replace(/^-+|-+$/g, '');

        anchor.href = objectUrl;
        anchor.download = `party-statement-${safePartyName || partyId}-${fromDate}-${toDate}.pdf`;

        document.body.appendChild(anchor);
        anchor.click();
        anchor.remove();

        setTimeout(() => {
          URL.revokeObjectURL(objectUrl);
        }, 1000);
      },

      error: async (error) => {
        let message = 'Failed to download party statement PDF';

        if (error?.error instanceof Blob) {
          try {
            const text = await error.error.text();
            const body = JSON.parse(text);

            message = body?.message ?? message;
          } catch {}
        } else {
          message = error?.error?.message ?? message;
        }

        this.alert.error(message);
      },
    });
  }

  // Download Party Statement as Excel
  downloadExcel(): void {
    const partyId = this.selectedPartyId();
    const fromDate = this.fromDate();
    const toDate = this.toDate();
    const statement = this.statement();

    if (!partyId || !fromDate || !toDate || !statement) {
      this.alert.error('Generate the statement before downloading Excel');
      return;
    }

    this.reportService.downloadPartyStatementExcel(partyId, fromDate, toDate).subscribe({
      next: (blob) => {
        if (!blob || blob.size === 0) {
          this.alert.error('Generated Excel file is empty');
          return;
        }

        const safePartyName = toSafeFilenamePart(statement.partyName);

        triggerBlobDownload(
          blob,
          `party-statement-${safePartyName || partyId}-${fromDate}-${toDate}.xlsx`,
          EXCEL_MIME_TYPE,
        );
      },
      error: async (error) => {
        this.alert.error(
          await extractBlobErrorMessage(error, 'Failed to download party statement Excel'),
        );
      },
    });
  }
}
