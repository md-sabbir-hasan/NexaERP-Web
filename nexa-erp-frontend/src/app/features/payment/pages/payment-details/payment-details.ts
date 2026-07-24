import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { Component, OnInit, signal, computed } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AlertService } from '../../../../core/services/alert.service';
import { PaymentResponse } from '../../models/payment.model';
import { PaymentService } from '../../services/payment.service';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-payment-details',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    DecimalPipe,
    DatePipe,
    HasPermissionDirective
  ],
  templateUrl: './payment-details.html',
  styleUrl: './payment-details.scss'
})
export class PaymentDetails implements OnInit {

  readonly loading = signal(true);

  readonly payment = signal<PaymentResponse | null>(null);

  readonly canPost = computed(() =>
    this.payment()?.status === 'DRAFT'
  );

  readonly canCancel = computed(() =>
    this.payment()?.status === 'POSTED'
  );

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private paymentService: PaymentService,
    private alert: AlertService
  ) {}

  ngOnInit(): void {

    const id = Number(
      this.route.snapshot.paramMap.get('id')
    );

    this.loadPayment(id);
  }

  loadPayment(id: number): void {

    this.loading.set(true);

    this.paymentService.getById(id).subscribe({

      next: res => {

        this.payment.set(res.data);

        this.loading.set(false);

      },

      error: () => {

        this.loading.set(false);

        this.alert.error('Unable to load payment.');

        this.router.navigate(['/payment']);

      }

    });

  }

  postPayment(): void {

    const payment = this.payment();

    if (!payment) return;

    this.paymentService.post(payment.id).subscribe({

      next: () => {

        this.alert.success('Payment posted successfully.');

        this.loadPayment(payment.id);

      }

    });

  }

  cancelPayment(): void {

    const payment = this.payment();

    if (!payment) return;

    this.paymentService.cancel(payment.id).subscribe({

      next: () => {

        this.alert.success('Payment cancelled.');

        this.loadPayment(payment.id);

      }

    });

  }

  downloadReceipt(): void {

    const payment = this.payment();

    if (!payment) return;

    this.paymentService.downloadReceipt(payment.id)
      .subscribe(blob => {

        const url = window.URL.createObjectURL(blob);

        const a = document.createElement('a');

        a.href = url;

        a.download = `${payment.paymentNumber}.pdf`;

        a.click();

        window.URL.revokeObjectURL(url);

      });

  }

  getStatusClass(status: string): string {

    switch (status) {

      case 'POSTED':
        return 'status-posted';

      case 'CANCELLED':
        return 'status-cancelled';

      default:
        return 'status-draft';

    }

  }

}