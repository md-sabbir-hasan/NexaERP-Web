import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import {
  FormBuilder,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable, forkJoin } from 'rxjs';

import { AlertService } from '../../../../core/services/alert.service';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';
import { FiscalYear } from '../../../fiscal-year/models/fiscal-year.model';
import { FiscalYearService } from '../../../fiscal-year/services/fiscal-year.service';
import {
  AccountingPeriod,
  AccountingPeriodRequest,
} from '../../models/accounting-period.model';
import { AccountingPeriodService } from '../../services/accounting-period.service';

@Component({
  selector: 'app-accounting-period-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterLink,
    HasPermissionDirective,
  ],
  templateUrl: './accounting-period-list.html',
  styleUrl: './accounting-period-list.scss',
})
export class AccountingPeriodList implements OnInit {

  private readonly fb = inject(FormBuilder);
  private readonly accountingPeriodService = inject(AccountingPeriodService);
  private readonly fiscalYearService = inject(FiscalYearService);
  private readonly alertService = inject(AlertService);

  readonly periods = signal<AccountingPeriod[]>([]);
  readonly fiscalYears = signal<FiscalYear[]>([]);
  readonly selectedFiscalYearId = signal<number | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly actionId = signal<number | null>(null);
  readonly formOpen = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly search = signal('');

  readonly form = this.fb.nonNullable.group({
    fiscalYearId: [0, [Validators.required, Validators.min(1)]],
    name: ['', [Validators.required, Validators.maxLength(100)]],
    periodNumber: [
      1,
      [Validators.required, Validators.min(1), Validators.max(99)],
    ],
    startDate: ['', Validators.required],
    endDate: ['', Validators.required],
    remarks: ['', Validators.maxLength(500)],
  });

  readonly totalCount = computed(() => this.periods().length);

  readonly openCount = computed(
    () =>
      this.periods().filter((period) => period.status === 'OPEN').length,
  );

  readonly closedCount = computed(
    () =>
      this.periods().filter((period) => period.status === 'CLOSED').length,
  );

  readonly currentCount = computed(
    () => this.periods().filter((period) => period.current).length,
  );

  readonly filteredPeriods = computed(() => {
    const query = this.search().trim().toLowerCase();

    if (!query) {
      return this.periods();
    }

    return this.periods().filter((period) =>
      [
        period.name,
        period.fiscalYearName,
        period.status,
        period.startDate,
        period.endDate,
      ]
        .join(' ')
        .toLowerCase()
        .includes(query),
    );
  });

  ngOnInit(): void {
    this.initialLoad();
  }

  initialLoad(): void {
    this.loading.set(true);

    forkJoin({
      years: this.fiscalYearService.getAll(),
      periods: this.accountingPeriodService.getAll(),
    }).subscribe({
      next: ({ years, periods }) => {
        const fiscalYears = years.data ?? [];

        this.fiscalYears.set(fiscalYears);
        this.periods.set(periods.data ?? []);

        const activeFiscalYear = fiscalYears.find(
          (year) => year.status === 'ACTIVE',
        );

        if (activeFiscalYear) {
          this.selectedFiscalYearId.set(activeFiscalYear.id);
          this.loadPeriods();
        } else {
          this.loading.set(false);
        }
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }

  loadPeriods(): void {
    this.loading.set(true);

    this.accountingPeriodService
      .getAll(this.selectedFiscalYearId())
      .subscribe({
        next: (response) => {
          this.periods.set(response.data ?? []);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
        },
      });
  }

  onFiscalYearChange(value: string): void {
    const fiscalYearId = value ? Number(value) : null;

    this.selectedFiscalYearId.set(fiscalYearId);
    this.loadPeriods();
  }

  openCreate(): void {
    const fiscalYearId =
      this.selectedFiscalYearId() ?? this.fiscalYears()[0]?.id ?? 0;

    this.editingId.set(null);

    this.form.reset({
      fiscalYearId,
      name: '',
      periodNumber: 1,
      startDate: '',
      endDate: '',
      remarks: '',
    });

    this.formOpen.set(true);
  }

  openEdit(period: AccountingPeriod): void {
    this.editingId.set(period.id);

    this.form.reset({
      fiscalYearId: period.fiscalYearId,
      name: period.name,
      periodNumber: period.periodNumber,
      startDate: period.startDate,
      endDate: period.endDate,
      remarks: period.remarks ?? '',
    });

    this.formOpen.set(true);
  }

  closeForm(): void {
    if (this.saving()) {
      return;
    }

    this.formOpen.set(false);
    this.editingId.set(null);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();

    if (value.endDate < value.startDate) {
      this.alertService.warning(
        'End date cannot be before start date',
      );
      return;
    }

    const request: AccountingPeriodRequest = {
      fiscalYearId: value.fiscalYearId,
      name: value.name.trim(),
      periodNumber: value.periodNumber,
      startDate: value.startDate,
      endDate: value.endDate,
      remarks: value.remarks.trim() || null,
    };

    this.saving.set(true);

    const editingId = this.editingId();

    const request$ =
      editingId === null
        ? this.accountingPeriodService.create(request)
        : this.accountingPeriodService.update(editingId, request);

    request$.subscribe({
      next: (response) => {
        this.saving.set(false);
        this.closeForm();

        this.selectedFiscalYearId.set(request.fiscalYearId);

        this.alertService.success(
          response.message ||
            (editingId === null
              ? 'Accounting period created'
              : 'Accounting period updated'),
        );

        this.loadPeriods();
      },
      error: (error) => {
        this.saving.set(false);

        this.alertService.error(
          error?.error?.message ??
            'Could not save accounting period',
        );
      },
    });
  }

  async generate(): Promise<void> {
    const fiscalYearId = this.selectedFiscalYearId();

    if (!fiscalYearId) {
      this.alertService.warning('Select a fiscal year first');
      return;
    }

    const fiscalYear = this.fiscalYears().find(
      (item) => item.id === fiscalYearId,
    );

    const confirmed = await this.alertService.confirm(
      `Generate monthly periods for ${
        fiscalYear?.name ?? 'selected fiscal year'
      }?`,
    );

    if (!confirmed) {
      return;
    }

    this.runAction(
      null,
      this.accountingPeriodService.generate(fiscalYearId),
      'Accounting periods generated',
    );
  }

  async toggleStatus(period: AccountingPeriod): Promise<void> {
    const nextAction =
      period.status === 'OPEN' ? 'close' : 'open';

    const confirmed = await this.alertService.confirm(
      `${nextAction === 'close' ? 'Close' : 'Open'} ${period.name}?`,
    );

    if (!confirmed) {
      return;
    }

    const request$ =
      nextAction === 'close'
        ? this.accountingPeriodService.close(
            period.id,
            period.remarks,
          )
        : this.accountingPeriodService.open(
            period.id,
            period.remarks,
          );

    this.runAction(
      period.id,
      request$,
      `Accounting period ${
        nextAction === 'close' ? 'closed' : 'opened'
      }`,
    );
  }

  async remove(period: AccountingPeriod): Promise<void> {
    const confirmed = await this.alertService.confirm(
      `Delete ${period.name}?`,
    );

    if (!confirmed) {
      return;
    }

    this.runAction(
      period.id,
      this.accountingPeriodService.delete(period.id),
      'Accounting period deleted',
    );
  }

  statusClass(period: AccountingPeriod): string {
    if (period.current && period.status === 'OPEN') {
      return 'status-current';
    }

    if (period.future && period.status === 'OPEN') {
      return 'status-future';
    }

    return `status-${period.status.toLowerCase()}`;
  }

  statusLabel(period: AccountingPeriod): string {
    if (period.current && period.status === 'OPEN') {
      return 'CURRENT';
    }

    if (period.future && period.status === 'OPEN') {
      return 'FUTURE · OPEN';
    }

    return period.status;
  }

  private runAction(
    id: number | null,
    request$: Observable<unknown>,
    fallbackMessage: string,
  ): void {
    this.actionId.set(id);

    request$.subscribe({
      next: (response: any) => {
        this.actionId.set(null);

        this.alertService.success(
          response?.message || fallbackMessage,
        );

        this.loadPeriods();
      },
      error: (error) => {
        this.actionId.set(null);

        this.alertService.error(
          error?.error?.message ?? 'Action failed',
        );
      },
    });
  }
}