import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AlertService } from '../../../../core/services/alert.service';
import { Party, PartyType } from '../../models/party.model';
import { PartyService } from '../../services/party.service';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-party-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, HasPermissionDirective],
  templateUrl: './party-list.html',
  styleUrl: './party-list.scss',
})
export class PartyList implements OnInit {
  readonly loading = signal(false);
  readonly parties = signal<Party[]>([]);

  readonly searchText = signal('');
  readonly selectedType = signal<PartyType | ''>('');
  readonly selectedStatus = signal<'ACTIVE' | 'INACTIVE' | ''>('');

  readonly partyTypes: PartyType[] = ['CUSTOMER', 'VENDOR', 'BOTH'];

  readonly filteredParties = computed(() => {
    const keyword = this.searchText().trim().toLowerCase();

    return this.parties().filter((party) => {
      const matchSearch =
        !keyword ||
        party.code.toLowerCase().includes(keyword) ||
        party.name.toLowerCase().includes(keyword) ||
        (party.companyName ?? '').toLowerCase().includes(keyword) ||
        (party.phone ?? '').toLowerCase().includes(keyword);

      const matchType = !this.selectedType() || party.type === this.selectedType();

      const matchStatus =
        !this.selectedStatus() ||
        (this.selectedStatus() === 'ACTIVE' && party.isActive) ||
        (this.selectedStatus() === 'INACTIVE' && !party.isActive);

      return matchSearch && matchType && matchStatus;
    });
  });

  readonly totalParties = computed(() => this.parties().length);

  readonly customerCount = computed(
    () => this.parties().filter((p) => p.type === 'CUSTOMER' || p.type === 'BOTH').length,
  );

  readonly vendorCount = computed(
    () => this.parties().filter((p) => p.type === 'VENDOR' || p.type === 'BOTH').length,
  );

  readonly inactiveCount = computed(() => this.parties().filter((p) => !p.isActive).length);

  constructor(
    private readonly partyService: PartyService,
    private readonly alert: AlertService,
  ) {}

  ngOnInit(): void {
    this.loadParties();
  }

  loadParties(): void {
    this.loading.set(true);

    this.partyService.getAll().subscribe({
      next: (res) => {
        this.parties.set(res.data);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.alert.error('Failed to load parties');
      },
    });
  }

  clearFilters(): void {
    this.searchText.set('');
    this.selectedType.set('');
    this.selectedStatus.set('');
  }

  deactivate(party: Party): void {
    if (!confirm(`Deactivate ${party.name}?`)) {
      return;
    }

    this.partyService.deactivate(party.id).subscribe({
      next: () => {
        this.alert.success('Party deactivated');

        this.loadParties();
      },

      error: (err) => {
        this.alert.error(err?.error?.message ?? 'Failed to deactivate party');
      },
    });
  }

  activate(party: Party): void {
    this.partyService.activate(party.id).subscribe({
      next: () => {
        this.alert.success('Party activated');

        this.loadParties();
      },

      error: (err) => {
        this.alert.error(err?.error?.message ?? 'Failed to activate party');
      },
    });
  }

  getTypeClass(type: PartyType): string {
    switch (type) {
      case 'CUSTOMER':
        return 'customer';

      case 'VENDOR':
        return 'vendor';

      default:
        return 'both';
    }
  }

  getStatusClass(active: boolean): string {
    return active ? 'active' : 'inactive';
  }
}