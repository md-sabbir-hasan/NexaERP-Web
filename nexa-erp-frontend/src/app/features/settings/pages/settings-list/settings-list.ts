import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { Account } from '../../../accounts/models/account.model';
import { AccountService } from '../../../accounts/services/account.service';
import { AlertService } from '../../../../core/services/alert.service';
import {
  ACCOUNT_MAPPED_KEYS,
  SETTING_LABELS,
  SettingKey,
  SystemSetting,
} from '../../models/setting.model';
import { SettingService } from '../../services/setting.service';

interface MappingRow {
  key: SettingKey;
  label: string;
  description: string | null;
  currentAccountId: number | null;
  selectedAccountId: number | null;
  updatedAt: string | null;
  saving: boolean;
}

@Component({
  selector: 'app-settings-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './settings-list.html',
  styleUrl: './settings-list.scss',
})
export class SettingsList implements OnInit {
  readonly labels = SETTING_LABELS;

  readonly accounts = signal<Account[]>([]);
  readonly mappingRows = signal<MappingRow[]>([]);
  readonly loading = signal(false);

  // Enum keys the backend hasn't wired an update path for yet - shown as
  // read-only "coming soon" so users aren't misled into thinking they're editable.
  readonly comingSoonKeys: SettingKey[] = [
    'COMPANY_NAME',
    'DEFAULT_CURRENCY',
    'FINANCIAL_YEAR',
    'DECIMAL_PRECISION',
    'TIMEZONE',
    'DATE_FORMAT',
    'AUTO_POST_INVOICE',
    'ALLOW_NEGATIVE_STOCK',
    'DEFAULT_WAREHOUSE',
  ];

  constructor(
    private settingService: SettingService,
    private accountService: AccountService,
    private alertService: AlertService,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);

    forkJoin({
      settings: this.settingService.getAll(),
      accounts: this.accountService.getAll(),
    }).subscribe({
      next: ({ settings, accounts }) => {
        this.accounts.set(accounts.data);

        const settingsByKey = new Map<SettingKey, SystemSetting>(
          settings.data.map((s) => [s.key, s]),
        );

        this.mappingRows.set(
          ACCOUNT_MAPPED_KEYS.map((key) => {
            const setting = settingsByKey.get(key);
            const accountId = setting ? Number(setting.value) : null;
            return {
              key,
              label: SETTING_LABELS[key],
              description: setting?.description ?? null,
              currentAccountId: accountId,
              selectedAccountId: accountId,
              updatedAt: setting?.updatedAt ?? null,
              saving: false,
            };
          }),
        );

        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  accountLabel(accountId: number | null): string {
    if (accountId === null) return 'Not set';
    const account = this.accounts().find((a) => a.id === accountId);
    return account ? `${account.code} · ${account.name}` : `#${accountId}`;
  }

  isDirty(row: MappingRow): boolean {
    return row.selectedAccountId !== row.currentAccountId;
  }

  save(row: MappingRow): void {
    if (row.selectedAccountId === null || !this.isDirty(row)) return;

    const accountId = row.selectedAccountId;
    this.patchRow(row.key, { saving: true });

    this.settingService.updateAccountMapping(row.key, accountId).subscribe({
      next: () => {
        this.patchRow(row.key, { saving: false, currentAccountId: accountId });
        this.alertService.success(`${row.label} updated`);
      },
      error: (err) => {
        this.patchRow(row.key, { saving: false });
        this.alertService.error(err?.error?.message ?? 'Could not update this setting');
      },
    });
  }

  reset(row: MappingRow): void {
    this.patchRow(row.key, { selectedAccountId: row.currentAccountId });
  }

  
  private patchRow(key: SettingKey, changes: Partial<MappingRow>): void {
    this.mappingRows.update((rows) =>
      rows.map((r) => (r.key === key ? { ...r, ...changes } : r)),
    );
  }
}