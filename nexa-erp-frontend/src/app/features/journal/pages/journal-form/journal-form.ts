import { CommonModule, DecimalPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import {
  FormArray,
  FormGroup,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AlertService } from '../../../../core/services/alert.service';
import { Account } from '../../../accounts/models/account.model';
import { AccountService } from '../../../accounts/services/account.service';
import { JournalEntryType, JournalEntryRequest } from '../../models/journal.model';
import { JournalService } from '../../services/journal.service';

@Component({
  selector: 'app-journal-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, DecimalPipe],
  templateUrl: './journal-form.html',
  styleUrl: './journal-form.scss',
})
export class JournalForm implements OnInit {
  readonly accounts = signal<Account[]>([]);
  readonly loading = signal(false);
  readonly submitting = signal(false);

  readonly journalTypes: JournalEntryType[] = [
    'GENERAL',
    'SALES',
    'PURCHASE',
    'CASH',
    'BANK',
    'PAYROLL',
  ];

  journalId: number | null = null;

  readonly form: FormGroup;

  constructor(
    private fb: NonNullableFormBuilder,
    private journalService: JournalService,
    private accountService: AccountService,
    private route: ActivatedRoute,
    private router: Router,
    private alert: AlertService,
  ) {
    this.form = this.fb.group({
      date: ['', [Validators.required]],
      type: ['GENERAL' as JournalEntryType, [Validators.required]],
      description: [''],
      lines: this.fb.array([]),
    });
  }

  ngOnInit(): void {
    this.journalId = Number(this.route.snapshot.paramMap.get('id')) || null;

    this.loadAccounts();

    if (this.journalId) {
      this.loadJournal(this.journalId);
    } else {
      this.form.patchValue({
        date: new Date().toISOString().substring(0, 10),
      });

      this.addLine();
      this.addLine();
    }
  }

  get lines(): FormArray {
    return this.form.get('lines') as FormArray;
  }

  createLine(
    accountId: number | null = null,
    debit = 0,
    credit = 0,
    description = '',
  ): FormGroup {
    return this.fb.group({
      accountId: [accountId, [Validators.required]],
      debit: [debit, [Validators.min(0)]],
      credit: [credit, [Validators.min(0)]],
      description: [description],
    });
  }

  addLine(): void {
    this.lines.push(this.createLine());
  }

  removeLine(index: number): void {
    if (this.lines.length <= 2) {
      this.alert.warning('At least 2 lines are required');
      return;
    }

    this.lines.removeAt(index);
  }

  loadAccounts(): void {
    this.accountService.search('', '', true).subscribe({
      next: (res) => {
        this.accounts.set(res.data);
      },
    });
  }

  loadJournal(id: number): void {
    this.loading.set(true);

    this.journalService.getById(id).subscribe({
      next: (res) => {
        const journal = res.data;

        if (journal.status !== 'DRAFT') {
          this.alert.warning('Only DRAFT journal can be edited');
          this.router.navigate(['/journals']);
          return;
        }

        this.form.patchValue({
          date: journal.date,
          type: journal.type,
          description: journal.description ?? '',
        });

        this.lines.clear();

        journal.lines.forEach((line) => {
          this.lines.push(
            this.createLine(
              line.accountId,
              Number(line.debit ?? 0),
              Number(line.credit ?? 0),
              line.description ?? '',
            ),
          );
        });

        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.router.navigate(['/journals']);
      },
    });
  }

  onDebitInput(index: number): void {
    const line = this.lines.at(index) as FormGroup;
    const debit = Number(line.get('debit')?.value ?? 0);

    if (debit > 0) {
      line.get('credit')?.setValue(0, { emitEvent: false });
    }
  }

  onCreditInput(index: number): void {
    const line = this.lines.at(index) as FormGroup;
    const credit = Number(line.get('credit')?.value ?? 0);

    if (credit > 0) {
      line.get('debit')?.setValue(0, { emitEvent: false });
    }
  }

  totalDebit(): number {
    return this.lines.controls.reduce((sum, control) => {
      const line = control as FormGroup;
      return sum + Number(line.get('debit')?.value ?? 0);
    }, 0);
  }

  totalCredit(): number {
    return this.lines.controls.reduce((sum, control) => {
      const line = control as FormGroup;
      return sum + Number(line.get('credit')?.value ?? 0);
    }, 0);
  }

  difference(): number {
    return this.totalDebit() - this.totalCredit();
  }

  isBalanced(): boolean {
    return this.totalDebit() > 0 && this.totalDebit() === this.totalCredit();
  }

  private hasInvalidLines(): boolean {
    for (const control of this.lines.controls) {
      const line = control as FormGroup;

      const accountId = line.get('accountId')?.value;
      const debit = Number(line.get('debit')?.value ?? 0);
      const credit = Number(line.get('credit')?.value ?? 0);

      if (!accountId) {
        this.alert.error('Please select account for all lines');
        return true;
      }

      if (debit > 0 && credit > 0) {
        this.alert.error('A line cannot have both debit and credit');
        return true;
      }

      if (debit === 0 && credit === 0) {
        this.alert.error('Each line must have debit or credit amount');
        return true;
      }
    }

    return false;
  }

  submitDraft(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.alert.error('Please complete required fields');
      return;
    }

    if (this.lines.length < 2) {
      this.alert.error('At least 2 lines are required');
      return;
    }

    if (this.hasInvalidLines()) {
      return;
    }

    if (!this.isBalanced()) {
      this.alert.error('Debit and Credit must be equal');
      return;
    }

    this.submitting.set(true);

    const raw = this.form.getRawValue();

    const request: JournalEntryRequest = {
      date: raw.date,
      type: raw.type,
      description: raw.description,
      lines: raw.lines.map((line: any) => ({
        accountId: Number(line.accountId),
        debit: Number(line.debit || 0),
        credit: Number(line.credit || 0),
        description: line.description ?? '',
      })),
    };

    const apiCall = this.journalId
      ? this.journalService.update(this.journalId, request)
      : this.journalService.create(request);

    apiCall.subscribe({
      next: () => {
        this.submitting.set(false);
        this.alert.success(
          this.journalId
            ? 'Journal updated successfully'
            : 'Journal saved as draft',
        );
        this.router.navigate(['/journals']);
      },
      error: (error) => {
        this.submitting.set(false);
        this.alert.error(error?.error?.message ?? 'Failed to save journal');
      },
    });
  }
}