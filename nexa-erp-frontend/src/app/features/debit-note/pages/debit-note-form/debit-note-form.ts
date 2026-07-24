import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { AlertService } from '../../../../core/services/alert.service';
import { DebitNoteReason, DebitNoteRequest } from '../../models/debit-note.model';
import { VendorBillItemOption, VendorBillOption } from '../../models/vendor-bill-option.model';
import { DebitNoteService } from '../../services/debit-note.service';
import { DebitNoteVendorBillService } from '../../services/debit-note-vendor-bill.service';

type DebitItemForm = FormGroup<{
  vendorBillItemId: FormControl<number>;
  quantity: FormControl<number>;
}>;

@Component({
  selector: 'app-debit-note-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './debit-note-form.html',
  styleUrl: './debit-note-form.scss',
})
export class DebitNoteForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly alert = inject(AlertService);
  private readonly service = inject(DebitNoteService);
  private readonly billService = inject(DebitNoteVendorBillService);

  readonly bills = signal<VendorBillOption[]>([]);
  readonly selectedBill = signal<VendorBillOption | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly editingId = signal<number | null>(null);

  readonly reasons: DebitNoteReason[] = [
    'PURCHASE_RETURN',
    'PRICE_ADJUSTMENT',
    'BILL_CORRECTION',
    'VAT_ADJUSTMENT',
    'QUALITY_REJECTION',
    'OTHER',
  ];

  readonly form = this.fb.nonNullable.group({
    vendorBillId: [0, [Validators.required, Validators.min(1)]],
    debitNoteDate: [this.today(), Validators.required],
    postingDate: [this.today(), Validators.required],
    reason: ['PURCHASE_RETURN' as DebitNoteReason, Validators.required],
    reference: [''],
    notes: [''],
    items: this.fb.array<DebitItemForm>([]),
  });

  private readonly formValue = toSignal(this.form.valueChanges, {
    initialValue: this.form.getRawValue(),
  });

  readonly subTotal = computed(() => {
    this.formValue();
    return this.controls().reduce((sum, _, i) => sum + this.lineSubTotal(i), 0);
  });

  readonly discountTotal = computed(() => {
    this.formValue();
    return this.controls().reduce((sum, _, i) => sum + this.lineDiscount(i), 0);
  });

  readonly vatTotal = computed(() => {
    this.formValue();
    return this.controls().reduce((sum, _, i) => sum + this.lineVat(i), 0);
  });

  readonly tdsTotal = computed(() => {
    this.formValue();
    return this.controls().reduce((sum, _, i) => sum + this.lineTds(i), 0);
  });

  readonly grandTotal = computed(() => this.subTotal() - this.discountTotal() + this.vatTotal());
  readonly netAdjustment = computed(() => this.grandTotal() - this.tdsTotal());

  get itemsArray(): FormArray<DebitItemForm> {
    return this.form.controls.items;
  }

  ngOnInit(): void {
    const parsed = Number(this.route.snapshot.paramMap.get('id'));
    const id = Number.isFinite(parsed) && parsed > 0 ? parsed : null;
    this.editingId.set(id);

    if (id !== null) {
      this.form.controls.vendorBillId.disable({ emitEvent: false });
    }

    this.load();
  }

  load(): void {
    this.loading.set(true);
    const id = this.editingId();

    if (id === null) {
      this.billService.getAll().subscribe({
        next: (response) => {
          this.bills.set(
            (response.data ?? []).filter((bill) => ['POSTED', 'PARTIAL'].includes(bill.status)),
          );
          this.loading.set(false);
        },
        error: (error) => {
          this.loading.set(false);
          this.alert.error(error?.error?.message ?? 'Could not load vendor bills');
        },
      });
      return;
    }

    forkJoin({
      bills: this.billService.getAll(),
      note: this.service.getById(id),
    }).subscribe({
      next: ({ bills, note }) => {
        this.bills.set(
          (bills.data ?? []).filter((bill) => ['POSTED', 'PARTIAL'].includes(bill.status)),
        );
        const debitNote = note.data;

        if (!debitNote) {
          this.alert.error('Debit note not found');
          this.router.navigate(['/debit-notes']);
          return;
        }

        this.form.patchValue({
          vendorBillId: debitNote.vendorBillId,
          debitNoteDate: debitNote.debitNoteDate,
          postingDate: debitNote.postingDate,
          reason: debitNote.reason,
          reference: debitNote.reference ?? '',
          notes: debitNote.notes ?? '',
        });

        const selected = this.bills().find((bill) => bill.id === debitNote.vendorBillId) ?? null;
        if (selected) {
          this.selectedBill.set(selected);
          this.loadItems(debitNote.items);
          this.loading.set(false);
          return;
        }

        this.billService.getById(debitNote.vendorBillId).subscribe({
          next: (response) => {
            this.selectedBill.set(response.data ?? null);
            this.loadItems(debitNote.items);
            this.loading.set(false);
          },
          error: (error) => {
            this.loading.set(false);
            this.alert.error(error?.error?.message ?? 'Could not load vendor bill');
          },
        });
      },
      error: (error) => {
        this.loading.set(false);
        this.alert.error(error?.error?.message ?? 'Could not load debit note');
      },
    });
  }

  onBillChange(): void {
    const id = Number(this.form.controls.vendorBillId.value);
    const bill = this.bills().find((item) => item.id === id) ?? null;
    this.selectedBill.set(bill);
    this.itemsArray.clear();
    bill?.items.forEach((item) => this.itemsArray.push(this.itemGroup(item.id, 0)));
  }

  controls(): DebitItemForm[] {
    return this.itemsArray.controls;
  }

  item(index: number): VendorBillItemOption | null {
    const control = this.controls()[index];
    if (!control) return null;
    const id = Number(control.controls.vendorBillItemId.value);
    return this.selectedBill()?.items.find((item) => item.id === id) ?? null;
  }

  lineSubTotal(index: number): number {
    const control = this.controls()[index];
    const item = this.item(index);
    if (!control || !item) return 0;
    return Number(control.controls.quantity.value || 0) * Number(item.unitPrice || 0);
  }

  lineDiscount(index: number): number {
    const item = this.item(index);
    if (!item) return 0;
    return (this.lineSubTotal(index) * Number(item.discountPercent ?? 0)) / 100;
  }

  lineVat(index: number): number {
    const item = this.item(index);
    if (!item) return 0;
    const base = this.lineSubTotal(index) - this.lineDiscount(index);
    return (base * Number(item.vatRate ?? 0)) / 100;
  }

  lineTds(index: number): number {
    const item = this.item(index);
    if (!item) return 0;
    const base = this.lineSubTotal(index) - this.lineDiscount(index);
    return (base * Number(item.tdsRate ?? 0)) / 100;
  }

  lineNet(index: number): number {
    return (
      this.lineSubTotal(index) -
      this.lineDiscount(index) +
      this.lineVat(index) -
      this.lineTds(index)
    );
  }

  label(value: string): string {
    return value.replaceAll('_', ' ');
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.alert.warning('Complete required fields');
      return;
    }

    const bill = this.selectedBill();
    if (!bill) {
      this.alert.warning('Select a vendor bill');
      return;
    }

    const items = this.controls()
      .map((control) => ({
        vendorBillItemId: Number(control.controls.vendorBillItemId.value),
        quantity: Number(control.controls.quantity.value || 0),
      }))
      .filter((item) => item.quantity > 0);

    if (items.length === 0) {
      this.alert.warning('Enter at least one debit quantity');
      return;
    }

    if (this.netAdjustment() > Number(bill.dueAmount)) {
      this.alert.warning('Debit note adjustment cannot exceed vendor bill due');
      return;
    }

    const raw = this.form.getRawValue();
    const body: DebitNoteRequest = {
      vendorBillId: Number(raw.vendorBillId),
      debitNoteDate: raw.debitNoteDate,
      postingDate: raw.postingDate || null,
      reason: raw.reason,
      reference: raw.reference.trim() || null,
      notes: raw.notes.trim() || null,
      items,
    };

    this.saving.set(true);
    const id = this.editingId();
    const request$ = id === null ? this.service.create(body) : this.service.update(id, body);

    request$.subscribe({
      next: (response) => {
        this.saving.set(false);
        this.alert.success(response.message || 'Debit note saved');
        this.router.navigate(['/debit-notes']);
      },
      error: (error) => {
        this.saving.set(false);
        this.alert.error(error?.error?.message ?? 'Could not save debit note');
      },
    });
  }

  private loadItems(items: { vendorBillItemId: number; quantity: number }[]): void {
    this.itemsArray.clear();
    items.forEach((item) =>
      this.itemsArray.push(this.itemGroup(item.vendorBillItemId, Number(item.quantity))),
    );
  }

  private itemGroup(id: number, quantity: number): DebitItemForm {
    return this.fb.nonNullable.group({
      vendorBillItemId: [id, [Validators.required, Validators.min(1)]],
      quantity: [quantity, [Validators.required, Validators.min(0)]],
    });
  }

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }
}
