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

  it('supports the PAYMENT_POSTED payload display metadata', () => {
    const value: NotificationResponse = {
      ...notification('PAYMENT', 12, '/payment/12'),
      type: 'PAYMENT_POSTED',
      priority: 'MEDIUM',
      module: 'PAYMENT',
    };

    expect(value.type).toBe('PAYMENT_POSTED');
    expect(getNotificationModule(value)).toBe('PAYMENT');
    expect(getNotificationModuleIcon(value)).toBe('bi-credit-card');
    expect(getNotificationPriority(value)).toBe('MEDIUM');
  });

  it('supports overdue invoice and vendor bill payload display metadata', () => {
    const invoice: NotificationResponse = {
      ...notification('INVOICE', 12, '/invoice/12'),
      type: 'INVOICE_OVERDUE',
      priority: 'HIGH',
      module: 'INVOICE',
    };
    const vendorBill: NotificationResponse = {
      ...notification('VENDOR_BILL', 13, '/vendor-bill/13'),
      type: 'VENDOR_BILL_OVERDUE',
      priority: 'HIGH',
      module: 'VENDOR_BILL',
    };

    expect(getSupportedNotificationRoute(invoice)).toBe('/invoice/12');
    expect(getNotificationModuleIcon(invoice)).toBe('bi-file-earmark-text');
    expect(getNotificationPriority(invoice)).toBe('HIGH');
    expect(getSupportedNotificationRoute(vendorBill)).toBe('/vendor-bill/13');
    expect(getNotificationModuleIcon(vendorBill)).toBe('bi-file-earmark-minus');
    expect(getNotificationPriority(vendorBill)).toBe('HIGH');
  });

  it.each(['APPROVAL_SUBMITTED','APPROVAL_APPROVED','APPROVAL_REJECTED','APPROVAL_RETURNED'] as const)(
    'supports safe %s approval navigation', (type) => {
      const value: NotificationResponse={...notification('APPROVAL_REQUEST',12,'/approvals/12'),type,module:'APPROVAL',priority:type==='APPROVAL_SUBMITTED'||type==='APPROVAL_APPROVED'?'MEDIUM':'HIGH'};
      expect(getSupportedNotificationRoute(value)).toBe('/approvals/12');
      expect(getNotificationModuleIcon(value)).toBe('bi-check2-square');
    });

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
    [notification('PAYMENT', 12, '/payment/12'), '/payment/12'],
    [notification('ACCOUNTING_PERIOD', 14, '/accounting-periods'), '/accounting-periods'],
    [notification('BUDGET', 15, '/budget/15/variance'), '/budget/15/variance'],
    [notification('BUDGET', null, '/budget'), '/budget'],
    [notification('APPROVAL_REQUEST', 12, '/approvals/12'), '/approvals/12'],
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
    notification('PAYMENT', 12, '/payment/99'),
    notification('PAYMENT', 12, '/payment/12/edit'),
    notification('PAYMENT', 12, '/payments/12'),
    notification('PAYMENT', 12, '/payment/0'),
    notification('PAYMENT', 12, '/payment/-12'),
    notification('PAYMENT', 12, '/payment/not-a-number'),
    notification('PAYMENT', 12, '/payment/12?tab=allocations'),
    notification('INVOICE', 12, '/payment/12'),
    notification('INVOICE', 12, 'data:text/html,test'),
    notification('SYSTEM', null, 'https://example.com'),
    notification('SYSTEM', null, '//example.com/path'),
    notification('SYSTEM', null, 'javascript:alert(1)'),
    notification('BANKING', 1, '/banking'),
    notification('APPROVAL_REQUEST', 12, '/approvals/99'),
    notification('APPROVAL_REQUEST', 12, '/approvals/12/edit'),
    notification('APPROVAL_REQUEST', 12, '/approval/12'),
    notification('APPROVAL_REQUEST', 12, '/approvals/12?tab=history'),
  ])('rejects mismatched, external, or unsupported routes', (value) => {
    expect(getSupportedNotificationRoute(value)).toBeNull();
  });
});
