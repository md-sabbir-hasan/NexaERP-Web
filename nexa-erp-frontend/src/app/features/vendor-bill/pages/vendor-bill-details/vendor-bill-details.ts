import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AlertService } from '../../../../core/services/alert.service';
import {
  VendorBill,
  VendorBillCancelledReason,
  VendorBillStatus,
} from '../../models/vendor-bill.model';
import { VendorBillService } from '../../services/vendor-bill.service';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';
import { AuditTimeline } from '../../../audit/components/audit-timeline/audit-timeline';
import { AuthService } from '../../../../core/auth/auth.service';

@Component({
  selector: 'app-vendor-bill-details',
  standalone: true,
  imports: [CommonModule, RouterLink, DatePipe, DecimalPipe, HasPermissionDirective, AuditTimeline],
  templateUrl: './vendor-bill-details.html',
  styleUrl: './vendor-bill-details.scss',
})
export class VendorBillDetails implements OnInit {
  readonly bill = signal<VendorBill | null>(null);
  readonly loading = signal(false);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private vendorBillService: VendorBillService,
    private alert: AlertService,
    private authService: AuthService,
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));

    if (!id) {
      this.router.navigate(['/vendor-bill']);
      return;
    }

    this.loadBill(id);
  }

  loadBill(id: number): void {
    this.loading.set(true);

    this.vendorBillService.getById(id).subscribe({
      next: (res) => {
        this.bill.set(res.data);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.alert.error('Failed to load vendor bill');
        this.router.navigate(['/vendor-bill']);
      },
    });
  }

  async approveBill(): Promise<void> {
    const bill = this.bill();
    if (!bill) return;

    const confirmed = await this.alert.confirm(`Approve ${bill.billNumber}?`);
    if (!confirmed) return;

    this.vendorBillService.approve(bill.id).subscribe({
      next: (res) => {
        this.alert.success('Vendor bill approved successfully');
        this.bill.set(res.data);
      },
      error: (error) => {
        this.alert.error(error?.error?.message ?? 'Failed to approve vendor bill');
      },
    });
  }

  submitForApproval(): void {
    const bill = this.bill();
    if (!bill) return;
    this.vendorBillService.submitForApproval(bill.id).subscribe({
      next: (response) => {
        this.alert.success('Vendor bill submitted for approval');
        this.bill.update((current) =>
          current
            ? {
                ...current,
                activeApprovalId: response.data.id,
                approvalStatus: response.data.status,
              }
            : current,
        );
      },
      error: (error) => this.alert.error(error?.error?.message ?? 'Failed to submit vendor bill'),
    });
  }

  isCreator(bill: VendorBill): boolean {
    return this.authService.currentUser()?.id === bill.createdBy;
  }

  hasActiveApproval(bill: VendorBill): boolean {
    return (
      bill.activeApprovalId !== null &&
      (bill.approvalStatus === 'PENDING' || bill.approvalStatus === 'APPROVED') &&
      !bill.approvalConsumed
    );
  }

  canSubmitApproval(): boolean {
    const permissions = this.authService.currentUser()?.permissions ?? [];
    return permissions.includes('CREATE_VENDOR_BILL') || permissions.includes('EDIT_VENDOR_BILL');
  }

  async postBill(): Promise<void> {
    const bill = this.bill();
    if (!bill) return;

    const confirmed = await this.alert.confirm(`Post ${bill.billNumber}?`);
    if (!confirmed) return;

    this.vendorBillService.post(bill.id).subscribe({
      next: (res) => {
        this.alert.success('Vendor bill posted successfully');
        for (const warning of res.data.budgetWarnings) {
          this.alert.warning(warning.message);
        }
        this.bill.set(res.data);
      },
      error: (error) => {
        this.alert.error(error?.error?.message ?? 'Failed to post vendor bill');
      },
    });
  }

  async cancelBill(): Promise<void> {
    const bill = this.bill();
    if (!bill) return;

    const confirmed = await this.alert.confirm(`Cancel ${bill.billNumber}?`);
    if (!confirmed) return;

    const reason: VendorBillCancelledReason = 'VENDOR_REQUESTED';

    this.vendorBillService.cancel(bill.id, reason).subscribe({
      next: (res) => {
        this.alert.success('Vendor bill cancelled successfully');
        this.bill.set(res.data);
      },
      error: (error) => {
        this.alert.error(error?.error?.message ?? 'Failed to cancel vendor bill');
      },
    });
  }

  printBill(): void {
    window.print();
  }

  getStatusClass(status: VendorBillStatus): string {
    return status.toLowerCase();
  }

  downloadPdf(): void {
    const bill = this.bill();
    if (!bill) return;

    this.vendorBillService.downloadPdf(bill.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);

        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = `${bill.billNumber}.pdf`;

        document.body.appendChild(anchor);
        anchor.click();
        anchor.remove();

        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.alert.error('Failed to download vendor bill PDF');
      },
    });
  }
}
