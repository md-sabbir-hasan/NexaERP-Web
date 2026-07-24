import { CommonModule, DecimalPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import {
  FormsModule,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';

import { AlertService } from '../../../../core/services/alert.service';
import { Account } from '../../../accounts/models/account.model';
import { AccountService } from '../../../accounts/services/account.service';
import {
  BankAccount,
  BankAccountRequest,
  BankAccountType,
  WalletProvider,
} from '../../models/bank-account.model';
import { BankAccountService } from '../../services/bank-account.service';
import { RouterLink } from '@angular/router';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-bank-account-list',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, DecimalPipe, RouterLink, HasPermissionDirective],
  templateUrl: './bank-account-list.html',
  styleUrl: './bank-account-list.scss',
})
export class BankAccountList implements OnInit {
  readonly bankAccounts = signal<BankAccount[]>([]);
  readonly coaAccounts = signal<Account[]>([]);

  readonly loading = signal(false);
  readonly submitting = signal(false);
  readonly showModal = signal(false);

  readonly accountTypes: BankAccountType[] = ['CASH', 'BANK', 'MOBILE_WALLET'];
  readonly walletProviders: WalletProvider[] = ['BKASH', 'NAGAD', 'ROCKET'];

  search = '';
  typeFilter: BankAccountType | '' = '';
  statusFilter: boolean | '' = '';

  editingAccount: BankAccount | null = null;

  readonly bankAccountForm;

  constructor(
    private bankAccountService: BankAccountService,
    private accountService: AccountService,
    private alert: AlertService,
    private fb: NonNullableFormBuilder,
  ) {
    this.bankAccountForm = this.fb.group({
      accountName: ['', [Validators.required]],
      accountType: ['BANK' as BankAccountType, [Validators.required]],
      accountNumber: [''],
      bankName: [''],
      branchName: [''],
      mobileNumber: [''],
      walletProvider: [null as WalletProvider | null],
      currency: ['BDT', [Validators.required]],
      openingBalance: [0, [Validators.min(0)]],
      coaAccountId: [null as number | null, [Validators.required]],
      notes: [''],
    });
  }

  ngOnInit(): void {
    this.loadBankAccounts();
    this.loadCoaAccounts();
  }

  loadBankAccounts(): void {
    this.loading.set(true);

    this.bankAccountService.getAll().subscribe({
      next: (res) => {
        this.bankAccounts.set(res.data);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.alert.error('Failed to load bank accounts');
      },
    });
  }

  loadCoaAccounts(): void {
    // Only ASSET-type, leaf-level (postable) ledger accounts should be
    // linkable — group/header accounts like "1000 - Asset" are excluded
    this.accountService.getByType('ASSET').subscribe({
      next: (res) => this.coaAccounts.set(res.data.filter((a) => a.isActive && !a.hasChildren)),
      error: () => this.alert.error('Failed to load ledger accounts'),
    });
  }

  readonly filteredAccounts = () => {
    let list = [...this.bankAccounts()];

    if (this.search.trim()) {
      const keyword = this.search.trim().toLowerCase();
      list = list.filter(
        (a) =>
          a.accountName.toLowerCase().includes(keyword) ||
          (a.accountNumber ?? '').toLowerCase().includes(keyword) ||
          (a.bankName ?? '').toLowerCase().includes(keyword) ||
          (a.mobileNumber ?? '').toLowerCase().includes(keyword),
      );
    }

    if (this.typeFilter) {
      list = list.filter((a) => a.accountType === this.typeFilter);
    }

    if (this.statusFilter !== '') {
      list = list.filter((a) => a.isActive === this.statusFilter);
    }

    return list;
  };

  clearFilter(): void {
    this.search = '';
    this.typeFilter = '';
    this.statusFilter = '';
  }

  get totalAccounts(): number {
    return this.bankAccounts().length;
  }

  get activeAccounts(): number {
    return this.bankAccounts().filter((a) => a.isActive).length;
  }

  get totalBalance(): number {
    return this.bankAccounts().reduce((sum, a) => sum + Number(a.currentBalance ?? 0), 0);
  }

  get mobileWalletCount(): number {
    return this.bankAccounts().filter((a) => a.accountType === 'MOBILE_WALLET').length;
  }

  openCreateModal(): void {
    this.editingAccount = null;
    this.bankAccountForm.enable();

    this.bankAccountForm.reset({
      accountName: '',
      accountType: 'BANK',
      accountNumber: '',
      bankName: '',
      branchName: '',
      mobileNumber: '',
      walletProvider: null,
      currency: 'BDT',
      openingBalance: 0,
      coaAccountId: null,
      notes: '',
    });

    this.showModal.set(true);
  }

  openEditModal(account: BankAccount): void {
    this.editingAccount = account;

    this.bankAccountForm.reset({
      accountName: account.accountName,
      accountType: account.accountType,
      accountNumber: account.accountNumber ?? '',
      bankName: account.bankName ?? '',
      branchName: account.branchName ?? '',
      mobileNumber: account.mobileNumber ?? '',
      walletProvider: account.walletProvider,
      currency: account.currency,
      openingBalance: account.openingBalance,
      coaAccountId: account.coaAccountId,
      notes: account.notes ?? '',
    });

    // Type, opening balance and COA link define the account's accounting
    // identity — locked after creation to protect journal integrity.
    this.bankAccountForm.controls.accountType.disable();
    this.bankAccountForm.controls.openingBalance.disable();
    this.bankAccountForm.controls.coaAccountId.disable();

    this.showModal.set(true);
  }

  closeModal(): void {
    this.showModal.set(false);
    this.editingAccount = null;
    this.bankAccountForm.enable();
  }

  isMobileWallet(): boolean {
    return this.bankAccountForm.controls.accountType.getRawValue() === 'MOBILE_WALLET';
  }

  isCash(): boolean {
    return this.bankAccountForm.controls.accountType.getRawValue() === 'CASH';
  }

  submitBankAccount(): void {
    if (this.bankAccountForm.invalid) {
      this.bankAccountForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);

    const raw = this.bankAccountForm.getRawValue();

    const request: BankAccountRequest = {
      accountName: raw.accountName,
      accountType: raw.accountType,
      accountNumber: raw.accountNumber || null,
      bankName: raw.bankName || null,
      branchName: raw.branchName || null,
      mobileNumber: raw.mobileNumber || null,
      walletProvider: raw.walletProvider,
      currency: raw.currency,
      openingBalance: raw.openingBalance,
      coaAccountId: raw.coaAccountId,
      notes: raw.notes || null,
    };

    const apiCall = this.editingAccount
      ? this.bankAccountService.update(this.editingAccount.id, request)
      : this.bankAccountService.create(request);

    apiCall.subscribe({
      next: () => {
        this.submitting.set(false);
        this.showModal.set(false);
        this.alert.success(
          this.editingAccount
            ? 'Bank account updated successfully'
            : 'Bank account created successfully',
        );
        this.editingAccount = null;
        this.bankAccountForm.enable();
        this.loadBankAccounts();
      },
      error: (error) => {
        this.submitting.set(false);
        this.alert.error(error?.error?.message ?? 'Failed to save bank account');
      },
    });
  }

  async deactivateAccount(account: BankAccount): Promise<void> {
    const confirmed = await this.alert.confirm(`Deactivate ${account.accountName}?`);
    if (!confirmed) return;

    this.bankAccountService.deactivate(account.id).subscribe({
      next: () => {
        this.alert.success('Bank account deactivated successfully');
        this.loadBankAccounts();
      },
      error: (error) =>
        this.alert.error(error?.error?.message ?? 'Failed to deactivate bank account'),
    });
  }

  async activateAccount(account: BankAccount): Promise<void> {
    const confirmed = await this.alert.confirm(`Activate ${account.accountName}?`);
    if (!confirmed) return;

    this.bankAccountService.activate(account.id).subscribe({
      next: () => {
        this.alert.success('Bank account activated successfully');
        this.loadBankAccounts();
      },
      error: (error) =>
        this.alert.error(error?.error?.message ?? 'Failed to activate bank account'),
    });
  }

  getTypeClass(type: BankAccountType): string {
    return type.toLowerCase().replace('_', '-');
  }

  getTypeIcon(type: BankAccountType): string {
    switch (type) {
      case 'CASH':
        return 'bi-cash-stack';
      case 'MOBILE_WALLET':
        return 'bi-phone';
      default:
        return 'bi-bank2';
    }
  }

  getWalletLabel(provider: WalletProvider | null): string {
    switch (provider) {
      case 'BKASH':
        return 'bKash';
      case 'NAGAD':
        return 'Nagad';
      case 'ROCKET':
        return 'Rocket';
      default:
        return '';
    }
  }
}
