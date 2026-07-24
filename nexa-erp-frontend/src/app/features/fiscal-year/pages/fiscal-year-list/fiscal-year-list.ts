import { CommonModule } from '@angular/common';
import {
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import {
  FormBuilder,
  FormsModule,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Observable } from 'rxjs';

import { AlertService } from '../../../../core/services/alert.service';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';
import {
  FiscalYear,
  FiscalYearRequest,
} from '../../models/fiscal-year.model';
import { FiscalYearService } from '../../services/fiscal-year.service';
import { AccountingPeriodService } from '../../../accounting-period/services/accounting-period.service';

@Component({
  selector: 'app-fiscal-year-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    HasPermissionDirective,
  ],
  templateUrl: './fiscal-year-list.html',
  styleUrl: './fiscal-year-list.scss',
})
export class FiscalYearList implements OnInit {

  private readonly fb = inject(FormBuilder);
  private readonly fiscalYearService = inject(FiscalYearService);
  private readonly alertService = inject(AlertService);
  private readonly accountingPeriodService = inject(AccountingPeriodService);

  readonly fiscalYears = signal<FiscalYear[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly actionId = signal<number | null>(null);
  readonly formOpen = signal(false);
  readonly editingId = signal<number | null>(null);
  readonly search = signal('');

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    startDate: ['', Validators.required],
    endDate: ['', Validators.required],
    description: ['', Validators.maxLength(500)],
  });

  readonly totalCount = computed(() => this.fiscalYears().length);

  readonly activeCount = computed(
    () =>
      this.fiscalYears().filter(
        (year) => year.status === 'ACTIVE',
      ).length,
  );

  readonly draftCount = computed(
    () =>
      this.fiscalYears().filter(
        (year) => year.status === 'DRAFT',
      ).length,
  );

  readonly closedCount = computed(
    () =>
      this.fiscalYears().filter(
        (year) => year.status === 'CLOSED',
      ).length,
  );

  readonly filteredFiscalYears = computed(() => {
    const query = this.search().trim().toLowerCase();

    if (!query) {
      return this.fiscalYears();
    }

    return this.fiscalYears().filter((year) =>
      [
        year.name,
        year.status,
        year.startDate,
        year.endDate,
        year.description ?? '',
      ]
        .join(' ')
        .toLowerCase()
        .includes(query),
    );
  });

  ngOnInit(): void {
    this.loadFiscalYears();
  }

  loadFiscalYears(): void {
    this.loading.set(true);

    this.fiscalYearService.getAll().subscribe({
      next: (response) => {
        this.fiscalYears.set(response.data ?? []);
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);

        this.alertService.error(
          error?.error?.message ??
            'Could not load fiscal years',
        );
      },
    });
  }

  openCreate(): void {
    this.editingId.set(null);

    this.form.reset({
      name: '',
      startDate: '',
      endDate: '',
      description: '',
    });

    this.formOpen.set(true);
  }

  openEdit(fiscalYear: FiscalYear): void {
    if (fiscalYear.status === 'CLOSED') {
      this.alertService.warning(
        'Closed fiscal year cannot be edited',
      );
      return;
    }

    this.editingId.set(fiscalYear.id);

    this.form.reset({
      name: fiscalYear.name,
      startDate: fiscalYear.startDate,
      endDate: fiscalYear.endDate,
      description: fiscalYear.description ?? '',
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

    const request: FiscalYearRequest = {
      name: value.name.trim(),
      startDate: value.startDate,
      endDate: value.endDate,
      description: value.description.trim() || null,
    };

    this.saving.set(true);

    const editingId = this.editingId();

    const request$ =
      editingId === null
        ? this.fiscalYearService.create(request)
        : this.fiscalYearService.update(
            editingId,
            request,
          );

    request$.subscribe({
      next: (response) => {
        this.saving.set(false);
        this.closeForm();

        this.alertService.success(
          response.message ||
            (editingId === null
              ? 'Fiscal year created'
              : 'Fiscal year updated'),
        );

        this.loadFiscalYears();
      },
      error: (error) => {
        this.saving.set(false);

        this.alertService.error(
          error?.error?.message ??
            'Could not save fiscal year',
        );
      },
    });
  }

  async activate(fiscalYear: FiscalYear): Promise<void> {
    if (fiscalYear.status !== 'DRAFT') {
      this.alertService.warning(
        'Only draft fiscal year can be activated',
      );
      return;
    }

    const confirmed = await this.alertService.confirm(
      `Activate ${fiscalYear.name}?`,
    );

    if (!confirmed) {
      return;
    }

    this.runAction(
      fiscalYear.id,
      this.fiscalYearService.activate(fiscalYear.id),
      'Fiscal year activated',
    );
  }

  async close(fiscalYear: FiscalYear): Promise<void> {
    if (fiscalYear.status !== 'ACTIVE') {
      this.alertService.warning(
        'Only active fiscal year can be closed',
      );
      return;
    }

    const confirmed = await this.alertService.confirm(
      `Close ${fiscalYear.name}?`,
    );

    if (!confirmed) {
      return;
    }

    this.runAction(
      fiscalYear.id,
      this.fiscalYearService.close(fiscalYear.id),
      'Fiscal year closed',
    );
  }

  async remove(fiscalYear: FiscalYear): Promise<void> {
    if (fiscalYear.status !== 'DRAFT') {
      this.alertService.warning(
        'Only draft fiscal year can be deleted',
      );
      return;
    }

    const confirmed = await this.alertService.confirm(
      `Delete ${fiscalYear.name}?`,
    );

    if (!confirmed) {
      return;
    }

    this.runAction(
      fiscalYear.id,
      this.fiscalYearService.delete(fiscalYear.id),
      'Fiscal year deleted',
    );
  }

  statusClass(fiscalYear: FiscalYear): string {
    return `status-${fiscalYear.status.toLowerCase()}`;
  }

  private runAction(
    id: number,
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

        this.loadFiscalYears();
      },
      error: (error) => {
        this.actionId.set(null);

        this.alertService.error(
          error?.error?.message ?? 'Action failed',
        );
      },
    });
  }

  async generate(fiscalYear: FiscalYear): Promise<void> {
  if (fiscalYear.status === 'CLOSED') {
    this.alertService.warning(
      'Closed fiscal year-এর জন্য accounting period generate করা যাবে না',
    );
    return;
  }

  const confirmed = await this.alertService.confirm(
    `Generate monthly accounting periods for ${fiscalYear.name}?`,
  );

  if (!confirmed) {
    return;
  }

  this.runAction(
    fiscalYear.id,
    this.accountingPeriodService.generate(fiscalYear.id),
    'Accounting periods generated successfully',
  );
}
}