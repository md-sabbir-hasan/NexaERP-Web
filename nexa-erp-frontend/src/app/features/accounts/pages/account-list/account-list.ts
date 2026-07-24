import { CommonModule, DecimalPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import {
  FormsModule,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { AlertService } from '../../../../core/services/alert.service';
import { Account, AccountRequest, AccountType } from '../../models/account.model';
import { AccountService } from '../../services/account.service';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

type ViewMode = 'TREE' | 'TABLE';

@Component({
  selector: 'app-account-list',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, DecimalPipe, HasPermissionDirective],
  templateUrl: './account-list.html',
  styleUrl: './account-list.scss',
})
export class AccountList implements OnInit {
  readonly accounts = signal<Account[]>([]);
  readonly treeAccounts = signal<Account[]>([]);
  readonly selectedAccount = signal<Account | null>(null);

  readonly loading = signal(false);
  readonly submitting = signal(false);
  readonly showModal = signal(false);

  readonly accountTypes: AccountType[] = ['ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE'];

  viewMode: ViewMode = 'TREE';
  search = '';
  type: AccountType | '' = '';
  active: boolean | '' = '';

  editingAccount: Account | null = null;

  readonly accountForm;

  constructor(
    private accountService: AccountService,
    private alert: AlertService,
    private fb: NonNullableFormBuilder,
  ) {
    this.accountForm = this.fb.group({
      code: ['', [Validators.required, Validators.pattern(/^\d+$/)]],
      name: ['', [Validators.required]],
      description: [''],
      type: ['ASSET' as AccountType, [Validators.required]],
      parentId: [null as number | null],
    });
  }

  ngOnInit(): void {
    this.loadAccounts();
    this.loadTree();
  }

  loadAccounts(): void {
    this.loading.set(true);

    this.accountService.search(this.search, this.type, this.active).subscribe({
      next: (res) => {
        this.accounts.set(res.data);
        this.treeAccounts.set(this.buildTreeFromFlat(res.data));
        this.loading.set(false);

        if (res.data.length > 0) {
          this.selectedAccount.set(res.data[0]);
        } else {
          this.selectedAccount.set(null);
        }
      },
      error: () => this.loading.set(false),
    });
  }

  loadTree(): void {
    this.accountService.getTree().subscribe({
      next: (res) => this.treeAccounts.set(res.data),
    });
  }

  applyFilter(): void {
    this.loadAccounts();
  }

  clearFilter(): void {
    this.search = '';
    this.type = '';
    this.active = '';
    this.loadAccounts();
  }

  setViewMode(mode: ViewMode): void {
    this.viewMode = mode;
  }

  selectAccount(account: Account): void {
    this.selectedAccount.set(account);
  }

  openCreateModal(): void {
    this.editingAccount = null;
    this.accountForm.enable();

    this.accountForm.reset({
      code: '',
      name: '',
      description: '',
      type: 'ASSET',
      parentId: null,
    });

    this.showModal.set(true);
  }

  openEditModal(account: Account): void {
    this.editingAccount = account;

    this.accountForm.reset({
      code: account.code,
      name: account.name,
      description: account.description ?? '',
      type: account.type,
      parentId: account.parentId,
    });

    this.accountForm.controls.code.disable();
    this.accountForm.controls.type.disable();

    this.showModal.set(true);
  }

  closeModal(): void {
    this.showModal.set(false);
    this.editingAccount = null;
    this.accountForm.enable();
  }

  submitAccount(): void {
    if (this.accountForm.invalid) {
      this.accountForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);

    const raw = this.accountForm.getRawValue();

    const request: AccountRequest = {
      code: raw.code,
      name: raw.name,
      description: raw.description,
      type: raw.type,
      parentId: raw.parentId,
    };

    const apiCall = this.editingAccount
      ? this.accountService.update(this.editingAccount.id, request)
      : this.accountService.create(request);

    apiCall.subscribe({
      next: () => {
        this.submitting.set(false);
        this.showModal.set(false);
        this.alert.success(
          this.editingAccount ? 'Account updated successfully' : 'Account created successfully',
        );
        this.editingAccount = null;
        this.accountForm.enable();
        this.loadAccounts();
        this.loadTree();
      },
      error: (error) => {
        this.submitting.set(false);
        this.alert.error(error?.error?.message ?? 'Failed to save account');
      },
    });
  }

  async activateAccount(account: Account): Promise<void> {
    const confirmed = await this.alert.confirm(`Activate ${account.code} - ${account.name}?`);
    if (!confirmed) return;

    this.accountService.activate(account.id).subscribe({
      next: () => {
        this.alert.success('Account activated successfully');
        this.loadAccounts();
        this.loadTree();
      },
      error: (error) => this.alert.error(error?.error?.message ?? 'Failed to activate account'),
    });
  }

  async deactivateAccount(account: Account): Promise<void> {
    const confirmed = await this.alert.confirm(`Deactivate ${account.code} - ${account.name}?`);
    if (!confirmed) return;

    this.accountService.deactivate(account.id).subscribe({
      next: () => {
        this.alert.success('Account deactivated successfully');
        this.loadAccounts();
        this.loadTree();
      },
      error: (error) => this.alert.error(error?.error?.message ?? 'Failed to deactivate account'),
    });
  }

  getParentOptions(): Account[] {
    const editingId = this.editingAccount?.id;

    return this.accounts().filter(
      (account) =>
        account.isActive &&
        account.id !== editingId &&
        account.type === this.accountForm.controls.type.getRawValue(),
    );
  }

  get totalAccounts(): number {
    return this.accounts().length;
  }

  get activeAccounts(): number {
    return this.accounts().filter((account) => account.isActive).length;
  }

  get rootAccounts(): number {
    return this.accounts().filter((account) => account.parentId === null).length;
  }

  get totalBalance(): number {
    return this.accounts().reduce((sum, account) => sum + Number(account.currentBalance ?? 0), 0);
  }

  getNaturalBalance(type: AccountType): 'Debit' | 'Credit' {
    return type === 'ASSET' || type === 'EXPENSE' ? 'Debit' : 'Credit';
  }

  getTypeClass(type: AccountType): string {
    return type.toLowerCase();
  }

  private buildTreeFromFlat(accounts: Account[]): Account[] {
    const map = new Map<number, Account>();

    accounts.forEach((account) => {
      map.set(account.id, { ...account, children: [] });
    });

    const roots: Account[] = [];

    map.forEach((account) => {
      if (account.parentId && map.has(account.parentId)) {
        map.get(account.parentId)?.children?.push(account);
      } else {
        roots.push(account);
      }
    });

    return roots;
  }
}
