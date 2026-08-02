import {
  NotificationModule,
  NotificationPriority,
  NotificationResponse,
} from '../models/notification.model';

export function getNotificationPriority(
  notification: NotificationResponse,
): NotificationPriority {
  return notification.priority ?? 'MEDIUM';
}

export function getNotificationModule(
  notification: NotificationResponse,
): NotificationModule {
  return notification.module ?? 'SYSTEM';
}

export function getNotificationModuleLabel(notification: NotificationResponse): string {
  return getNotificationModule(notification).replaceAll('_', ' ');
}

export function getNotificationModuleIcon(notification: NotificationResponse): string {
  const icons: Record<NotificationModule, string> = {
    SYSTEM: 'bi-bell',
    BUDGET: 'bi-pie-chart',
    JOURNAL: 'bi-journal-text',
    EXPENSE: 'bi-receipt',
    ACCOUNTING_PERIOD: 'bi-calendar-check',
    INVOICE: 'bi-file-earmark-text',
    VENDOR_BILL: 'bi-file-earmark-minus',
    PAYMENT: 'bi-credit-card',
    BANKING: 'bi-bank',
    FIXED_ASSET: 'bi-building',
    APPROVAL: 'bi-check2-square',
  };
  return icons[getNotificationModule(notification)];
}
