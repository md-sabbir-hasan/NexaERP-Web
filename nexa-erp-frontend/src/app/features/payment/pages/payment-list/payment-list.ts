import { CommonModule } from '@angular/common';
import { Component, computed, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AlertService } from '../../../../core/services/alert.service';
import {
  PaymentResponse,
  PaymentStatus,
  PaymentType,
} from '../../models/payment.model';
import { PaymentService } from '../../services/payment.service';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-payment-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    HasPermissionDirective
  ],
  templateUrl: './payment-list.html',
  styleUrl: './payment-list.scss',
})
export class PaymentList implements OnInit {

  readonly loading = signal(false);

  readonly payments = signal<PaymentResponse[]>([]);

  readonly search = signal('');

  readonly status = signal('');

  readonly paymentType = signal('');

  readonly statuses: PaymentStatus[] = [
    'DRAFT',
    'POSTED',
    'CANCELLED',
  ];

  readonly paymentTypes: PaymentType[] = [
    'RECEIPT',
    'PAYMENT',
  ];

  readonly filteredPayments = computed(() => {

    let list = [...this.payments()];

    if (this.search()) {
  const keyword = this.search().toLowerCase();

  list = list.filter(payment =>
    payment.paymentNumber.toLowerCase().includes(keyword) ||
    payment.partyName.toLowerCase().includes(keyword) ||
    payment.paymentType.toLowerCase().includes(keyword) ||
    payment.paymentMethod.toLowerCase().includes(keyword) ||
    (payment.transactionRef ?? '').toLowerCase().includes(keyword)
  );
}

    if (this.status()) {
      list = list.filter(p => p.status === this.status());
    }

    if (this.paymentType()) {
      list = list.filter(p => p.paymentType === this.paymentType());
    }

    return list;
  });

  constructor(
    private paymentService: PaymentService,
    private alert: AlertService,
  ) { }

  ngOnInit(): void {
    this.loadPayments();
  }

  loadPayments(): void {

    this.loading.set(true);

    this.paymentService.getAll().subscribe({

      next: (res) => {
  const statusOrder: Record<PaymentStatus, number> = {
    DRAFT: 1,
    POSTED: 2,
    CANCELLED: 3,
  };

  this.payments.set(
    res.data.sort((a, b) => statusOrder[a.status] - statusOrder[b.status])
  );

  this.loading.set(false);
},

      error: () => {
        this.loading.set(false);
        this.alert.error('Failed to load payments');
      },

    });

  }

  clearFilter(): void {
    this.search.set('');
    this.status.set('');
    this.paymentType.set('');
  }

  draftCount(): number {
    return this.payments().filter(x => x.status === 'DRAFT').length;
  }

  postedCount(): number {
    return this.payments().filter(x => x.status === 'POSTED').length;
  }

  cancelledCount(): number {
    return this.payments().filter(x => x.status === 'CANCELLED').length;
  }

  totalAmount(): number {
    return this.payments()
      .reduce((sum, p) => sum + p.amount, 0);
  }

  receiptTotal(): number {
  return this.payments()
    .filter(x => x.paymentType === 'RECEIPT')
    .reduce((sum, x) => sum + Number(x.amount ?? 0), 0);
}

paymentTotal(): number {
  return this.payments()
    .filter(x => x.paymentType === 'PAYMENT')
    .reduce((sum, x) => sum + Number(x.amount ?? 0), 0);
}

getMethodLabel(method: string): string {
  switch (method) {
    case 'BANK_TRANSFER':
      return 'Bank Transfer';
    case 'BKASH':
      return 'bKash';
    case 'NAGAD':
      return 'Nagad';
    default:
      return method.replace('_', ' ');
  }
}

  async postPayment(payment: PaymentResponse): Promise<void> {

    const confirmed =
      await this.alert.confirm(
        `Post ${payment.paymentNumber}?`
      );

    if (!confirmed) return;

    this.paymentService.post(payment.id).subscribe({

      next: () => {
        this.alert.success('Payment posted successfully');
        this.loadPayments();
      },

      error: (err) => {
        this.alert.error(
          err?.error?.message ??
          'Failed to post payment'
        );
      }

    });

  }

  async cancelPayment(payment: PaymentResponse): Promise<void> {

    const confirmed =
      await this.alert.confirm(
        `Cancel ${payment.paymentNumber}?`
      );

    if (!confirmed) return;

    this.paymentService.cancel(payment.id).subscribe({

      next: () => {
        this.alert.success('Payment cancelled successfully');
        this.loadPayments();
      },

      error: (err) => {
        this.alert.error(
          err?.error?.message ??
          'Failed to cancel payment'
        );
      }

    });

  }

  getStatusClass(status: PaymentStatus): string {
    return status.toLowerCase();
  }

  downloadReceipt(payment: PaymentResponse): void {
  if (payment.status !== 'POSTED') {
    this.alert.warning('Receipt PDF is available only after posting');
    return;
  }

  this.paymentService.downloadReceipt(payment.id).subscribe({
    next: (blob) => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');

      a.href = url;
      a.download = `${payment.paymentNumber}.pdf`;
      a.click();

      window.URL.revokeObjectURL(url);
    },
    error: () => {
      this.alert.error('Failed to download receipt');
    },
  });
}

}