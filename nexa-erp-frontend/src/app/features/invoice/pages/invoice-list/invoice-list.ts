import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AlertService } from '../../../../core/services/alert.service';
import { CancelledReason, Invoice, InvoiceStatus } from '../../models/invoice.model';
import { InvoiceService } from '../../services/invoice.service';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-invoice-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, DatePipe, DecimalPipe, HasPermissionDirective],
  templateUrl: './invoice-list.html',
  styleUrl: './invoice-list.scss',
})
export class InvoiceList implements OnInit {
  readonly invoices = signal<Invoice[]>([]);
  readonly loading = signal(false);

  readonly search = signal('');
  readonly status = signal<InvoiceStatus | ''>('');

  readonly statuses: InvoiceStatus[] = ['DRAFT', 'POSTED', 'PARTIAL', 'PAID', 'CANCELLED'];

  readonly filteredInvoices = computed(() => {
    const keyword = this.search().trim().toLowerCase();
    const status = this.status();

    return this.invoices().filter((invoice) => {
      const matchesSearch =
        !keyword ||
        invoice.invoiceNumber.toLowerCase().includes(keyword) ||
        invoice.partyName.toLowerCase().includes(keyword) ||
        (invoice.reference ?? '').toLowerCase().includes(keyword);

      const matchesStatus = !status || invoice.status === status;

      return matchesSearch && matchesStatus;
    });
  });

  readonly draftCount = computed(() => this.invoices().filter((i) => i.status === 'DRAFT').length);
  readonly postedCount = computed(
    () => this.invoices().filter((i) => i.status === 'POSTED').length,
  );
  readonly paidCount = computed(() => this.invoices().filter((i) => i.status === 'PAID').length);

  readonly totalDue = computed(() =>
    this.filteredInvoices().reduce((sum, invoice) => sum + Number(invoice.dueAmount ?? 0), 0),
  );

  constructor(
    private invoiceService: InvoiceService,
    private alert: AlertService,
  ) {}

  ngOnInit(): void {
    this.loadInvoices();
  }

  loadInvoices(): void {
    this.loading.set(true);

    this.invoiceService.getAll().subscribe({
      next: (res) => {
        this.invoices.set(res.data);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.alert.error('Failed to load invoices');
      },
    });
  }

  clearFilter(): void {
    this.search.set('');
    this.status.set('');
  }

  async postInvoice(invoice: Invoice): Promise<void> {
    const confirmed = await this.alert.confirm(`Post ${invoice.invoiceNumber}?`);
    if (!confirmed) return;

    this.invoiceService.post(invoice.id).subscribe({
      next: () => {
        this.alert.success('Invoice posted successfully');
        this.loadInvoices();
      },
      error: (error) => {
        this.alert.error(error?.error?.message ?? 'Failed to post invoice');
      },
    });
  }

  async cancelInvoice(invoice: Invoice): Promise<void> {
    const confirmed = await this.alert.confirm(`Cancel ${invoice.invoiceNumber}?`);
    if (!confirmed) return;

    const reason: CancelledReason = 'CUSTOMER_REQUESTED';

    this.invoiceService.cancel(invoice.id, reason).subscribe({
      next: () => {
        this.alert.success('Invoice cancelled successfully');
        this.loadInvoices();
      },
      error: (error) => {
        this.alert.error(error?.error?.message ?? 'Failed to cancel invoice');
      },
    });
  }

  getStatusClass(status: InvoiceStatus): string {
    return status.toLowerCase();
  }
}
