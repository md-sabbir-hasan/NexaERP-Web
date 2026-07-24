import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { AlertService } from '../../../../core/services/alert.service';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';
import { DebitNote, DebitNoteCancelledReason } from '../../models/debit-note.model';
import { DebitNoteService } from '../../services/debit-note.service';

@Component({
  selector: 'app-debit-note-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, HasPermissionDirective],
  templateUrl: './debit-note-list.html',
  styleUrl: './debit-note-list.scss',
})
export class DebitNoteList implements OnInit {
  private readonly service = inject(DebitNoteService);
  private readonly alert = inject(AlertService);
  readonly notes = signal<DebitNote[]>([]);
  readonly loading = signal(false);
  readonly actionId = signal<number | null>(null);
  readonly search = signal('');
  readonly status = signal('ALL');
  readonly filtered = computed(() => {
    const q = this.search().trim().toLowerCase();
    const s = this.status();
    return this.notes().filter(
      (n) =>
        (s === 'ALL' || n.status === s) &&
        (!q ||
          [
            n.debitNoteNumber,
            n.vendorBillNumber,
            n.partyName,
            n.reason,
            n.status,
            n.reference ?? '',
          ]
            .join(' ')
            .toLowerCase()
            .includes(q)),
    );
  });
  readonly total = computed(() => this.notes().length);
  readonly draft = computed(() => this.notes().filter((n) => n.status === 'DRAFT').length);
  readonly approved = computed(() => this.notes().filter((n) => n.status === 'APPROVED').length);
  readonly posted = computed(() => this.notes().filter((n) => n.status === 'POSTED').length);
  ngOnInit(): void {
    this.load();
  }
  load(): void {
    this.loading.set(true);
    this.service.getAll().subscribe({
      next: (r) => {
        this.notes.set(r.data ?? []);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.alert.error(e?.error?.message ?? 'Could not load debit notes');
      },
    });
  }
  async approve(n: DebitNote) {
    if (await this.alert.confirm(`Approve ${n.debitNoteNumber}?`))
      this.run(n.id, this.service.approve(n.id), 'Debit note approved');
  }
  async post(n: DebitNote) {
    if (await this.alert.confirm(`Post ${n.debitNoteNumber}?`))
      this.run(n.id, this.service.post(n.id), 'Debit note posted');
  }
  async remove(n: DebitNote) {
    if (await this.alert.confirm(`Delete ${n.debitNoteNumber}?`))
      this.run(n.id, this.service.delete(n.id), 'Debit note deleted');
  }
  async cancel(n: DebitNote) {
    const r = window.prompt(
      'Reason: DUPLICATE, WRONG_VENDOR, WRONG_AMOUNT, WRONG_REFERENCE, OTHER',
      'WRONG_AMOUNT',
    ) as DebitNoteCancelledReason | null;
    if (!r) return;
    const valid: DebitNoteCancelledReason[] = [
      'DUPLICATE',
      'WRONG_VENDOR',
      'WRONG_AMOUNT',
      'WRONG_REFERENCE',
      'OTHER',
    ];
    if (!valid.includes(r)) {
      this.alert.warning('Invalid cancellation reason');
      return;
    }
    if (await this.alert.confirm(`Cancel ${n.debitNoteNumber}?`))
      this.run(n.id, this.service.cancel(n.id, r), 'Debit note cancelled');
  }
  reasonLabel(v: string) {
    return v.replaceAll('_', ' ');
  }
  statusClass(v: string) {
    return `status-${v.toLowerCase()}`;
  }
  private run(id: number, req: Observable<unknown>, msg: string) {
    this.actionId.set(id);
    req.subscribe({
      next: (r: any) => {
        this.actionId.set(null);
        this.alert.success(r?.message || msg);
        this.load();
      },
      error: (e) => {
        this.actionId.set(null);
        this.alert.error(e?.error?.message ?? 'Action failed');
      },
    });
  }
}
