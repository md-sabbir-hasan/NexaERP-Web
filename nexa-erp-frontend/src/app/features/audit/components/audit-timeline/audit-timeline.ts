import { DatePipe } from '@angular/common';
import { Component, Input, OnChanges, SimpleChanges, signal } from '@angular/core';
import {
  AuditAction,
  AuditTimelineEntityName,
  AuditTimelineItem,
} from '../../models/audit-log.model';
import { AuditLogService } from '../../services/audit-log.service';

@Component({
  selector: 'app-audit-timeline',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './audit-timeline.html',
  styleUrl: './audit-timeline.scss',
})
export class AuditTimeline implements OnChanges {
  @Input({ required: true }) entityName!: AuditTimelineEntityName;
  @Input({ required: true }) entityId!: number;

  readonly items = signal<AuditTimelineItem[]>([]);
  readonly loading = signal(false);
  readonly open = signal(false);
  readonly error = signal<string | null>(null);

  private loaded = false;

  constructor(private auditLogService: AuditLogService) {}

  ngOnChanges(changes: SimpleChanges): void {
    const entityChanged =
      (changes['entityId'] && !changes['entityId'].firstChange) ||
      (changes['entityName'] && !changes['entityName'].firstChange);

    if (entityChanged) {
      this.loaded = false;
      this.items.set([]);
      this.error.set(null);

      if (this.open()) {
        this.load();
      }
    }
  }

  toggle(): void {
    this.open.set(!this.open());

    if (this.open() && !this.loaded) {
      this.load();
    }
  }

  retry(): void {
    this.load();
  }

  load(): void {
    if (this.loading() || !this.entityId) {
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.auditLogService.getEntityTimeline(this.entityName, this.entityId, 0, 20).subscribe({
      next: (res) => {
        this.items.set(res.data.content);
        this.loading.set(false);
        this.loaded = true;
      },
      error: (error: unknown) => {
        this.error.set(this.getErrorMessage(error));
        this.loading.set(false);
      },
    });
  }

  actionClass(action: AuditAction): string {
    return action.toLowerCase();
  }

  private getErrorMessage(error: unknown): string {
    if (typeof error === 'object' && error !== null && 'error' in error) {
      const responseBody = error.error;

      if (
        typeof responseBody === 'object' &&
        responseBody !== null &&
        'message' in responseBody &&
        typeof responseBody.message === 'string'
      ) {
        return responseBody.message;
      }
    }

    return 'Failed to load activity history';
  }
}
