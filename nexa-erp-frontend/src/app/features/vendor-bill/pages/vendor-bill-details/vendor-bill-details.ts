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

@Component({
  selector: 'app-vendor-bill-details',
  standalone: true,
  imports: [CommonModule, RouterLink, DatePipe, DecimalPipe, HasPermissionDirective],
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

  async postBill(): Promise<void> {
    const bill = this.bill();
    if (!bill) return;

    const confirmed = await this.alert.confirm(`Post ${bill.billNumber}?`);
    if (!confirmed) return;

    this.vendorBillService.post(bill.id).subscribe({
      next: (res) => {
        this.alert.success('Vendor bill posted successfully');
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
}