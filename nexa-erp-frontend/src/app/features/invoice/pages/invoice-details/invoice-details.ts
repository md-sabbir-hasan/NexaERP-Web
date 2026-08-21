import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AlertService } from '../../../../core/services/alert.service';
import { CancelledReason, Invoice, InvoiceStatus } from '../../models/invoice.model';
import { InvoiceService } from '../../services/invoice.service';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';
import { AuditTimeline } from '../../../audit/components/audit-timeline/audit-timeline';
import { AuthService } from '../../../../core/auth/auth.service';

@Component({
  selector: 'app-invoice-details',
  standalone: true,
  imports: [CommonModule, RouterLink, DatePipe, DecimalPipe, HasPermissionDirective, AuditTimeline],
  templateUrl: './invoice-details.html',
  styleUrl: './invoice-details.scss',
})
export class InvoiceDetails implements OnInit {
  readonly invoice = signal<Invoice | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  private invoiceId = 0;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private invoiceService: InvoiceService,
    private alert: AlertService,
    private authService: AuthService,
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.invoiceId = id;

    if (!id) {
      this.router.navigate(['/invoice']);
      return;
    }

    this.loadInvoice(id);
  }

  loadInvoice(id: number): void {
    this.loading.set(true);
    this.error.set(null);

    this.invoiceService.getById(id).subscribe({
      next: (res) => {
        this.invoice.set(res.data);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Failed to load invoice');
      },
    });
  }

  retry(): void {
    if (this.invoiceId) this.loadInvoice(this.invoiceId);
  }

  submitForApproval(): void {
    const invoice = this.invoice();
    if (!invoice) return;
    this.invoiceService.submitForApproval(invoice.id).subscribe({
      next: (response) => {
        this.alert.success('Invoice submitted for approval');
        this.invoice.update((current) =>
          current
            ? {
                ...current,
                latestApprovalId: response.data.id,
                activeApprovalId: response.data.id,
                approvalStatus: response.data.status,
                approvalConsumed: false,
              }
            : current,
        );
      },
      error: (error) => this.alert.error(error?.error?.message ?? 'Failed to submit invoice'),
    });
  }

  isCreator(invoice: Invoice): boolean {
    return this.authService.currentUser()?.id === invoice.createdBy;
  }

  canSubmitApproval(invoice: Invoice): boolean {
    const permissions = this.authService.currentUser()?.permissions ?? [];
    return (
      invoice.approvalFeatureEnabled === true &&
      invoice.status === 'DRAFT' &&
      this.isCreator(invoice) &&
      !this.hasActiveApproval(invoice) &&
      (permissions.includes('CREATE_INVOICE') || permissions.includes('EDIT_INVOICE'))
    );
  }

  hasActiveApproval(invoice: Invoice): boolean {
    return (
      invoice.activeApprovalId != null &&
      (invoice.approvalStatus === 'PENDING' || invoice.approvalStatus === 'APPROVED') &&
      !invoice.approvalConsumed
    );
  }

  canPost(invoice: Invoice): boolean {
    return (
      invoice.approvalFeatureEnabled !== true ||
      (invoice.approvalStatus === 'APPROVED' && !invoice.approvalConsumed)
    );
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

  downloadPdf(): void {
    const invoice = this.invoice();
    if (!invoice) return;

    this.invoiceService.downloadPdf(invoice.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);

        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = `${invoice.invoiceNumber}.pdf`;

        document.body.appendChild(anchor);
        anchor.click();
        anchor.remove();

        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.alert.error('Failed to download invoice PDF');
      },
    });
  }
}
