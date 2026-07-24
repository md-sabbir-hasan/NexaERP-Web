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
import { CreditNoteReason, CreditNoteRequest } from '../../models/credit-note.model';
import { InvoiceItemOption, InvoiceOption } from '../../models/invoice-option.model';
import { CreditNoteInvoiceService } from '../../services/credit-note-invoice.service';
import { CreditNoteService } from '../../services/credit-note.service';

type CreditNoteItemForm = FormGroup<{
  invoiceItemId: FormControl<number>;
  quantity: FormControl<number>;
}>;

interface ExistingCreditNoteItem {
  invoiceItemId: number;
  quantity: number;
}

@Component({
  selector: 'app-credit-note-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './credit-note-form.html',
  styleUrl: './credit-note-form.scss',
})
export class CreditNoteForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly alert = inject(AlertService);
  private readonly service = inject(CreditNoteService);
  private readonly invoiceService = inject(CreditNoteInvoiceService);

  readonly invoices = signal<InvoiceOption[]>([]);
  readonly selectedInvoice = signal<InvoiceOption | null>(null);

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly editingId = signal<number | null>(null);

  readonly reasons: CreditNoteReason[] = [
    'SALES_RETURN',
    'PRICE_ADJUSTMENT',
    'POST_INVOICE_DISCOUNT',
    'BILLING_ERROR',
    'VAT_ADJUSTMENT',
    'OTHER',
  ];

  readonly form = this.fb.nonNullable.group({
    invoiceId: [0, [Validators.required, Validators.min(1)]],

    creditNoteDate: [this.today(), Validators.required],

    postingDate: [this.today(), Validators.required],

    reason: ['SALES_RETURN' as CreditNoteReason, Validators.required],

    reference: [''],
    notes: [''],

    items: this.fb.array<CreditNoteItemForm>([]),
  });

  /*
   * Reactive form values are converted into a signal.
   * This makes computed totals update whenever quantity changes.
   */
  private readonly formValueSignal = toSignal(this.form.valueChanges, {
    initialValue: this.form.getRawValue(),
  });

  readonly subTotal = computed(() => {
    this.formValueSignal();

    return this.controls().reduce((total: number, control: CreditNoteItemForm, index: number) => {
      const item = this.item(index);

      if (!item) {
        return total;
      }

      const quantity = Number(control.controls.quantity.value || 0);

      return total + quantity * Number(item.unitPrice || 0);
    }, 0);
  });

  readonly discountTotal = computed(() => {
    this.formValueSignal();

    return this.controls().reduce((total: number, control: CreditNoteItemForm, index: number) => {
      const item = this.item(index);

      if (!item) {
        return total;
      }

      const quantity = Number(control.controls.quantity.value || 0);

      const lineSubTotal = quantity * Number(item.unitPrice || 0);

      const discount = (lineSubTotal * Number(item.discountPercent ?? 0)) / 100;

      return total + discount;
    }, 0);
  });

  readonly vatTotal = computed(() => {
    this.formValueSignal();

    return this.controls().reduce((total: number, control: CreditNoteItemForm, index: number) => {
      const item = this.item(index);

      if (!item) {
        return total;
      }

      const quantity = Number(control.controls.quantity.value || 0);

      const lineSubTotal = quantity * Number(item.unitPrice || 0);

      const discount = (lineSubTotal * Number(item.discountPercent ?? 0)) / 100;

      const afterDiscount = lineSubTotal - discount;

      const vat = (afterDiscount * Number(item.vatRate ?? 0)) / 100;

      return total + vat;
    }, 0);
  });

  readonly total = computed(() => {
    this.formValueSignal();

    return this.subTotal() - this.discountTotal() + this.vatTotal();
  });

  get itemsArray(): FormArray<CreditNoteItemForm> {
    return this.form.controls.items;
  }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');

    const parsedId = Number(idParam);

    const id = Number.isFinite(parsedId) && parsedId > 0 ? parsedId : null;

    this.editingId.set(id);

    /*
     * Disable Reactive Form control from TypeScript,
     * not through [disabled] in the template.
     */
    if (id !== null) {
      this.form.controls.invoiceId.disable({
        emitEvent: false,
      });
    } else {
      this.form.controls.invoiceId.enable({
        emitEvent: false,
      });
    }

    this.load();
  }

  load(): void {
    this.loading.set(true);

    const id = this.editingId();

    if (id === null) {
      this.invoiceService.getAll().subscribe({
        next: (response) => {
          const invoices = response.data ?? [];

          this.invoices.set(
            invoices.filter((invoice) => ['POSTED', 'PARTIAL'].includes(invoice.status)),
          );

          this.loading.set(false);
        },

        error: (error) => {
          this.loading.set(false);

          this.alert.error(error?.error?.message ?? 'Could not load invoices');
        },
      });

      return;
    }

    forkJoin({
      invoices: this.invoiceService.getAll(),
      note: this.service.getById(id),
    }).subscribe({
      next: ({ invoices, note }) => {
        const invoiceList = invoices.data ?? [];

        this.invoices.set(
          invoiceList.filter((invoice) => ['POSTED', 'PARTIAL'].includes(invoice.status)),
        );

        const creditNote = note.data;

        if (!creditNote) {
          this.loading.set(false);
          this.alert.error('Credit note not found');
          this.router.navigate(['/credit-notes']);
          return;
        }

        this.form.patchValue({
          invoiceId: creditNote.invoiceId,
          creditNoteDate: creditNote.creditNoteDate,
          postingDate: creditNote.postingDate,
          reason: creditNote.reason,
          reference: creditNote.reference ?? '',
          notes: creditNote.notes ?? '',
        });

        const selectedInvoice =
          this.invoices().find((invoice) => invoice.id === creditNote.invoiceId) ?? null;

        if (selectedInvoice) {
          this.selectedInvoice.set(selectedInvoice);

          this.loadExistingItems(creditNote.items);

          this.loading.set(false);
          return;
        }

        /*
         * Load invoice directly if it was not present
         * in the POSTED/PARTIAL list.
         */
        this.invoiceService.getById(creditNote.invoiceId).subscribe({
          next: (invoiceResponse) => {
            const invoice = invoiceResponse.data ?? null;

            this.selectedInvoice.set(invoice);

            this.loadExistingItems(creditNote.items);

            this.loading.set(false);
          },

          error: (error) => {
            this.loading.set(false);

            this.alert.error(error?.error?.message ?? 'Could not load invoice');
          },
        });
      },

      error: (error) => {
        this.loading.set(false);

        this.alert.error(error?.error?.message ?? 'Could not load credit note');
      },
    });
  }

  onInvoiceChange(): void {
    const invoiceId = Number(this.form.controls.invoiceId.value);

    const invoice = this.invoices().find((item: InvoiceOption) => item.id === invoiceId) ?? null;

    this.selectedInvoice.set(invoice);
    this.itemsArray.clear();

    if (!invoice) {
      return;
    }

    invoice.items.forEach((item: InvoiceItemOption) => {
      this.itemsArray.push(this.itemGroup(item.id, 0));
    });
  }

  controls(): CreditNoteItemForm[] {
    return this.itemsArray.controls;
  }

  item(index: number): InvoiceItemOption | null {
    const control = this.controls()[index];

    if (!control) {
      return null;
    }

    const invoiceItemId = Number(control.controls.invoiceItemId.value);

    return (
      this.selectedInvoice()?.items.find((item: InvoiceItemOption) => item.id === invoiceItemId) ??
      null
    );
  }

  lineSubTotal(index: number): number {
    const control = this.controls()[index];
    const item = this.item(index);

    if (!control || !item) {
      return 0;
    }

    const quantity = Number(control.controls.quantity.value || 0);

    return quantity * Number(item.unitPrice || 0);
  }

  lineDiscount(index: number): number {
    const item = this.item(index);

    if (!item) {
      return 0;
    }

    return (this.lineSubTotal(index) * Number(item.discountPercent ?? 0)) / 100;
  }

  lineVat(index: number): number {
    const item = this.item(index);

    if (!item) {
      return 0;
    }

    const afterDiscount = this.lineSubTotal(index) - this.lineDiscount(index);

    return (afterDiscount * Number(item.vatRate ?? 0)) / 100;
  }

  lineTotal(index: number): number {
    return this.lineSubTotal(index) - this.lineDiscount(index) + this.lineVat(index);
  }

  reasonLabel(value: string): string {
    return value.replaceAll('_', ' ');
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();

      this.alert.warning('Complete required fields');

      return;
    }

    const invoice = this.selectedInvoice();

    if (!invoice) {
      this.alert.warning('Select an invoice first');

      return;
    }

    const items = this.controls()
      .map((control: CreditNoteItemForm) => ({
        invoiceItemId: Number(control.controls.invoiceItemId.value),

        quantity: Number(control.controls.quantity.value || 0),
      }))
      .filter((item) => item.quantity > 0);

    if (items.length === 0) {
      this.alert.warning('Enter at least one credit quantity');

      return;
    }

    for (const selectedItem of items) {
      const invoiceItem = invoice.items.find(
        (item: InvoiceItemOption) => item.id === selectedItem.invoiceItemId,
      );

      if (!invoiceItem) {
        this.alert.warning('Invalid invoice item selected');

        return;
      }

      if (selectedItem.quantity > Number(invoiceItem.quantity)) {
        this.alert.warning(
          `Credit quantity cannot exceed invoice quantity for ${invoiceItem.description}`,
        );

        return;
      }
    }

    if (this.total() > Number(invoice.dueAmount)) {
      this.alert.warning('Credit total cannot exceed invoice due amount');

      return;
    }

    const value = this.form.getRawValue();

    const body: CreditNoteRequest = {
      invoiceId: Number(value.invoiceId),
      creditNoteDate: value.creditNoteDate,
      postingDate: value.postingDate || null,
      reason: value.reason,
      reference: value.reference.trim() || null,
      notes: value.notes.trim() || null,
      items,
    };

    this.saving.set(true);

    const id = this.editingId();

    const request$ = id === null ? this.service.create(body) : this.service.update(id, body);

    request$.subscribe({
      next: (response) => {
        this.saving.set(false);

        this.alert.success(
          response.message ||
            (id === null ? 'Credit note created successfully' : 'Credit note updated successfully'),
        );

        this.router.navigate(['/credit-notes']);
      },

      error: (error) => {
        this.saving.set(false);

        this.alert.error(error?.error?.message ?? 'Could not save credit note');
      },
    });
  }

  private loadExistingItems(items: ExistingCreditNoteItem[]): void {
    this.itemsArray.clear();

    items.forEach((item: ExistingCreditNoteItem) => {
      this.itemsArray.push(this.itemGroup(item.invoiceItemId, Number(item.quantity)));
    });
  }

  private itemGroup(id: number, quantity: number): CreditNoteItemForm {
    return this.fb.nonNullable.group({
      invoiceItemId: [id, [Validators.required, Validators.min(1)]],

      quantity: [quantity, [Validators.required, Validators.min(0)]],
    });
  }

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }
}
