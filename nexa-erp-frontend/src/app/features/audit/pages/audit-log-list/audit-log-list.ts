import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { ApiResponse } from '../../../../core/models/api-response.model';
import { PageResponse } from '../../../../core/models/page.model';
import {
  AUDIT_ENTITY_TYPES,
  AuditAction,
  AuditDiffRow,
  AuditEntityType,
  AuditLog,
} from '../../models/audit-log.model';
import { AuditLogService } from '../../services/audit-log.service';

@Component({
  selector: 'app-audit-log-list',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  templateUrl: './audit-log-list.html',
  styleUrl: './audit-log-list.scss',
})
export class AuditLogList implements OnInit {
  readonly entityTypes = AUDIT_ENTITY_TYPES;

  readonly logs = signal<AuditLog[]>([]);
  readonly loading = signal(false);
  readonly expandedId = signal<number | null>(null);

  entityType: AuditEntityType = 'INVOICE';
  entityId: number | null = null;
  userId: number | null = null;
  actionFilter: AuditAction | '' = '';

  page = 0;
  size = 20;
  totalElements = 0;
  totalPages = 0;

  constructor(private auditLogService: AuditLogService) {}

  ngOnInit(): void {
    this.loadLogs();
  }

  loadLogs(): void {
    this.loading.set(true);

    const request$: Observable<ApiResponse<PageResponse<AuditLog>>> = this.userId
      ? this.auditLogService.getUserActivity(this.userId, this.page, this.size)
      : this.entityId
        ? this.auditLogService.getEntityHistory(
            this.entityType,
            this.entityId,
            this.page,
            this.size,
          )
        : this.auditLogService.getEntityLogs(this.entityType, this.page, this.size);

    request$.subscribe({
      next: (res) => {
        this.logs.set(res.data.content);
        this.page = res.data.page;
        this.size = res.data.size;
        this.totalElements = res.data.totalElements;
        this.totalPages = res.data.totalPages;
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  // Action filter is applied client-side on the currently loaded page,
  // since the backend endpoints don't accept an action query param yet.
  get visibleLogs(): AuditLog[] {
    if (!this.actionFilter) return this.logs();
    return this.logs().filter((log) => log.action === this.actionFilter);
  }

  onFilterChange(): void {
    this.page = 0;
    this.loadLogs();
  }

  clearFilters(): void {
    this.entityId = null;
    this.userId = null;
    this.actionFilter = '';
    this.page = 0;
    this.loadLogs();
  }

  nextPage(): void {
    if (this.page + 1 < this.totalPages) {
      this.page++;
      this.loadLogs();
    }
  }

  previousPage(): void {
    if (this.page > 0) {
      this.page--;
      this.loadLogs();
    }
  }

  toggleExpand(log: AuditLog): void {
    this.expandedId.set(this.expandedId() === log.id ? null : log.id);
  }

  actionClass(action: string): string {
    return action.toLowerCase();
  }

  // Builds a field-by-field diff between oldValue and newValue JSON,
  // instead of dumping raw JSON at the user.
  diffRows(log: AuditLog): AuditDiffRow[] {
    const oldObj = this.safeParse(log.oldValue);
    const newObj = this.safeParse(log.newValue);

    if (!oldObj && !newObj) return [];

    const keys = new Set([...Object.keys(oldObj ?? {}), ...Object.keys(newObj ?? {})]);
    const rows: AuditDiffRow[] = [];

    keys.forEach((key) => {
      const ov = oldObj?.[key];
      const nv = newObj?.[key];
      if (JSON.stringify(ov) !== JSON.stringify(nv)) {
        rows.push({
          field: key,
          oldVal: ov === undefined || ov === null ? '—' : String(ov),
          newVal: nv === undefined || nv === null ? '—' : String(nv),
        });
      }
    });

    return rows;
  }

  private safeParse(value: string | null): Record<string, unknown> | null {
    if (!value) return null;
    try {
      const parsed = JSON.parse(value);
      return typeof parsed === 'object' && parsed !== null ? parsed : null;
    } catch {
      return null;
    }
  }
}
