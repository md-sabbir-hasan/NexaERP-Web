import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AlertService } from '../../../../core/services/alert.service';
import { JournalEntry, JournalEntryType, JournalStatus } from '../../models/journal.model';
import { JournalService } from '../../services/journal.service';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-journal-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, DatePipe, DecimalPipe, HasPermissionDirective],
  templateUrl: './journal-list.html',
  styleUrl: './journal-list.scss',
})
export class JournalList implements OnInit {
  readonly journals = signal<JournalEntry[]>([]);
  readonly loading = signal(false);

 readonly search = signal('');
readonly status = signal<JournalStatus | ''>('');
readonly type = signal<JournalEntryType | ''>('');

  readonly journalTypes: JournalEntryType[] = ['GENERAL', 'SALES', 'PURCHASE', 'CASH', 'BANK', 'PAYROLL'];
  readonly statuses: JournalStatus[] = ['DRAFT', 'POSTED', 'REVERSED'];

  readonly filteredJournals = computed(() => {
  const keyword = this.search().trim().toLowerCase();
  const status = this.status();
  const type = this.type();

  return this.journals().filter(journal => {
    const entryNumber = (journal.entryNumber ?? '').toLowerCase();
    const description = (journal.description ?? '').toLowerCase();

    const matchesSearch =
      !keyword ||
      entryNumber.includes(keyword) ||
      description.includes(keyword);

    const matchesStatus = !status || journal.status === status;
    const matchesType = !type || journal.type === type;

    return matchesSearch && matchesStatus && matchesType;
  });
});

  readonly draftCount = computed(() => this.journals().filter(j => j.status === 'DRAFT').length);
  readonly postedCount = computed(() => this.journals().filter(j => j.status === 'POSTED').length);
  readonly reversedCount = computed(() => this.journals().filter(j => j.status === 'REVERSED').length);

  readonly totalAmount = computed(() =>
    this.filteredJournals().reduce((sum, journal) => sum + Number(journal.totalAmount ?? 0), 0),
  );

  constructor(
    private journalService: JournalService,
    private alert: AlertService,
  ) {}

  ngOnInit(): void {
    this.loadJournals();
  }

  loadJournals(): void {
    this.loading.set(true);

    this.journalService.getAll().subscribe({
      next: (res) => {
        this.journals.set(res.data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  clearFilter(): void {
  this.search.set('');
  this.status.set('');
  this.type.set('');
}

  async postJournal(journal: JournalEntry): Promise<void> {
    const confirmed = await this.alert.confirm(`Post ${journal.entryNumber}?`);
    if (!confirmed) return;

    this.journalService.post(journal.id).subscribe({
      next: () => {
        this.alert.success('Journal posted successfully');
        this.loadJournals();
      },
      error: (error) => this.alert.error(error?.error?.message ?? 'Failed to post journal'),
    });
  }

  async reverseJournal(journal: JournalEntry): Promise<void> {
    const confirmed = await this.alert.confirm(`Reverse ${journal.entryNumber}?`);
    if (!confirmed) return;

    this.journalService.reverse(journal.id).subscribe({
      next: () => {
        this.alert.success('Reversal journal created successfully');
        this.loadJournals();
      },
      error: (error) => this.alert.error(error?.error?.message ?? 'Failed to reverse journal'),
    });
  }

  async deleteJournal(journal: JournalEntry): Promise<void> {
    const confirmed = await this.alert.confirm(`Delete draft ${journal.entryNumber}?`);
    if (!confirmed) return;

    this.journalService.delete(journal.id).subscribe({
      next: () => {
        this.alert.success('Journal deleted successfully');
        this.loadJournals();
      },
      error: (error) => this.alert.error(error?.error?.message ?? 'Failed to delete journal'),
    });
  }

  getStatusClass(status: JournalStatus): string {
    return status.toLowerCase();
  }
}