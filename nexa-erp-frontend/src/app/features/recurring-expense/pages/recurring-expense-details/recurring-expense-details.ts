import { CommonModule, DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AlertService } from '../../../../core/services/alert.service';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';
import {
  RecurringExpenseStatus,
  RecurringExpenseTemplateResponse,
} from '../../models/recurring-expense.model';
import { RecurringExpenseService } from '../../services/recurring-expense.service';

@Component({
  selector: 'app-recurring-expense-details',
  standalone: true,
  imports: [CommonModule, RouterLink, DatePipe, DecimalPipe],
  templateUrl: './recurring-expense-details.html',
  styleUrl: './recurring-expense-details.scss',
})
export class RecurringExpenseDetails implements OnInit {
  readonly loading = signal(false);
  readonly template = signal<RecurringExpenseTemplateResponse | null>(null);

  readonly pausing = signal(false);
  readonly resuming = signal(false);
  readonly running = signal(false);
  readonly deleting = signal(false);

  private templateId!: number;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private recurringExpenseService: RecurringExpenseService,
    private alert: AlertService,
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));

    if (!id) {
      this.router.navigate(['/recurring-expense']);
      return;
    }

    this.templateId = id;
    this.loadTemplate();
  }

  loadTemplate(): void {
    this.loading.set(true);

    this.recurringExpenseService.getById(this.templateId).subscribe({
      next: (res) => {
        this.template.set(res.data);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.alert.error('Failed to load recurring expense template');
        this.router.navigate(['/recurring-expense']);
      },
    });
  }

  async pause(): Promise<void> {
    const confirmed = await this.alert.confirm('Pause this recurring template?');
    if (!confirmed) return;

    this.pausing.set(true);

    this.recurringExpenseService.pause(this.templateId).subscribe({
      next: () => {
        this.pausing.set(false);
        this.alert.success('Template paused');
        this.loadTemplate();
      },
      error: (err) => {
        this.pausing.set(false);
        this.alert.error(err?.error?.message ?? 'Failed to pause template');
      },
    });
  }

  async resume(): Promise<void> {
    const confirmed = await this.alert.confirm('Resume this recurring template?');
    if (!confirmed) return;

    this.resuming.set(true);

    this.recurringExpenseService.resume(this.templateId).subscribe({
      next: () => {
        this.resuming.set(false);
        this.alert.success('Template resumed');
        this.loadTemplate();
      },
      error: (err) => {
        this.resuming.set(false);
        this.alert.error(err?.error?.message ?? 'Failed to resume template');
      },
    });
  }

  async runNow(): Promise<void> {
    const confirmed = await this.alert.confirm(
      'Generate an expense from this template right now? It will be created as a DRAFT for your review.',
    );
    if (!confirmed) return;

    this.running.set(true);

    this.recurringExpenseService.runNow(this.templateId).subscribe({
      next: () => {
        this.running.set(false);
        this.alert.success('Draft expense generated — review and post it from the Expenses page');
        this.loadTemplate();
      },
      error: (err) => {
        this.running.set(false);
        this.alert.error(err?.error?.message ?? 'Failed to generate expense');
      },
    });
  }

  async deleteTemplate(): Promise<void> {
    const confirmed = await this.alert.confirm(
      'Delete this recurring template? This cannot be undone.',
    );
    if (!confirmed) return;

    this.deleting.set(true);

    this.recurringExpenseService.delete(this.templateId).subscribe({
      next: () => {
        this.deleting.set(false);
        this.alert.success('Template deleted');
        this.router.navigate(['/recurring-expense']);
      },
      error: (err) => {
        this.deleting.set(false);
        this.alert.error(err?.error?.message ?? 'Failed to delete template');
      },
    });
  }

  getStatusClass(status: RecurringExpenseStatus): string {
    return status.toLowerCase();
  }

  frequencyLabel(t: RecurringExpenseTemplateResponse): string {
    const map: Record<string, string> = {
      WEEKLY: 'Every week',
      MONTHLY: 'Every month',
      QUARTERLY: 'Every quarter',
      YEARLY: 'Every year',
    };

    return map[t.frequency] ?? t.frequency;
  }
}
