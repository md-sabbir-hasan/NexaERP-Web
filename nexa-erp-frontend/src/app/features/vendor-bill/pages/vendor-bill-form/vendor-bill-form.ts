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
import { Party } from '../../../party/models/party.model';
import { PartyService } from '../../../party/services/party.service';
import {
  VendorBillReferenceType,
  VendorBillRequest,
  VendorBillType,
} from '../../models/vendor-bill.model';
import { VendorBillService } from '../../services/vendor-bill.service';

@Component({
  selector: 'app-vendor-bill-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, DecimalPipe],
  templateUrl: './vendor-bill-form.html',
  styleUrl: './vendor-bill-form.scss',
})
export class VendorBillForm implements OnInit {
  readonly vendors = signal<Party[]>([]);
  readonly expenseAccounts = signal<Account[]>([]);
  readonly loading = signal(false);
  readonly submitting = signal(false);

  billId: number | null = null;

  readonly billTypes: VendorBillType[] = ['EXPENSE', 'PURCHASE', 'SERVICE', 'ASSET'];
  readonly referenceTypes: VendorBillReferenceType[] = ['MANUAL', 'PURCHASE_ORDER', 'GOODS_RECEIPT'];

  readonly form: FormGroup;

  constructor(
    private fb: NonNullableFormBuilder,
    private vendorBillService: VendorBillService,
    private partyService: PartyService,
    private accountService: AccountService,
    private route: ActivatedRoute,
    private router: Router,
    private alert: AlertService,
  ) {
    this.form = this.fb.group({
      partyId: [null as number | null, [Validators.required]],
      billDate: ['', [Validators.required]],
      postingDate: ['', [Validators.required]],
      vendorBillRef: [''],
      billType: ['EXPENSE' as VendorBillType, [Validators.required]],
      paymentTerms: [30, [Validators.required, Validators.min(0)]],
      currencyCode: ['BDT', [Validators.required]],
      referenceType: ['MANUAL' as VendorBillReferenceType],
      referenceId: [''],
      notes: [''],
      items: this.fb.array([]),
    });
  }

  ngOnInit(): void {
    this.billId = Number(this.route.snapshot.paramMap.get('id')) || null;

    this.loadVendors();
    this.loadExpenseAccounts();

    if (this.billId) {
      this.loadBill(this.billId);
    } else {
      const today = new Date().toISOString().substring(0, 10);

      this.form.patchValue({
        billDate: today,
        postingDate: today,
      });

      this.addItem();
    }
  }

  get items(): FormArray {
    return this.form.get('items') as FormArray;
  }

  createItem(
    expenseAccountId: number | null = null,
    description = '',
    quantity = 1,
    unitPrice = 0,
    discountPercent = 0,
    vatRate = 0,
    tdsRate = 0,
    productId: number | null = null,
    costCenterId: number | null = null,
  ): FormGroup {
    return this.fb.group({
      productId: [productId],
      expenseAccountId: [expenseAccountId, [Validators.required]],
      costCenterId: [costCenterId],
      description: [description, [Validators.required]],
      quantity: [quantity, [Validators.required, Validators.min(0.01)]],
      unitPrice: [unitPrice, [Validators.required, Validators.min(0)]],
      discountPercent: [discountPercent, [Validators.min(0)]],
      vatRate: [vatRate, [Validators.min(0)]],
      tdsRate: [tdsRate, [Validators.min(0)]],
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

  loadVendors(): void {
    this.partyService.getByType('VENDOR').subscribe({
      next: (res) => {
        this.vendors.set(res.data.filter((p) => p.isActive));
      },
      error: () => {
        this.alert.error('Failed to load vendors');
      },
    });
  }

  loadExpenseAccounts(): void {
    this.accountService.search('', 'EXPENSE', true).subscribe({
      next: (res) => {
        this.expenseAccounts.set(res.data);
      },
      error: () => {
        this.alert.error('Failed to load expense accounts');
      },
    });
  }

  onVendorChange(): void {
    const partyId = this.form.get('partyId')?.value;
    const vendor = this.vendors().find((p) => p.id === Number(partyId));

    if (vendor) {
      this.form.patchValue({
        paymentTerms: vendor.paymentTerms ?? 30,
        currencyCode: vendor.currency ?? 'BDT',
      });
    }
  }

  onBillDateChange(): void {
    const billDate = this.form.get('billDate')?.value;

    if (billDate && !this.form.get('postingDate')?.value) {
      this.form.patchValue({ postingDate: billDate });
    }
  }

  loadBill(id: number): void {
    this.loading.set(true);

    this.vendorBillService.getById(id).subscribe({
      next: (res) => {
        const bill = res.data;

        if (bill.status !== 'DRAFT') {
          this.alert.warning('Only DRAFT vendor bills can be edited');
          this.router.navigate(['/vendor-bill']);
          return;
        }

        this.form.patchValue({
          partyId: bill.partyId,
          billDate: bill.billDate,
          postingDate: bill.postingDate,
          vendorBillRef: bill.vendorBillRef ?? '',
          billType: bill.billType,
          paymentTerms: bill.paymentTerms,
          currencyCode: bill.currencyCode,
          referenceType: bill.referenceType,
          referenceId: bill.referenceId ?? '',
          notes: bill.notes ?? '',
        });

        this.items.clear();

        bill.items.forEach((item) => {
          this.items.push(
            this.createItem(
              item.expenseAccountId,
              item.description,
              Number(item.quantity ?? 1),
              Number(item.unitPrice ?? 0),
              Number(item.discountPercent ?? 0),
              Number(item.vatRate ?? 0),
              Number(item.tdsRate ?? 0),
              item.productId,
              item.costCenterId,
            ),
          );
        });

        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.alert.error('Failed to load vendor bill');
        this.router.navigate(['/vendor-bill']);
      },
    });
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

  getLineTds(index: number): number {
    const subTotal = this.getLineSubTotal(index);
    const discount = this.getLineDiscount(index);
    const item = this.items.at(index) as FormGroup;
    const tdsRate = Number(item.get('tdsRate')?.value ?? 0);

    return ((subTotal - discount) * tdsRate) / 100;
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

  getTdsTotal(): number {
    return this.items.controls.reduce((sum, _, index) => sum + this.getLineTds(index), 0);
  }

  getGrandTotal(): number {
    return this.getSubTotal() - this.getDiscountTotal() + this.getVatTotal();
  }

  getNetPayable(): number {
    return this.getGrandTotal() - this.getTdsTotal();
  }

  submitDraft(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.alert.error('Please complete required fields');
      return;
    }

    this.submitting.set(true);

    const raw = this.form.getRawValue();

    const request: VendorBillRequest = {
      partyId: raw.partyId,
      billDate: raw.billDate,
      postingDate: raw.postingDate,
      vendorBillRef: raw.vendorBillRef ?? '',
      billType: raw.billType,
      paymentTerms: Number(raw.paymentTerms ?? 30),
      currencyCode: raw.currencyCode,
      referenceType: raw.referenceType,
      referenceId: raw.referenceId ?? '',
      notes: raw.notes ?? '',
      items: raw.items.map((item: any) => ({
        productId: item.productId ?? null,
        expenseAccountId: Number(item.expenseAccountId),
        costCenterId: item.costCenterId ?? null,
        description: item.description,
        quantity: Number(item.quantity ?? 0),
        unitPrice: Number(item.unitPrice ?? 0),
        discountPercent: Number(item.discountPercent ?? 0),
        vatRate: Number(item.vatRate ?? 0),
        tdsRate: Number(item.tdsRate ?? 0),
      })),
    };

    const apiCall = this.billId
      ? this.vendorBillService.update(this.billId, request)
      : this.vendorBillService.create(request);

    apiCall.subscribe({
      next: () => {
        this.submitting.set(false);
        this.alert.success(this.billId ? 'Vendor bill updated successfully' : 'Vendor bill saved as draft');
        this.router.navigate(['/vendor-bill']);
      },
      error: (error) => {
        this.submitting.set(false);
        this.alert.error(error?.error?.message ?? 'Failed to save vendor bill');
      },
    });
  }
}