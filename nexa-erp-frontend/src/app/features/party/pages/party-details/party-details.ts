import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { APP_CONFIG } from '../../../../core/config/app.config';
import { AlertService } from '../../../../core/services/alert.service';
import { Party } from '../../models/party.model';
import { PartyService } from '../../services/party.service';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

type PartyDocumentType = 'tradeLicense' | 'binCertificate' | 'tinCertificate' | 'nid';

interface PartyDocumentCard {
  key: PartyDocumentType;
  title: string;
  url: string | null;
}

@Component({
  selector: 'app-party-details',
  standalone: true,
  imports: [CommonModule, RouterLink, DatePipe, DecimalPipe, HasPermissionDirective],
  templateUrl: './party-details.html',
  styleUrl: './party-details.scss',
})
export class PartyDetails implements OnInit {
  readonly loading = signal(false);
  readonly uploading = signal<PartyDocumentType | null>(null);
  readonly party = signal<Party | null>(null);

  private partyId!: number;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private partyService: PartyService,
    private alert: AlertService,
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));

    if (!id) {
      this.router.navigate(['/party']);
      return;
    }

    this.partyId = id;
    this.loadParty();
  }

  loadParty(): void {
    this.loading.set(true);

    this.partyService.getById(this.partyId).subscribe({
      next: (res) => {
        this.party.set(res.data);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.alert.error('Failed to load party');
        this.router.navigate(['/party']);
      },
    });
  }

  getDocuments(): PartyDocumentCard[] {
    const party = this.party();

    return [
      {
        key: 'tradeLicense',
        title: 'Trade License',
        url: party?.tradeLicenseUrl ?? null,
      },
      {
        key: 'binCertificate',
        title: 'BIN Certificate',
        url: party?.binCertificateUrl ?? null,
      },
      {
        key: 'tinCertificate',
        title: 'TIN Certificate',
        url: party?.tinCertificateUrl ?? null,
      },
      {
        key: 'nid',
        title: 'NID',
        url: party?.nidUrl ?? null,
      },
    ];
  }

  uploadDocument(event: Event, type: PartyDocumentType): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    if (!file) return;

    this.uploading.set(type);

    const request$ =
      type === 'tradeLicense'
        ? this.partyService.uploadTradeLicense(this.partyId, file)
        : type === 'binCertificate'
          ? this.partyService.uploadBinCertificate(this.partyId, file)
          : type === 'tinCertificate'
            ? this.partyService.uploadTinCertificate(this.partyId, file)
            : this.partyService.uploadNid(this.partyId, file);

    request$.subscribe({
      next: () => {
        this.uploading.set(null);
        this.alert.success('Document uploaded successfully');
        input.value = '';
        this.loadParty();
      },
      error: (error) => {
        this.uploading.set(null);
        this.alert.error(error?.error?.message ?? 'Failed to upload document');
        input.value = '';
      },
    });
  }

  openDocument(url: string | null): void {
    if (!url) return;
    window.open(this.getFileUrl(url), '_blank');
  }

  getFileUrl(url: string | null): string {
    if (!url) return '#';

    if (url.startsWith('http')) {
      return url;
    }

    return `${APP_CONFIG.apiUrl.replace('/api', '')}${url}`;
  }

  getStatusClass(): string {
    return this.party()?.isActive ? 'active' : 'inactive';
  }

  getTypeClass(): string {
    return (this.party()?.type ?? 'CUSTOMER').toLowerCase();
  }
}
