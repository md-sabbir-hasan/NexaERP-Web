import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AlertService } from '../../../../core/services/alert.service';
import { CancelledReason, Invoice, InvoiceStatus } from '../../models/invoice.model';
import { InvoiceService } from '../../services/invoice.service';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-invoice-details',
  standalone: true,
  imports: [CommonModule, RouterLink, DatePipe, DecimalPipe, HasPermissionDirective],
  templateUrl: './invoice-details.html',
  styleUrl: './invoice-details.scss',
})
export class InvoiceDetails implements OnInit {
  readonly invoice = signal<Invoice | null>(null);
  readonly loading = signal(false);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private invoiceService: InvoiceService,
    private alert: AlertService,
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));

    if (!id) {
      this.router.navigate(['/invoice']);
      return;
    }

    this.loadInvoice(id);
  }

  loadInvoice(id: number): void {
    this.loading.set(true);

    this.invoiceService.getById(id).subscribe({
      next: (res) => {
        this.invoice.set(res.data);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.alert.error('Failed to load invoice');
        this.router.navigate(['/invoice']);
      },
    });
  }

  async postInvoice(): Promise<void> {
    const invoice = this.invoice();
    if (!invoice) return;

    const confirmed = await this.alert.confirm(`Post ${invoice.invoiceNumber}?`);
    if (!confirmed) return;

    this.invoiceService.post(invoice.id).subscribe({
      next: (res) => {
        this.alert.success('Invoice posted successfully');
        this.invoice.set(res.data);
      },
      error: (error) => {
        this.alert.error(error?.error?.message ?? 'Failed to post invoice');
      },
    });
  }

  async cancelInvoice(): Promise<void> {
    const invoice = this.invoice();
    if (!invoice) return;

    const confirmed = await this.alert.confirm(`Cancel ${invoice.invoiceNumber}?`);
    if (!confirmed) return;

    const reason: CancelledReason = 'CUSTOMER_REQUESTED';

    this.invoiceService.cancel(invoice.id, reason).subscribe({
      next: (res) => {
        this.alert.success('Invoice cancelled successfully');
        this.invoice.set(res.data);
      },
      error: (error) => {
        this.alert.error(error?.error?.message ?? 'Failed to cancel invoice');
      },
    });
  }

  printInvoice(): void {
    window.print();
  }

  getStatusClass(status: InvoiceStatus): string {
    return status.toLowerCase();
  }
}
