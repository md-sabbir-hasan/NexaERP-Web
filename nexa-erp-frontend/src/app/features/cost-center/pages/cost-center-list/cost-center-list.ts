import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AlertService } from '../../../../core/services/alert.service';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';
import { CostCenter, CostCenterRequest } from '../../models/cost-center.model';
import { CostCenterService } from '../../services/cost-center.service';

@Component({
  selector: 'app-cost-center-list',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, HasPermissionDirective],
  templateUrl: './cost-center-list.html',
  styleUrl: './cost-center-list.scss',
})
export class CostCenterList implements OnInit {
  readonly costCenters = signal<CostCenter[]>([]);
  readonly loading = signal(false);
  readonly submitting = signal(false);
  readonly showModal = signal(false);
  search = '';
  active: boolean | '' = '';
  editing: CostCenter | null = null;

  readonly form;

  constructor(
    private readonly service: CostCenterService,
    private readonly alert: AlertService,
    private readonly fb: NonNullableFormBuilder,
  ) {
    this.form = this.fb.group({
      code: ['', [Validators.required, Validators.maxLength(30), Validators.pattern(/^[A-Za-z0-9_-]+$/)]],
      name: ['', [Validators.required, Validators.maxLength(150)]],
      description: ['', [Validators.maxLength(500)]],
    });
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.service.search(this.search, this.active).subscribe({
      next: (response) => {
        this.costCenters.set(response.data);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.alert.error('Failed to load cost centers');
      },
    });
  }

  clearFilters(): void {
    this.search = '';
    this.active = '';
    this.load();
  }

  openCreate(): void {
    this.editing = null;
    this.form.controls.code.enable();
    this.form.reset({ code: '', name: '', description: '' });
    this.showModal.set(true);
  }

  openEdit(costCenter: CostCenter): void {
    this.editing = costCenter;
    this.form.reset({
      code: costCenter.code,
      name: costCenter.name,
      description: costCenter.description ?? '',
    });
    this.form.controls.code.disable();
    this.showModal.set(true);
  }

  closeModal(): void {
    this.showModal.set(false);
    this.editing = null;
    this.form.controls.code.enable();
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    const request: CostCenterRequest = {
      code: raw.code.trim().toUpperCase(),
      name: raw.name.trim(),
      description: raw.description.trim() || undefined,
    };
    this.submitting.set(true);
    const call = this.editing
      ? this.service.update(this.editing.id, request)
      : this.service.create(request);
    call.subscribe({
      next: () => {
        this.submitting.set(false);
        this.alert.success(this.editing ? 'Cost center updated' : 'Cost center created');
        this.closeModal();
        this.load();
      },
      error: (error) => {
        this.submitting.set(false);
        this.alert.error(error?.error?.message ?? 'Failed to save cost center');
      },
    });
  }

  async setActive(costCenter: CostCenter, active: boolean): Promise<void> {
    const action = active ? 'activate' : 'deactivate';
    if (!await this.alert.confirm(`${action[0].toUpperCase()}${action.slice(1)} ${costCenter.code}?`)) return;
    const call = active ? this.service.activate(costCenter.id) : this.service.deactivate(costCenter.id);
    call.subscribe({
      next: () => {
        this.alert.success(`Cost center ${active ? 'activated' : 'deactivated'}`);
        this.load();
      },
      error: (error) => this.alert.error(error?.error?.message ?? `Failed to ${action} cost center`),
    });
  }
}
