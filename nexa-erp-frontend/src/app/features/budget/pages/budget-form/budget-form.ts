import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { AlertService } from '../../../../core/services/alert.service';
import { FiscalYear } from '../../../fiscal-year/models/fiscal-year.model';
import { FiscalYearService } from '../../../fiscal-year/services/fiscal-year.service';
import { BudgetCreateRequest } from '../../models/budget.model';
import { BudgetService } from '../../services/budget.service';

@Component({
  selector: 'app-budget-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './budget-form.html',
  styleUrl: './budget-form.scss',
})
export class BudgetForm implements OnInit {
  readonly submitting = signal(false);
  readonly fiscalYears = signal<FiscalYear[]>([]);

  readonly form: FormGroup;

  constructor(
    private readonly fb: FormBuilder,
    private readonly budgetService: BudgetService,
    private readonly fiscalYearService: FiscalYearService,
    private readonly router: Router,
    private readonly alert: AlertService,
  ) {
    this.form = this.fb.group({
      fiscalYearId: [null, [Validators.required]],
      name: ['', [Validators.required]],
      description: [''],
    });
  }

  ngOnInit(): void {
    this.loadFiscalYears();
  }

  loadFiscalYears(): void {
    this.fiscalYearService.getAll().subscribe({
      next: (res) => this.fiscalYears.set(res.data.filter((fy) => fy.status !== 'CLOSED')),
      error: () => this.alert.error('Failed to load fiscal years'),
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.alert.error('Please complete required fields');
      return;
    }

    const raw = this.form.getRawValue();

    const request: BudgetCreateRequest = {
      fiscalYearId: Number(raw.fiscalYearId),
      name: raw.name.trim(),
      description: raw.description?.trim() || undefined,
    };

    this.submitting.set(true);

    this.budgetService
      .create(request)
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (response) => {
          this.alert.success('Budget created — now add lines to it');
          this.router.navigate(['/budget', response.data.id]);
        },
        error: (error) => {
          this.alert.error(error?.error?.message ?? 'Failed to create budget');
        },
      });
  }
}