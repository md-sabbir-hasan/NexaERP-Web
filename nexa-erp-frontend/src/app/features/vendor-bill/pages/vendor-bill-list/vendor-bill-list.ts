import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AlertService } from '../../../../core/services/alert.service';
import {
  VendorBill,
  VendorBillCancelledReason,
  VendorBillStatus,
} from '../../models/vendor-bill.model';
import { VendorBillService } from '../../services/vendor-bill.service';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-vendor-bill-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, DatePipe, DecimalPipe, HasPermissionDirective],
  templateUrl: './vendor-bill-list.html',
  styleUrl: './vendor-bill-list.scss',
})
export class VendorBillList implements OnInit {
  readonly bills = signal<VendorBill[]>([]);
  readonly loading = signal(false);

  readonly search = signal('');
  readonly status = signal<VendorBillStatus | ''>('');

  readonly statuses: VendorBillStatus[] = [
    'DRAFT',
    'APPROVED',
    'POSTED',
    'PARTIAL',
    'PAID',
    'CANCELLED',
  ];

  readonly filteredBills = computed(() => {
    const keyword = this.search().trim().toLowerCase();
    const status = this.status();

    return this.bills().filter((bill) => {
      const matchesSearch =
        !keyword ||
        bill.billNumber.toLowerCase().includes(keyword) ||
        bill.partyName.toLowerCase().includes(keyword) ||
        (bill.vendorBillRef ?? '').toLowerCase().includes(keyword);

      const matchesStatus = !status || bill.status === status;

      return matchesSearch && matchesStatus;
    });
  });

  readonly draftCount = computed(() => this.bills().filter(b => b.status === 'DRAFT').length);
  readonly approvedCount = computed(() => this.bills().filter(b => b.status === 'APPROVED').length);
  readonly postedCount = computed(() => this.bills().filter(b => b.status === 'POSTED').length);

  readonly totalDue = computed(() =>
    this.filteredBills().reduce((sum, bill) => sum + Number(bill.dueAmount ?? 0), 0),
  );

  constructor(
    private vendorBillService: VendorBillService,
    private alert: AlertService,
  ) {}

  ngOnInit(): void {
    this.loadBills();
  }

  loadBills(): void {
    this.loading.set(true);

    this.vendorBillService.getAll().subscribe({
      next: (res) => {
        this.bills.set(res.data);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.alert.error('Failed to load vendor bills');
      },
    });
  }

  clearFilter(): void {
    this.search.set('');
    this.status.set('');
  }

  async approveBill(bill: VendorBill): Promise<void> {
    const confirmed = await this.alert.confirm(`Approve ${bill.billNumber}?`);
    if (!confirmed) return;

    this.vendorBillService.approve(bill.id).subscribe({
      next: () => {
        this.alert.success('Vendor bill approved successfully');
        this.loadBills();
      },
      error: (error) => {
        this.alert.error(error?.error?.message ?? 'Failed to approve vendor bill');
      },
    });
  }

  async postBill(bill: VendorBill): Promise<void> {
    const confirmed = await this.alert.confirm(`Post ${bill.billNumber}?`);
    if (!confirmed) return;

    this.vendorBillService.post(bill.id).subscribe({
      next: () => {
        this.alert.success('Vendor bill posted successfully');
        this.loadBills();
      },
      error: (error) => {
        this.alert.error(error?.error?.message ?? 'Failed to post vendor bill');
      },
    });
  }

  async cancelBill(bill: VendorBill): Promise<void> {
    const confirmed = await this.alert.confirm(`Cancel ${bill.billNumber}?`);
    if (!confirmed) return;

    const reason: VendorBillCancelledReason = 'VENDOR_REQUESTED';

    this.vendorBillService.cancel(bill.id, reason).subscribe({
      next: () => {
        this.alert.success('Vendor bill cancelled successfully');
        this.loadBills();
      },
      error: (error) => {
        this.alert.error(error?.error?.message ?? 'Failed to cancel vendor bill');
      },
    });
  }

  getStatusClass(status: VendorBillStatus): string {
    return status.toLowerCase();
  }
}