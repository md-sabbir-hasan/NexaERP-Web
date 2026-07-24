import { CommonModule, DatePipe } from '@angular/common';
import { Component, Input, OnChanges, SimpleChanges, signal } from '@angular/core';
import { AuditLog } from '../../models/audit-log.model';
import { AuditLogService } from '../../services/audit-log.service';

// Usage in any detail page/component:
//   <app-audit-timeline [entityName]="'INVOICE'" [entityId]="invoice.id"></app-audit-timeline>
@Component({
  selector: 'app-audit-timeline',
  standalone: true,
  imports: [CommonModule, DatePipe],
  templateUrl: './audit-timeline.html',
  styleUrl: './audit-timeline.scss',
})
export class AuditTimeline implements OnChanges {
  @Input({ required: true }) entityName!: string;
  @Input({ required: true }) entityId!: number;

  readonly logs = signal<AuditLog[]>([]);
  readonly loading = signal(false);
  readonly open = signal(false);

  private loaded = false;

  constructor(private auditLogService: AuditLogService) {}

  ngOnChanges(changes: SimpleChanges): void {
    // Reset if the parent navigates to a different record while this component stays mounted
    if (changes['entityId'] && !changes['entityId'].firstChange) {
      this.loaded = false;
      this.logs.set([]);
    }
  }

  toggle(): void {
    this.open.set(!this.open());
    if (this.open() && !this.loaded) {
      this.load();
    }
  }

  private load(): void {
    this.loading.set(true);
    this.auditLogService.getEntityHistory(this.entityName, this.entityId, 0, 50).subscribe({
      next: (res) => {
        this.logs.set(res.data.content);
        this.loading.set(false);
        this.loaded = true;
      },
      error: () => this.loading.set(false),
    });
  }

  actionClass(action: string): string {
    return action.toLowerCase();
  }
}
