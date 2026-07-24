import { CommonModule, DecimalPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import {
  FormsModule,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { AlertService } from '../../../../core/services/alert.service';
import { Account } from '../../../accounts/models/account.model';
import { AccountService } from '../../../accounts/services/account.service';
import { BankAccount } from '../../models/bank-account.model';
import {
  BankTransaction,
  BankTransactionRequest,
  BankTransferRequest,
  TransactionType,
} from '../../models/bank-transaction.model';
import { BankAccountService } from '../../services/bank-account.service';
import { BankTransactionService } from '../../services/bank-transaction.service';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-bank-transaction-list',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, DecimalPipe, RouterLink, HasPermissionDirective],
  templateUrl: './bank-transaction-list.html',
  styleUrl: './bank-transaction-list.scss',
})
export class BankTransactionList implements OnInit {
  readonly transactions = signal<BankTransaction[]>([]);
  readonly bankAccounts = signal<BankAccount[]>([]);
  readonly contraAccounts = signal<Account[]>([]);

  readonly loading = signal(false);
  readonly submitting = signal(false);
  readonly showModal = signal(false);
  readonly showTransferModal = signal(false);

  readonly transactionTypes: TransactionType[] = ['CREDIT', 'DEBIT'];

  search = '';
  accountFilter: number | '' = '';
  typeFilter: TransactionType | '' = '';

  readonly transactionForm;
  readonly transferForm;

  constructor(
    private bankTransactionService: BankTransactionService,
    private bankAccountService: BankAccountService,
    private accountService: AccountService,
    private alert: AlertService,
    private route: ActivatedRoute,
    private fb: NonNullableFormBuilder,
  ) {
    this.transactionForm = this.fb.group({
      bankAccountId: [null as number | null, [Validators.required]],
      transactionType: ['CREDIT' as TransactionType, [Validators.required]],
      transactionDate: [this.today(), [Validators.required]],
      amount: [0, [Validators.required, Validators.min(0.01)]],
      contraAccountId: [null as number | null, [Validators.required]],
      referenceNumber: [''],
      description: [''],
    });

    this.transferForm = this.fb.group({
      fromBankAccountId: [null as number | null, [Validators.required]],
      toBankAccountId: [null as number | null, [Validators.required]],
      transactionDate: [this.today(), [Validators.required]],
      amount: [0, [Validators.required, Validators.min(0.01)]],
      referenceNumber: [''],
      description: [''],
    });
  }

  ngOnInit(): void {
    const preselectedAccountId = Number(this.route.snapshot.queryParamMap.get('bankAccountId'));
    if (preselectedAccountId) {
      this.accountFilter = preselectedAccountId;
    }

    this.loadBankAccounts();
    this.loadContraAccounts();
    this.loadTransactions();
  }

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }

  loadTransactions(): void {
    this.loading.set(true);

    this.bankTransactionService.getAll().subscribe({
      next: (res) => {
        this.transactions.set(res.data);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.alert.error('Failed to load bank transactions');
      },
    });
  }

  loadBankAccounts(): void {
    this.bankAccountService.getAll().subscribe({
      next: (res) => this.bankAccounts.set(res.data.filter((a) => a.isActive)),
    });
  }

  loadContraAccounts(): void {
    // Contra side of a bank transaction can be any leaf-level ledger account
    // (expense, revenue, another asset, liability, etc.)
    this.accountService.getAll().subscribe({
      next: (res) => this.contraAccounts.set(res.data.filter((a) => a.isActive && !a.hasChildren)),
      error: () => this.alert.error('Failed to load ledger accounts'),
    });
  }

  readonly filteredTransactions = () => {
    let list = [...this.transactions()];

    if (this.accountFilter !== '') {
      list = list.filter((t) => t.bankAccountId === this.accountFilter);
    }

    if (this.typeFilter) {
      list = list.filter((t) => t.transactionType === this.typeFilter);
    }

    if (this.search.trim()) {
      const keyword = this.search.trim().toLowerCase();
      list = list.filter(
        (t) =>
          t.transactionNumber.toLowerCase().includes(keyword) ||
          (t.referenceNumber ?? '').toLowerCase().includes(keyword) ||
          (t.description ?? '').toLowerCase().includes(keyword),
      );
    }

    return list.sort((a, b) => (a.transactionDate < b.transactionDate ? 1 : -1));
  };

  clearFilter(): void {
    this.search = '';
    this.accountFilter = '';
    this.typeFilter = '';
  }

  get totalCredit(): number {
    return this.transactions()
      .filter((t) => t.transactionType === 'CREDIT' && !t.voided)
      .reduce((sum, t) => sum + Number(t.amount), 0);
  }

  get totalDebit(): number {
    return this.transactions()
      .filter((t) => t.transactionType === 'DEBIT' && !t.voided)
      .reduce((sum, t) => sum + Number(t.amount), 0);
  }

  get reconciledCount(): number {
    return this.transactions().filter((t) => t.reconciled).length;
  }

  openCreateModal(): void {
    this.transactionForm.reset({
      bankAccountId: this.accountFilter || null,
      transactionType: 'CREDIT',
      transactionDate: this.today(),
      amount: 0,
      contraAccountId: null,
      referenceNumber: '',
      description: '',
    });

    this.showModal.set(true);
  }

  closeModal(): void {
    this.showModal.set(false);
  }

  submitTransaction(): void {
    if (this.transactionForm.invalid) {
      this.transactionForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);

    const raw = this.transactionForm.getRawValue();

    const request: BankTransactionRequest = {
      bankAccountId: raw.bankAccountId!,
      transactionType: raw.transactionType,
      transactionDate: raw.transactionDate,
      amount: raw.amount,
      contraAccountId: raw.contraAccountId!,
      referenceNumber: raw.referenceNumber || null,
      description: raw.description || null,
    };

    this.bankTransactionService.create(request).subscribe({
      next: () => {
        this.submitting.set(false);
        this.showModal.set(false);
        this.alert.success('Transaction created successfully');
        this.loadTransactions();
        this.loadBankAccounts(); // balances changed
      },
      error: (error) => {
        this.submitting.set(false);
        this.alert.error(error?.error?.message ?? 'Failed to create transaction');
      },
    });
  }

  openTransferModal(): void {
    this.transferForm.reset({
      fromBankAccountId: null,
      toBankAccountId: null,
      transactionDate: this.today(),
      amount: 0,
      referenceNumber: '',
      description: '',
    });

    this.showTransferModal.set(true);
  }

  closeTransferModal(): void {
    this.showTransferModal.set(false);
  }

  submitTransfer(): void {
    if (this.transferForm.invalid) {
      this.transferForm.markAllAsTouched();
      return;
    }

    const raw = this.transferForm.getRawValue();

    if (raw.fromBankAccountId === raw.toBankAccountId) {
      this.alert.error('Source and destination bank accounts must be different');
      return;
    }

    this.submitting.set(true);

    const request: BankTransferRequest = {
      fromBankAccountId: raw.fromBankAccountId!,
      toBankAccountId: raw.toBankAccountId!,
      transactionDate: raw.transactionDate,
      amount: raw.amount,
      referenceNumber: raw.referenceNumber || null,
      description: raw.description || null,
    };

    this.bankTransactionService.transfer(request).subscribe({
      next: () => {
        this.submitting.set(false);
        this.showTransferModal.set(false);
        this.alert.success('Transfer completed successfully');
        this.loadTransactions();
        this.loadBankAccounts();
      },
      error: (error) => {
        this.submitting.set(false);
        this.alert.error(error?.error?.message ?? 'Failed to complete transfer');
      },
    });
  }

  async reconcileTransaction(txn: BankTransaction): Promise<void> {
    const confirmed = await this.alert.confirm(`Mark ${txn.transactionNumber} as reconciled?`);
    if (!confirmed) return;

    this.bankTransactionService.reconcile(txn.id).subscribe({
      next: () => {
        this.alert.success('Transaction reconciled successfully');
        this.loadTransactions();
      },
      error: (error) =>
        this.alert.error(error?.error?.message ?? 'Failed to reconcile transaction'),
    });
  }

  async unreconcileTransaction(txn: BankTransaction): Promise<void> {
    const confirmed = await this.alert.confirm(`Un-reconcile ${txn.transactionNumber}?`);
    if (!confirmed) return;

    this.bankTransactionService.unreconcile(txn.id).subscribe({
      next: () => {
        this.alert.success('Transaction un-reconciled successfully');
        this.loadTransactions();
      },
      error: (error) =>
        this.alert.error(error?.error?.message ?? 'Failed to un-reconcile transaction'),
    });
  }

  async voidTransactionAction(txn: BankTransaction): Promise<void> {
    const confirmed = await this.alert.confirm(
      `Void ${txn.transactionNumber}? This will reverse its effect on the account balance and journal.`,
    );
    if (!confirmed) return;

    this.bankTransactionService.voidTransaction(txn.id).subscribe({
      next: () => {
        this.alert.success('Transaction voided successfully');
        this.loadTransactions();
        this.loadBankAccounts(); // balance reversed
      },
      error: (error) => this.alert.error(error?.error?.message ?? 'Failed to void transaction'),
    });
  }
}
