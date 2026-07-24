import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AlertService } from '../../../../core/services/alert.service';
import { PartyType } from '../../models/party.model';
import { PartyRequest, PartyService } from '../../services/party.service';

@Component({
  selector: 'app-party-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './party-form.html',
  styleUrl: './party-form.scss',
})
export class PartyForm implements OnInit {
  readonly loading = signal(false);
  readonly submitting = signal(false);

  partyId: number | null = null;

  readonly partyTypes: PartyType[] = ['CUSTOMER', 'VENDOR', 'BOTH'];

  readonly form: FormGroup;

  constructor(
    private fb: NonNullableFormBuilder,
    private partyService: PartyService,
    private route: ActivatedRoute,
    private router: Router,
    private alert: AlertService,
  ) {
    this.form = this.fb.group({
      name: ['', [Validators.required]],
      type: ['CUSTOMER' as PartyType, [Validators.required]],
      notes: [''],

      companyName: [''],
      contactPerson: [''],
      jobPosition: [''],

      email: [''],
      phone: ['', [Validators.required]],
      mobile: [''],

      street: [''],
      city: [''],
      state: [''],
      country: ['Bangladesh'],

      creditLimit: [0],
      paymentTerms: [30, [Validators.required, Validators.min(0)]],
      openingBalance: [0],
      currency: ['BDT'],

      bankAccountNo: [''],
      bankName: [''],

      bin: [''],
      tin: [''],
      vatRegistered: [false],

      tradeLicenseNo: [''],
      tradeLicenseExpiry: [''],
      binCertificateNo: [''],
      tinCertificateNo: [''],
      nidNo: [''],
    });
  }

  ngOnInit(): void {
    this.partyId = Number(this.route.snapshot.paramMap.get('id')) || null;

    if (this.partyId) {
      this.loadParty(this.partyId);
    }
  }

  loadParty(id: number): void {
    this.loading.set(true);

    this.partyService.getById(id).subscribe({
      next: (res) => {
        const party = res.data;

        this.form.patchValue({
          name: party.name,
          type: party.type,
          notes: party.notes ?? '',

          companyName: party.companyName ?? '',
          contactPerson: party.contactPerson ?? '',
          jobPosition: party.jobPosition ?? '',

          email: party.email ?? '',
          phone: party.phone ?? '',
          mobile: party.mobile ?? '',

          street: party.street ?? '',
          city: party.city ?? '',
          state: party.state ?? '',
          country: party.country ?? 'Bangladesh',

          creditLimit: party.creditLimit ?? 0,
          paymentTerms: party.paymentTerms ?? 30,
          openingBalance: party.openingBalance ?? 0,
          currency: party.currency ?? 'BDT',

          bankAccountNo: party.bankAccountNo ?? '',
          bankName: party.bankName ?? '',

          bin: party.bin ?? '',
          tin: party.tin ?? '',
          vatRegistered: party.vatRegistered ?? false,

          tradeLicenseNo: party.tradeLicenseNo ?? '',
          tradeLicenseExpiry: party.tradeLicenseExpiry ?? '',
          binCertificateNo: party.binCertificateNo ?? '',
          tinCertificateNo: party.tinCertificateNo ?? '',
          nidNo: party.nidNo ?? '',
        });

        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.alert.error('Failed to load party');
        this.router.navigate(['/party']);
      },
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.alert.error('Please complete required fields');
      return;
    }

    const raw = this.form.getRawValue();

    const request: PartyRequest = {
      name: raw.name,
      type: raw.type,
      notes: raw.notes ?? null,

      companyName: raw.companyName ?? null,
      contactPerson: raw.contactPerson ?? null,
      jobPosition: raw.jobPosition ?? null,

      email: raw.email ?? null,
      phone: raw.phone,
      mobile: raw.mobile ?? null,

      street: raw.street ?? null,
      city: raw.city ?? null,
      state: raw.state ?? null,
      country: raw.country ?? 'Bangladesh',

      creditLimit: Number(raw.creditLimit ?? 0),
      paymentTerms: Number(raw.paymentTerms ?? 30),
      openingBalance: Number(raw.openingBalance ?? 0),
      currency: raw.currency ?? 'BDT',

      bankAccountNo: raw.bankAccountNo ?? null,
      bankName: raw.bankName ?? null,

      bin: raw.bin ?? null,
      tin: raw.tin ?? null,
      vatRegistered: Boolean(raw.vatRegistered),

      tradeLicenseNo: raw.tradeLicenseNo ?? null,
      tradeLicenseExpiry: raw.tradeLicenseExpiry || null,
      binCertificateNo: raw.binCertificateNo ?? null,
      tinCertificateNo: raw.tinCertificateNo ?? null,
      nidNo: raw.nidNo ?? null,
    };

    this.submitting.set(true);

    const request$ = this.partyId
      ? this.partyService.update(this.partyId, request)
      : this.partyService.create(request);

    request$.subscribe({
      next: (res) => {
        this.submitting.set(false);
        this.alert.success(
          this.partyId ? 'Party updated successfully' : 'Party created successfully',
        );
        this.router.navigate(['/party', res.data.id]);
      },
      error: (error) => {
        this.submitting.set(false);
        this.alert.error(error?.error?.message ?? 'Failed to save party');
      },
    });
  }
}
