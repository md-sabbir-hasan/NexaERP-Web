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
import { APP_CONFIG } from '../../../../core/config/app.config';
import { Party } from '../../../party/models/party.model';
import { PartyService } from '../../../party/services/party.service';
import { InvoiceRequest } from '../../models/invoice.model';
import { InvoiceService } from '../../services/invoice.service';

@Component({
  selector: 'app-invoice-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, DecimalPipe],
  templateUrl: './invoice-form.html',
  styleUrl: './invoice-form.scss',
})
export class InvoiceForm implements OnInit {
  readonly customers = signal<Party[]>([]);
  readonly loading = signal(false);
  readonly submitting = signal(false);

  readonly selectedCustomer = signal<Party | null>(null);
  readonly invoiceNumber = signal<string | null>(null);
  readonly attachmentUrl = signal<string | null>(null);
  readonly attachmentName = signal<string | null>(null);
  readonly uploadingAttachment = signal(false);
  pendingAttachment: File | null = null;

  invoiceId: number | null = null;

  readonly form: FormGroup;

  constructor(
    private fb: NonNullableFormBuilder,
    private invoiceService: InvoiceService,
    private partyService: PartyService,
    private route: ActivatedRoute,
    private router: Router,
    private alert: AlertService,
  ) {
    this.form = this.fb.group({
      partyId: [null as number | null, [Validators.required]],
      invoiceDate: ['', [Validators.required]],
      paymentTerms: [30, [Validators.required, Validators.min(0)]],
      currencyCode: ['BDT', [Validators.required]],
      reference: [''],
      notes: [''],
      items: this.fb.array([]),
    });
  }

  ngOnInit(): void {
    this.invoiceId = Number(this.route.snapshot.paramMap.get('id')) || null;

    this.loadCustomers();

    if (this.invoiceId) {
      this.loadInvoice(this.invoiceId);
    } else {
      this.form.patchValue({
        invoiceDate: new Date().toISOString().substring(0, 10),
      });

      this.addItem();
    }
  }

  get items(): FormArray {
    return this.form.get('items') as FormArray;
  }

  createItem(
    description = '',
    quantity = 1,
    unitPrice = 0,
    discountPercent = 0,
    vatRate = 0,
    productId: number | null = null,
    unit: string | null = '',
  ): FormGroup {
    return this.fb.group({
      productId: [productId],
      description: [description, [Validators.required]],
      quantity: [quantity, [Validators.required, Validators.min(0.01)]],
      unitPrice: [unitPrice, [Validators.required, Validators.min(0)]],
      unit: [unit ?? ''],
      discountPercent: [discountPercent, [Validators.min(0), Validators.max(100)]],

      vatRate: [vatRate, [Validators.min(0), Validators.max(100)]],
    });
  }

  addItem(): void {
    this.items.push(this.createItem());
  }

  removeItem(index: number): void {
    if (this.items.length <= 1) {
      this.alert.warning('At least 1 item is required');
      return;
    }

    this.items.removeAt(index);
  }

  loadCustomers(): void {
    this.partyService.getByType('CUSTOMER').subscribe({
      next: (res) => {
        this.customers.set(res.data.filter((p) => p.isActive));

        const partyId = this.form.get('partyId')?.value;
        if (partyId) {
          this.selectedCustomer.set(this.customers().find((p) => p.id === Number(partyId)) ?? null);
        }
      },
      error: () => {
        this.alert.error('Failed to load customers');
      },
    });
  }

  onCustomerChange(): void {
    const partyId = this.form.get('partyId')?.value;
    const customer = this.customers().find((p) => p.id === Number(partyId));

    this.selectedCustomer.set(customer ?? null);

    if (customer) {
      this.form.patchValue({
        paymentTerms: customer.paymentTerms ?? 30,
        currencyCode: customer.currency ?? 'BDT',
      });
    }
  }

  loadInvoice(id: number): void {
    this.loading.set(true);

    this.invoiceService.getById(id).subscribe({
      next: (res) => {
        const invoice = res.data;

        if (invoice.status !== 'DRAFT') {
          this.alert.warning('Only DRAFT invoices can be edited');
          this.router.navigate(['/invoice']);
          return;
        }

        this.form.patchValue({
          partyId: invoice.partyId,
          invoiceDate: invoice.invoiceDate,
          paymentTerms: invoice.paymentTerms,
          currencyCode: invoice.currencyCode,
          reference: invoice.reference ?? '',
          notes: invoice.notes ?? '',
        });

        this.invoiceNumber.set(invoice.invoiceNumber);
        this.attachmentUrl.set(
          invoice.attachmentUrl ? this.toFullFileUrl(invoice.attachmentUrl) : null,
        );
        this.selectedCustomer.set(this.customers().find((p) => p.id === invoice.partyId) ?? null);

        this.items.clear();

        invoice.items.forEach((item) => {
          this.items.push(
            this.createItem(
              item.description,
              Number(item.quantity ?? 1),
              Number(item.unitPrice ?? 0),
              Number(item.discountPercent ?? 0),
              Number(item.vatRate ?? 0),
              item.productId,
              item.unit,
            ),
          );
        });

        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.alert.error('Failed to load invoice');
        this.router.navigate(['/invoice']);
      },
    });
  }

  getCustomerAddress(customer: Party): string {
    const parts = [customer.street, customer.city, customer.country].filter((part) => !!part);
    return parts.length ? parts.join(', ') : 'No address on file';
  }

  getDueDate(): string | null {
    const invoiceDate = this.form.get('invoiceDate')?.value;
    const paymentTerms = Number(this.form.get('paymentTerms')?.value ?? 0);

    if (!invoiceDate) return null;

    const [year, month, day] = invoiceDate.split('-').map(Number);

    const date = new Date(year, month - 1, day);

    if (isNaN(date.getTime())) {
      return null;
    }

    date.setDate(date.getDate() + paymentTerms);

    const yyyy = date.getFullYear();
    const mm = String(date.getMonth() + 1).padStart(2, '0');
    const dd = String(date.getDate()).padStart(2, '0');

    return `${yyyy}-${mm}-${dd}`;
  }

  getLineSubTotal(index: number): number {
    const item = this.items.at(index) as FormGroup;
    const qty = Number(item.get('quantity')?.value ?? 0);
    const price = Number(item.get('unitPrice')?.value ?? 0);

    return qty * price;
  }

  getLineDiscount(index: number): number {
    const subTotal = this.getLineSubTotal(index);
    const item = this.items.at(index) as FormGroup;
    const discountPercent = Number(item.get('discountPercent')?.value ?? 0);

    return (subTotal * discountPercent) / 100;
  }

  getLineVat(index: number): number {
    const subTotal = this.getLineSubTotal(index);
    const discount = this.getLineDiscount(index);
    const item = this.items.at(index) as FormGroup;
    const vatRate = Number(item.get('vatRate')?.value ?? 0);

    return ((subTotal - discount) * vatRate) / 100;
  }

  getLineTotal(index: number): number {
    return this.getLineSubTotal(index) - this.getLineDiscount(index) + this.getLineVat(index);
  }

  getSubTotal(): number {
    return this.items.controls.reduce((sum, _, index) => sum + this.getLineSubTotal(index), 0);
  }

  getDiscountTotal(): number {
    return this.items.controls.reduce((sum, _, index) => sum + this.getLineDiscount(index), 0);
  }

  getVatTotal(): number {
    return this.items.controls.reduce((sum, _, index) => sum + this.getLineVat(index), 0);
  }

  getGrandTotal(): number {
    return this.getSubTotal() - this.getDiscountTotal() + this.getVatTotal();
  }

  submitDraft(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.alert.error('Please complete required fields');
      return;
    }

    if (this.items.length === 0) {
      this.alert.error('At least one invoice item is required');
      return;
    }

    this.submitting.set(true);

    const raw = this.form.getRawValue();

    const request: InvoiceRequest = {
      partyId: raw.partyId,
      invoiceDate: raw.invoiceDate,
      paymentTerms: Number(raw.paymentTerms ?? 30),
      currencyCode: raw.currencyCode,
      reference: raw.reference ?? '',
      notes: raw.notes ?? '',

      items: raw.items.map((item: any) => ({
        productId: item.productId ?? null,
        description: item.description,
        quantity: Number(item.quantity ?? 0),
        unitPrice: Number(item.unitPrice ?? 0),
        unit: item.unit || null,
        discountPercent: Number(item.discountPercent ?? 0),
        vatRate: Number(item.vatRate ?? 0),
      })),
    };

    const apiCall = this.invoiceId
      ? this.invoiceService.update(this.invoiceId, request)
      : this.invoiceService.create(request);

    apiCall.subscribe({
      next: (res) => {
        const savedInvoiceId = res.data.id;

        // No attachment
        if (!this.pendingAttachment) {
          this.submitting.set(false);

          this.alert.success(
            this.invoiceId ? 'Invoice updated successfully' : 'Invoice saved as draft',
          );

          this.form.reset();
          this.items.clear();
          this.pendingAttachment = null;

          this.router.navigate(['/invoice', savedInvoiceId]);

          return;
        }

        // Upload attachment after invoice creation/update
        this.uploadingAttachment.set(true);

        this.invoiceService.uploadAttachment(savedInvoiceId, this.pendingAttachment).subscribe({
          next: () => {
            this.submitting.set(false);
            this.uploadingAttachment.set(false);

            this.pendingAttachment = null;

            this.alert.success('Invoice saved with attachment');

            this.form.reset();
            this.items.clear();
            this.pendingAttachment = null;

            this.router.navigate(['/invoice', savedInvoiceId]);
          },

          error: (error) => {
            this.submitting.set(false);
            this.uploadingAttachment.set(false);

            this.pendingAttachment = null;

            this.alert.warning(
              error?.error?.message ?? 'Invoice saved, but attachment upload failed',
            );

            this.router.navigate(['/invoice', savedInvoiceId, 'edit']);
          },
        });
      },

      error: (error) => {
        this.submitting.set(false);

        this.alert.error(error?.error?.message ?? 'Failed to save invoice');
      },
    });
  }

  toFullFileUrl(relativeUrl: string): string {
    const origin = APP_CONFIG.apiUrl.replace(/\/api\/?$/, '');
    return `${origin}${relativeUrl}`;
  }

  onAttachmentSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    if (!file) return;

    const allowedTypes = ['application/pdf', 'image/jpeg', 'image/png'];

    if (!allowedTypes.includes(file.type)) {
      this.alert.error('Only PDF, JPEG or PNG files are allowed');
      input.value = '';
      return;
    }

    if (file.size > 10 * 1024 * 1024) {
      this.alert.error('File size must be less than 10MB');
      input.value = '';
      return;
    }

    // New invoice → keep file temporarily
    if (!this.invoiceId) {
      this.pendingAttachment = file;
      this.attachmentName.set(file.name);

      this.alert.success('Attachment selected');
      input.value = '';
      return;
    }

    // Existing draft → upload immediately
    this.uploadAttachment(file);

    input.value = '';
  }

  private uploadAttachment(file: File): void {
    if (!this.invoiceId) return;

    this.uploadingAttachment.set(true);

    this.invoiceService.uploadAttachment(this.invoiceId, file).subscribe({
      next: (res) => {
        this.uploadingAttachment.set(false);

        this.attachmentUrl.set(this.toFullFileUrl(res.data.fileUrl));

        this.attachmentName.set(res.data.originalName);

        this.alert.success('Attachment uploaded');
      },

      error: (error) => {
        this.uploadingAttachment.set(false);

        this.alert.error(error?.error?.message ?? 'Failed to upload attachment');
      },
    });
  }
}
