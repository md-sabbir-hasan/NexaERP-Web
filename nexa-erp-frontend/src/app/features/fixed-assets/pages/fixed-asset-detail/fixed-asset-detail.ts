import { CommonModule, DecimalPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { AlertService } from '../../../../core/services/alert.service';
import { Account } from '../../../accounts/models/account.model';
import { AccountService } from '../../../accounts/services/account.service';
import { AssetDisposalRequest, DepreciationEntry, FixedAsset } from '../../models/fixed-asset.model';
import { FixedAssetService } from '../../services/fixed-asset.service';

@Component({
  selector: 'app-fixed-asset-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, DecimalPipe, RouterLink],
  templateUrl: './fixed-asset-detail.html',
  styleUrl: './fixed-asset-detail.scss',
})
export class FixedAssetDetail implements OnInit {
  readonly asset = signal<FixedAsset | null>(null);
  readonly history = signal<DepreciationEntry[]>([]);
  readonly accounts = signal<Account[]>([]);

  readonly loading = signal(false);
  readonly submitting = signal(false);

  readonly showRunModal = signal(false);
  readonly runDate = signal(this.today());

  readonly showDisposeModal = signal(false);
  readonly disposeForm;

  private assetId!: number;

  constructor(
    private route: ActivatedRoute,
    private fixedAssetService: FixedAssetService,
    private accountService: AccountService,
    private alert: AlertService,
    private fb: NonNullableFormBuilder,
  ) {
    this.disposeForm = this.fb.group({
      disposalDate: [this.today(), [Validators.required]],
      disposalProceeds: [0, [Validators.required, Validators.min(0)]],
      proceedsAccountId: [null as number | null],
      gainLossAccountId: [null as number | null],
      notes: [''],
    });
  }

  ngOnInit(): void {
    this.assetId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadAll();
    this.accountService.getAll().subscribe({
      next: (res) => this.accounts.set(res.data.filter((a) => a.isActive && !a.hasChildren)),
    });
  }

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }

  loadAll(): void {
    this.loading.set(true);
    this.fixedAssetService.getById(this.assetId).subscribe({
      next: (res) => {
        this.asset.set(res.data);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.alert.error('Failed to load asset');
      },
    });

    this.fixedAssetService.getDepreciationHistory(this.assetId).subscribe({
      next: (res) => this.history.set(res.data),
    });
  }

  get isActive(): boolean {
    return this.asset()?.status === 'ACTIVE';
  }

  get isDisposed(): boolean {
    return this.asset()?.status === 'DISPOSED';
  }

  openRunModal(): void {
    this.runDate.set(this.today());
    this.showRunModal.set(true);
  }

  closeRunModal(): void {
    this.showRunModal.set(false);
  }

  submitRun(): void {
    this.submitting.set(true);
    this.fixedAssetService.runDepreciation(this.assetId, this.runDate()).subscribe({
      next: () => {
        this.submitting.set(false);
        this.showRunModal.set(false);
        this.alert.success('Depreciation posted');
        this.loadAll();
      },
      error: (error) => {
        this.submitting.set(false);
        this.alert.error(error?.error?.message ?? 'Failed to run depreciation');
      },
    });
  }

  openDisposeModal(): void {
    this.disposeForm.reset({
      disposalDate: this.today(),
      disposalProceeds: 0,
      proceedsAccountId: null,
      gainLossAccountId: null,
      notes: '',
    });
    this.showDisposeModal.set(true);
  }

  closeDisposeModal(): void {
    this.showDisposeModal.set(false);
  }

  get estimatedGainLoss(): number {
    const a = this.asset();
    if (!a) return 0;
    return this.disposeForm.controls.disposalProceeds.value - a.bookValue;
  }

  async submitDispose(): Promise<void> {
    if (this.disposeForm.invalid) {
      this.disposeForm.markAllAsTouched();
      return;
    }

    const confirmed = await this.alert.confirm('Dispose this asset? This cannot be undone.');
    if (!confirmed) return;

    this.submitting.set(true);
    const raw = this.disposeForm.getRawValue();

    const request: AssetDisposalRequest = {
      disposalDate: raw.disposalDate,
      disposalProceeds: raw.disposalProceeds,
      proceedsAccountId: raw.proceedsAccountId,
      gainLossAccountId: raw.gainLossAccountId,
      notes: raw.notes || null,
    };

    this.fixedAssetService.dispose(this.assetId, request).subscribe({
      next: () => {
        this.submitting.set(false);
        this.showDisposeModal.set(false);
        this.alert.success('Asset disposed');
        this.loadAll();
      },
      error: (error) => {
        this.submitting.set(false);
        this.alert.error(error?.error?.message ?? 'Failed to dispose asset');
      },
    });
  }
}
