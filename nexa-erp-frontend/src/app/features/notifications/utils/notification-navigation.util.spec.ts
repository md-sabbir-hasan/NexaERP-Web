import { NotificationResponse } from '../models/notification.model';
import {
  getNotificationModule,
  getNotificationModuleIcon,
  getNotificationPriority,
} from './notification-display.util';
import { getSupportedNotificationRoute } from './notification-navigation.util';

function notification(
  entityType: string,
  entityId: number | null,
  route: string | null,
): NotificationResponse {
  return {
    id: 1,
    type: 'SYSTEM',
    title: 'Test',
    message: 'Test notification',
    route,
    entityType,
    entityId,
    read: false,
    readAt: null,
    expiresAt: null,
    createdAt: '2026-07-30T10:00:00',
  };
}

describe('notification utilities', () => {
  it.each(['INVOICE_POSTED', 'INVOICE_CANCELLED'] as const)(
    'supports the %s notification payload type',
    (type) => {
      const value: NotificationResponse = {
        ...notification('INVOICE', 12, '/invoice/12'),
        type,
        priority: type === 'INVOICE_CANCELLED' ? 'HIGH' : 'MEDIUM',
        module: 'INVOICE',
      };

      expect(value.type).toBe(type);
      expect(getNotificationModule(value)).toBe('INVOICE');
      expect(getNotificationModuleIcon(value)).toBe('bi-file-earmark-text');
      expect(getNotificationPriority(value)).toBe(
        type === 'INVOICE_CANCELLED' ? 'HIGH' : 'MEDIUM',
      );
    },
  );

  it.each(['VENDOR_BILL_POSTED', 'VENDOR_BILL_CANCELLED'] as const)(
    'supports the %s notification payload type',
    (type) => {
      const value: NotificationResponse = {
        ...notification('VENDOR_BILL', 12, '/vendor-bill/12'),
        type,
        priority: type === 'VENDOR_BILL_CANCELLED' ? 'HIGH' : 'MEDIUM',
        module: 'VENDOR_BILL',
      };

      expect(value.type).toBe(type);
      expect(getNotificationModule(value)).toBe('VENDOR_BILL');
      expect(getNotificationModuleIcon(value)).toBe('bi-file-earmark-minus');
      expect(getNotificationPriority(value)).toBe(
        type === 'VENDOR_BILL_CANCELLED' ? 'HIGH' : 'MEDIUM',
      );
    },
  );

  it('defaults old payloads to MEDIUM priority and SYSTEM module', () => {
    const oldPayload = notification('SYSTEM', null, null);

    expect(getNotificationPriority(oldPayload)).toBe('MEDIUM');
    expect(getNotificationModule(oldPayload)).toBe('SYSTEM');
  });

  it.each([
    [notification('JOURNAL', 12, '/journals/12/edit'), '/journals/12/edit'],
    [notification('EXPENSE', 13, '/expense/13'), '/expense/13'],
    [notification('INVOICE', 12, '/invoice/12'), '/invoice/12'],
    [notification('VENDOR_BILL', 12, '/vendor-bill/12'), '/vendor-bill/12'],
    [notification('ACCOUNTING_PERIOD', 14, '/accounting-periods'), '/accounting-periods'],
    [notification('BUDGET', 15, '/budget/15/variance'), '/budget/15/variance'],
    [notification('BUDGET', null, '/budget'), '/budget'],
  ])('accepts an allowlisted internal route', (value, expected) => {
    expect(getSupportedNotificationRoute(value)).toBe(expected);
  });

  it.each([
    notification('JOURNAL', 12, '/journals/99/edit'),
    notification('EXPENSE', 13, '/expense/99'),
    notification('INVOICE', 12, '/invoice/99'),
    notification('INVOICE', 12, '/invoice/12/edit'),
    notification('INVOICE', 12, '/invoices/12'),
    notification('PAYMENT', 12, '/invoice/12'),
    notification('INVOICE', 12, '/invoice/12?tab=payments'),
    notification('VENDOR_BILL', 12, '/vendor-bill/99'),
    notification('VENDOR_BILL', 12, '/vendor-bill/12/edit'),
    notification('VENDOR_BILL', 12, '/vendor-bills/12'),
    notification('VENDOR_BILL', 12, '/vendor-bill/0'),
    notification('VENDOR_BILL', 12, '/vendor-bill/not-a-number'),
    notification('VENDOR_BILL', 12, '/vendor-bill/12?tab=payments'),
    notification('PAYMENT', 12, '/vendor-bill/12'),
    notification('INVOICE', 12, 'data:text/html,test'),
    notification('SYSTEM', null, 'https://example.com'),
    notification('SYSTEM', null, '//example.com/path'),
    notification('SYSTEM', null, 'javascript:alert(1)'),
    notification('BANKING', 1, '/banking'),
  ])('rejects mismatched, external, or unsupported routes', (value) => {
    expect(getSupportedNotificationRoute(value)).toBeNull();
  });
});
