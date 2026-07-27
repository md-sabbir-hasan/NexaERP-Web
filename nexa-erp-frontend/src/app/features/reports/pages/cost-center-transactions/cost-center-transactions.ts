import { CommonModule, DecimalPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AlertService } from '../../../../core/services/alert.service';
import { CostCenterLookup } from '../../../cost-center/models/cost-center.model';
import { CostCenterService } from '../../../cost-center/services/cost-center.service';
import { CostCenterTransactionReport } from '../../models/cost-center-transaction.model';
import { ReportService } from '../../services/report.service';

@Component({
  selector: 'app-cost-center-transactions',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, DecimalPipe],
  templateUrl: './cost-center-transactions.html',
  styleUrl: './cost-center-transactions.scss',
})
export class CostCenterTransactions implements OnInit {
  readonly costCenters = signal<CostCenterLookup[]>([]);
  readonly report = signal<CostCenterTransactionReport | null>(null);
  readonly loading = signal(false);
  readonly form;

  constructor(
    private readonly fb: NonNullableFormBuilder,
    private readonly costCenterService: CostCenterService,
    private readonly reportService: ReportService,
    private readonly alert: AlertService,
  ) {
    const today = new Date();
    const first = new Date(today.getFullYear(), today.getMonth(), 1);
    this.form = this.fb.group({
      costCenterId: [null as number | null, Validators.required],
      fromDate: [first.toISOString().substring(0, 10), Validators.required],
      toDate: [today.toISOString().substring(0, 10), Validators.required],
    });
  }

  ngOnInit(): void {
    this.costCenterService.lookup().subscribe({
      next: (response) => this.costCenters.set(response.data),
      error: () => this.alert.error('Failed to load cost centers'),
    });
  }

  load(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    this.loading.set(true);
    this.reportService.getCostCenterTransactions(Number(raw.costCenterId), raw.fromDate, raw.toDate)
      .subscribe({
        next: (response) => {
          this.report.set(response.data);
          this.loading.set(false);
        },
        error: (error) => {
          this.loading.set(false);
          this.alert.error(error?.error?.message ?? 'Failed to load report');
        },
      });
  }
}
