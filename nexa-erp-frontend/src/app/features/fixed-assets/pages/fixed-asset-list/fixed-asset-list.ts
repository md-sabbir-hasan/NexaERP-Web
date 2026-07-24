import { CommonModule, DecimalPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import {
  FormsModule,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';

import { AlertService } from '../../../../core/services/alert.service';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

import { Account } from '../../../accounts/models/account.model';
import { AccountService } from '../../../accounts/services/account.service';

import { DepreciationMethod, FixedAsset, FixedAssetRequest } from '../../models/fixed-asset.model';

import { FixedAssetService } from '../../services/fixed-asset.service';

@Component({
  selector: 'app-fixed-asset-list',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, DecimalPipe, HasPermissionDirective],
  templateUrl: './fixed-asset-list.html',
  styleUrl: './fixed-asset-list.scss',
})
export class FixedAssetList implements OnInit {
  readonly assets = signal<FixedAsset[]>([]);
  readonly accounts = signal<Account[]>([]);

  readonly loading = signal(false);
  readonly submitting = signal(false);

  readonly showRegisterModal = signal(false);
  readonly showRunAllModal = signal(false);

  readonly runAllDate = signal(this.today());

  readonly registerForm;

  constructor(
    private fixedAssetService: FixedAssetService,
    private accountService: AccountService,
    private alert: AlertService,
    private router: Router,
    private fb: NonNullableFormBuilder,
  ) {
    this.registerForm = this.fb.group({
      name: ['', [Validators.required]],
      description: [''],

      assetAccountId: [null as number | null, [Validators.required]],

      depreciationExpenseAccountId: [null as number | null, [Validators.required]],

      accumulatedDepreciationAccountId: [null as number | null, [Validators.required]],

      paymentSourceAccountId: [null as number | null, [Validators.required]],

      purchaseDate: [this.today(), [Validators.required]],

      purchaseCost: [0, [Validators.required, Validators.min(0.01)]],

      salvageValue: [0, [Validators.required, Validators.min(0)]],

      usefulLifeYears: [5, [Validators.required, Validators.min(1)]],

      depreciationMethod: ['STRAIGHT_LINE' as DepreciationMethod, [Validators.required]],

      reducingBalanceRate: [null as number | null],
    });
  }

  ngOnInit(): void {
    this.loadAssets();
    this.loadAccounts();
  }

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }

  loadAssets(): void {
    this.loading.set(true);

    this.fixedAssetService.getAll().subscribe({
      next: (res) => {
        this.assets.set(res.data ?? []);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.alert.error('Failed to load fixed assets');
      },
    });
  }

  loadAccounts(): void {
    this.accountService.getAll().subscribe({
      next: (res) => {
        const postingAccounts = (res.data ?? []).filter(
          (account) => account.isActive && !account.hasChildren,
        );

        this.accounts.set(postingAccounts);
      },
      error: () => {
        this.alert.error('Failed to load accounts');
      },
    });
  }

  /*
   * Fixed Asset Cost Account
   * যেমন Furniture, Computer, Vehicle
   */
  get assetAccounts(): Account[] {
    return this.accounts()
      .filter((account) => account.type === 'ASSET')
      .sort((a, b) => a.code.localeCompare(b.code));
  }

  /*
   * Depreciation Expense Account
   */
  get depreciationExpenseAccounts(): Account[] {
    return this.accounts()
      .filter((account) => account.type === 'EXPENSE')
      .sort((a, b) => a.code.localeCompare(b.code));
  }

  /*
   * Accumulated Depreciation account সাধারণত ASSET type।
   * নামের মধ্যে accumulated এবং depreciation থাকলে শুধু সেগুলো দেখাবে।
   *
   * যদি matching account না পাওয়া যায়,
   * fallback হিসেবে সব ASSET account দেখাবে।
   */
  get accumulatedDepreciationAccounts(): Account[] {
    const matched = this.accounts()
      .filter((account) => {
        if (account.type !== 'ASSET') {
          return false;
        }

        const text = `${account.code} ${account.name}`.toLowerCase();

        return text.includes('accumulated') && text.includes('depreciation');
      })
      .sort((a, b) => a.code.localeCompare(b.code));

    return matched.length > 0 ? matched : this.assetAccounts;
  }

  /*
   * Payment Source:
   * ASSET = Cash/Bank
   * LIABILITY = Accounts Payable
   */
  get paymentSourceAccounts(): Account[] {
    return this.accounts()
      .filter((account) => account.type === 'ASSET' || account.type === 'LIABILITY')
      .sort((a, b) => a.code.localeCompare(b.code));
  }

  get isReducingBalance(): boolean {
    return this.registerForm.controls.depreciationMethod.value === 'REDUCING_BALANCE';
  }

  get totals() {
    const list = this.assets();

    return {
      cost: list.reduce((sum, asset) => sum + Number(asset.purchaseCost ?? 0), 0),

      accumulatedDepreciation: list.reduce(
        (sum, asset) => sum + Number(asset.accumulatedDepreciation ?? 0),
        0,
      ),

      bookValue: list.reduce((sum, asset) => sum + Number(asset.bookValue ?? 0), 0),
    };
  }

  openRegisterModal(): void {
    this.registerForm.reset({
      name: '',
      description: '',

      assetAccountId: null,
      depreciationExpenseAccountId: null,
      accumulatedDepreciationAccountId: null,
      paymentSourceAccountId: null,

      purchaseDate: this.today(),
      purchaseCost: 0,
      salvageValue: 0,
      usefulLifeYears: 5,

      depreciationMethod: 'STRAIGHT_LINE',
      reducingBalanceRate: null,
    });

    this.showRegisterModal.set(true);
  }

  closeRegisterModal(): void {
    if (this.submitting()) {
      return;
    }

    this.showRegisterModal.set(false);
  }

  submitRegister(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      this.alert.warning('Please complete all required fields');
      return;
    }

    const raw = this.registerForm.getRawValue();

    if (raw.salvageValue >= raw.purchaseCost) {
      this.alert.warning('Salvage value must be less than purchase cost');
      return;
    }

    if (
      raw.depreciationMethod === 'REDUCING_BALANCE' &&
      (raw.reducingBalanceRate === null ||
        raw.reducingBalanceRate <= 0 ||
        raw.reducingBalanceRate > 100)
    ) {
      this.alert.warning('Reducing balance rate must be between 0 and 100');
      return;
    }

    if (raw.assetAccountId === raw.paymentSourceAccountId) {
      this.alert.warning('Asset account and payment source account cannot be the same');
      return;
    }

    const request: FixedAssetRequest = {
      name: raw.name.trim(),

      description: raw.description.trim() || null,

      assetAccountId: raw.assetAccountId!,

      depreciationExpenseAccountId: raw.depreciationExpenseAccountId!,

      accumulatedDepreciationAccountId: raw.accumulatedDepreciationAccountId!,

      paymentSourceAccountId: raw.paymentSourceAccountId!,

      purchaseDate: raw.purchaseDate,

      purchaseCost: Number(raw.purchaseCost),

      salvageValue: Number(raw.salvageValue),

      usefulLifeYears: Number(raw.usefulLifeYears),

      depreciationMethod: raw.depreciationMethod,

      reducingBalanceRate:
        raw.depreciationMethod === 'REDUCING_BALANCE' ? Number(raw.reducingBalanceRate) : null,
    };

    this.submitting.set(true);

    this.fixedAssetService.create(request).subscribe({
      next: () => {
        this.submitting.set(false);
        this.showRegisterModal.set(false);

        this.alert.success('Fixed asset registered successfully');

        this.loadAssets();
      },

      error: (error) => {
        this.submitting.set(false);

        this.alert.error(error?.error?.message ?? 'Failed to register asset');
      },
    });
  }

  openRunAllModal(): void {
    this.runAllDate.set(this.today());
    this.showRunAllModal.set(true);
  }

  closeRunAllModal(): void {
    if (this.submitting()) {
      return;
    }

    this.showRunAllModal.set(false);
  }

  submitRunAll(): void {
    if (!this.runAllDate()) {
      this.alert.warning('As-of date is required');
      return;
    }

    this.submitting.set(true);

    this.fixedAssetService.runDepreciationForAll(this.runAllDate()).subscribe({
      next: (res) => {
        this.submitting.set(false);
        this.showRunAllModal.set(false);

        this.alert.success(`Depreciation posted for ${res.data?.length ?? 0} asset(s)`);

        this.loadAssets();
      },

      error: (error) => {
        this.submitting.set(false);

        this.alert.error(error?.error?.message ?? 'Failed to run depreciation');
      },
    });
  }

  openAsset(asset: FixedAsset): void {
    this.router.navigate(['/fixed-assets', asset.id]);
  }
}
