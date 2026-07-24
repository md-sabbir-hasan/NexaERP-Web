import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { AlertService } from '../../../../core/services/alert.service';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';
import { CreditNote, CreditNoteCancelledReason } from '../../models/credit-note.model';
import { CreditNoteService } from '../../services/credit-note.service';

@Component({
  selector: 'app-credit-note-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, HasPermissionDirective],
  templateUrl: './credit-note-list.html',
  styleUrl: './credit-note-list.scss',
})
export class CreditNoteList implements OnInit {
  private readonly service = inject(CreditNoteService);
  private readonly alert = inject(AlertService);
  readonly notes = signal<CreditNote[]>([]);
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
          [n.creditNoteNumber, n.invoiceNumber, n.partyName, n.reason, n.status, n.reference ?? '']
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
        this.alert.error(e?.error?.message ?? 'Could not load credit notes');
      },
    });
  }
  async approve(n: CreditNote) {
    if (await this.alert.confirm(`Approve ${n.creditNoteNumber}?`))
      this.run(n.id, this.service.approve(n.id), 'Credit note approved');
  }
  async post(n: CreditNote) {
    if (await this.alert.confirm(`Post ${n.creditNoteNumber}?`))
      this.run(n.id, this.service.post(n.id), 'Credit note posted');
  }
  async remove(n: CreditNote) {
    if (await this.alert.confirm(`Delete ${n.creditNoteNumber}?`))
      this.run(n.id, this.service.delete(n.id), 'Credit note deleted');
  }
  async cancel(n: CreditNote) {
    const r = window.prompt(
      'Reason: DUPLICATE, WRONG_CUSTOMER, WRONG_AMOUNT, WRONG_REFERENCE, OTHER',
      'WRONG_AMOUNT',
    ) as CreditNoteCancelledReason | null;
    if (!r) return;
    const valid: CreditNoteCancelledReason[] = [
      'DUPLICATE',
      'WRONG_CUSTOMER',
      'WRONG_AMOUNT',
      'WRONG_REFERENCE',
      'OTHER',
    ];
    if (!valid.includes(r)) {
      this.alert.warning('Invalid cancellation reason');
      return;
    }
    if (await this.alert.confirm(`Cancel ${n.creditNoteNumber}?`))
      this.run(n.id, this.service.cancel(n.id, r), 'Credit note cancelled');
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
