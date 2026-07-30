import { NotificationResponse } from '../models/notification.model';
import { getNotificationModule, getNotificationPriority } from './notification-display.util';
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
  it('defaults old payloads to MEDIUM priority and SYSTEM module', () => {
    const oldPayload = notification('SYSTEM', null, null);

    expect(getNotificationPriority(oldPayload)).toBe('MEDIUM');
    expect(getNotificationModule(oldPayload)).toBe('SYSTEM');
  });

  it.each([
    [notification('JOURNAL', 12, '/journals/12/edit'), '/journals/12/edit'],
    [notification('EXPENSE', 13, '/expense/13'), '/expense/13'],
    [notification('ACCOUNTING_PERIOD', 14, '/accounting-periods'), '/accounting-periods'],
    [notification('BUDGET', 15, '/budget/15/variance'), '/budget/15/variance'],
    [notification('BUDGET', null, '/budget'), '/budget'],
  ])('accepts an allowlisted internal route', (value, expected) => {
    expect(getSupportedNotificationRoute(value)).toBe(expected);
  });

  it.each([
    notification('JOURNAL', 12, '/journals/99/edit'),
    notification('EXPENSE', 13, '/expense/99'),
    notification('SYSTEM', null, 'https://example.com'),
    notification('SYSTEM', null, '//example.com/path'),
    notification('SYSTEM', null, 'javascript:alert(1)'),
    notification('BANKING', 1, '/banking'),
  ])('rejects mismatched, external, or unsupported routes', (value) => {
    expect(getSupportedNotificationRoute(value)).toBeNull();
  });
});
