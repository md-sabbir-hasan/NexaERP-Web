import { CommonModule, DecimalPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { AlertService } from '../../../../core/services/alert.service';
import { Account } from '../../../accounts/models/account.model';
import { AccountService } from '../../../accounts/services/account.service';
import { BankReconciliation, BankStatementLine } from '../../models/bank-reconciliation.model';
import { BankTransaction, BankTransactionRequest, TransactionType } from '../../models/bank-transaction.model';
import { BankReconciliationService } from '../../services/bank-reconciliation.service';
import { BankTransactionService } from '../../services/bank-transaction.service';

@Component({
  selector: 'app-bank-reconciliation-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, DecimalPipe, RouterLink],
  templateUrl: './bank-reconciliation-detail.html',
  styleUrl: './bank-reconciliation-detail.scss',
})
export class BankReconciliationDetail implements OnInit {
  readonly reconciliation = signal<BankReconciliation | null>(null);
  readonly unmatched = signal<BankTransaction[]>([]);
  readonly matched = signal<BankTransaction[]>([]);
  readonly contraAccounts = signal<Account[]>([]);
  readonly statementLines = signal<BankStatementLine[]>([]);

  readonly loading = signal(false);
  readonly submitting = signal(false);
  readonly uploading = signal(false);
  readonly showAdjustmentModal = signal(false);

  readonly selectedIds = signal<Set<number>>(new Set());
  readonly lineMatchSelection = signal<Record<number, number | null>>({});

  // Set when the adjustment modal was opened to convert a specific unmatched
  // statement line (rather than a free-standing adjustment)
  private activeLineId: number | null = null;
  selectedCsvFile: File | null = null;

  readonly adjustmentForm;

  private reconciliationId!: number;

  constructor(
    private route: ActivatedRoute,
    private reconciliationService: BankReconciliationService,
    private bankTransactionService: BankTransactionService,
    private accountService: AccountService,
    private alert: AlertService,
    private fb: NonNullableFormBuilder,
  ) {
    this.adjustmentForm = this.fb.group({
      transactionType: ['DEBIT' as TransactionType, [Validators.required]],
      transactionDate: [this.today(), [Validators.required]],
      amount: [0, [Validators.required, Validators.min(0.01)]],
      contraAccountId: [null as number | null, [Validators.required]],
      referenceNumber: [''],
      description: ['', [Validators.required]],
    });
  }

  ngOnInit(): void {
    this.reconciliationId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadContraAccounts();
    this.loadAll();
  }

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }

  loadAll(): void {
    this.loading.set(true);
    this.selectedIds.set(new Set());

    this.reconciliationService.getById(this.reconciliationId).subscribe({
      next: (res) => {
        this.reconciliation.set(res.data);
        this.loadUnmatched();
        this.loadMatched(res.data.bankAccountId);
        this.loadStatementLines();
      },
      error: () => {
        this.loading.set(false);
        this.alert.error('Failed to load reconciliation');
      },
    });
  }

  loadUnmatched(): void {
    this.reconciliationService.getUnmatchedTransactions(this.reconciliationId).subscribe({
      next: (res) => {
        this.unmatched.set(res.data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  loadMatched(bankAccountId: number): void {
    this.bankTransactionService.getByAccount(bankAccountId).subscribe({
      next: (res) => this.matched.set(res.data.filter((t) => t.reconciliationId === this.reconciliationId)),
    });
  }

  loadContraAccounts(): void {
    this.accountService.getAll().subscribe({
      next: (res) => this.contraAccounts.set(res.data.filter((a) => a.isActive && !a.hasChildren)),
    });
  }

  loadStatementLines(): void {
    this.reconciliationService.getStatementLines(this.reconciliationId).subscribe({
      next: (res) => this.statementLines.set(res.data),
    });
  }

  // ---- CSV import ----

  onCsvFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedCsvFile = input.files?.[0] ?? null;
  }

  uploadStatement(): void {
    if (!this.selectedCsvFile) {
      this.alert.warning('Choose a CSV file first');
      return;
    }

    this.uploading.set(true);

    this.reconciliationService.importStatement(this.reconciliationId, this.selectedCsvFile).subscribe({
      next: (res) => {
        this.uploading.set(false);
        this.selectedCsvFile = null;
        this.alert.success(
          `Imported ${res.data.totalLines} lines — ${res.data.autoMatchedCount} auto-matched, ${res.data.unmatchedCount} need review`,
        );
        this.loadAll();
      },
      error: (error) => {
        this.uploading.set(false);
        this.alert.error(error?.error?.message ?? 'Failed to import statement');
      },
    });
  }

  unmatchedStatementLines(): BankStatementLine[] {
    return this.statementLines().filter((l) => l.status === 'UNMATCHED');
  }

  matchedStatementLines(): BankStatementLine[] {
    return this.statementLines().filter((l) => l.status === 'MATCHED');
  }

  // Candidate book transactions of the same type as this statement line, for the manual-match dropdown
  candidatesForLine(line: BankStatementLine): BankTransaction[] {
    return this.unmatched().filter((t) => t.transactionType === line.transactionType);
  }

  setLineMatchSelection(lineId: number, transactionId: string): void {
    this.lineMatchSelection.update((map) => ({ ...map, [lineId]: transactionId ? Number(transactionId) : null }));
  }

  matchLine(line: BankStatementLine): void {
    const txnId = this.lineMatchSelection()[line.id];
    if (!txnId) {
      this.alert.warning('Select a transaction to match this line against');
      return;
    }

    this.reconciliationService.matchStatementLine(this.reconciliationId, line.id, txnId).subscribe({
      next: () => {
        this.alert.success('Statement line matched');
        this.loadAll();
      },
      error: (error) => this.alert.error(error?.error?.message ?? 'Failed to match statement line'),
    });
  }

  openConvertToAdjustment(line: BankStatementLine): void {
    this.activeLineId = line.id;
    this.adjustmentForm.reset({
      transactionType: line.transactionType,
      transactionDate: line.lineDate,
      amount: line.amount,
      contraAccountId: null,
      referenceNumber: line.referenceNumber ?? '',
      description: line.description ?? '',
    });
    this.showAdjustmentModal.set(true);
  }

  // ---- Manual matching ----

  toggleSelect(id: number): void {
    const set = new Set(this.selectedIds());
    if (set.has(id)) {
      set.delete(id);
    } else {
      set.add(id);
    }
    this.selectedIds.set(set);
  }

  isSelected(id: number): boolean {
    return this.selectedIds().has(id);
  }

  get isCompleted(): boolean {
    return this.reconciliation()?.status === 'COMPLETED';
  }

  matchSelected(): void {
    const ids = Array.from(this.selectedIds());
    if (ids.length === 0) {
      this.alert.warning('Select at least one transaction to match');
      return;
    }

    this.reconciliationService.match(this.reconciliationId, ids).subscribe({
      next: () => {
        this.alert.success('Transactions matched');
        this.loadAll();
      },
      error: (error) => this.alert.error(error?.error?.message ?? 'Failed to match transactions'),
    });
  }

  async unmatchOne(txn: BankTransaction): Promise<void> {
    const confirmed = await this.alert.confirm(`Unmatch ${txn.transactionNumber}?`);
    if (!confirmed) return;

    this.reconciliationService.unmatch(this.reconciliationId, txn.id).subscribe({
      next: () => {
        this.alert.success('Transaction unmatched');
        this.loadAll();
      },
      error: (error) => this.alert.error(error?.error?.message ?? 'Failed to unmatch transaction'),
    });
  }

  openAdjustmentModal(): void {
    this.activeLineId = null;
    this.adjustmentForm.reset({
      transactionType: 'DEBIT',
      transactionDate: this.reconciliation()?.statementDate ?? this.today(),
      amount: 0,
      contraAccountId: null,
      referenceNumber: '',
      description: '',
    });
    this.showAdjustmentModal.set(true);
  }

  closeAdjustmentModal(): void {
    this.activeLineId = null;
    this.showAdjustmentModal.set(false);
  }

  submitAdjustment(): void {
    if (this.adjustmentForm.invalid) {
      this.adjustmentForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    const raw = this.adjustmentForm.getRawValue();
    const r = this.reconciliation();
    if (!r) return;

    if (this.activeLineId !== null) {
      this.reconciliationService
        .convertLineToAdjustment(this.reconciliationId, this.activeLineId, raw.contraAccountId!, raw.description)
        .subscribe({
          next: () => {
            this.submitting.set(false);
            this.showAdjustmentModal.set(false);
            this.activeLineId = null;
            this.alert.success('Statement line converted to an adjustment and matched');
            this.loadAll();
          },
          error: (error) => {
            this.submitting.set(false);
            this.alert.error(error?.error?.message ?? 'Failed to convert statement line');
          },
        });
      return;
    }

    const request: BankTransactionRequest = {
      bankAccountId: r.bankAccountId,
      transactionType: raw.transactionType,
      transactionDate: raw.transactionDate,
      amount: raw.amount,
      contraAccountId: raw.contraAccountId!,
      referenceNumber: raw.referenceNumber || null,
      description: raw.description,
    };

    this.reconciliationService.addAdjustment(this.reconciliationId, request).subscribe({
      next: () => {
        this.submitting.set(false);
        this.showAdjustmentModal.set(false);
        this.alert.success('Adjustment recorded and matched');
        this.loadAll();
      },
      error: (error) => {
        this.submitting.set(false);
        this.alert.error(error?.error?.message ?? 'Failed to record adjustment');
      },
    });
  }

  async complete(): Promise<void> {
    const confirmed = await this.alert.confirm(
      'Complete this reconciliation? All matched transactions will be locked.',
    );
    if (!confirmed) return;

    this.reconciliationService.complete(this.reconciliationId).subscribe({
      next: () => {
        this.alert.success('Reconciliation completed');
        this.loadAll();
      },
      error: (error) => this.alert.error(error?.error?.message ?? 'Balances do not tie out yet'),
    });
  }

  async reopen(): Promise<void> {
    const confirmed = await this.alert.confirm('Reopen this completed reconciliation?');
    if (!confirmed) return;

    this.reconciliationService.reopen(this.reconciliationId).subscribe({
      next: () => {
        this.alert.success('Reconciliation reopened');
        this.loadAll();
      },
      error: (error) => this.alert.error(error?.error?.message ?? 'Failed to reopen reconciliation'),
    });
  }
}
