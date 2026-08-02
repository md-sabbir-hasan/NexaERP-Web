import { NotificationResponse } from '../models/notification.model';

const BUDGET_LIST_ROUTE = '/budget';
const BUDGET_VARIANCE_ROUTE = /^\/budget\/([1-9]\d*)\/variance$/;
const JOURNAL_EDIT_ROUTE = /^\/journals\/([1-9]\d*)\/edit$/;
const EXPENSE_DETAIL_ROUTE = /^\/expense\/([1-9]\d*)$/;
const INVOICE_DETAIL_ROUTE = /^\/invoice\/([1-9]\d*)$/;
const VENDOR_BILL_DETAIL_ROUTE = /^\/vendor-bill\/([1-9]\d*)$/;
const PAYMENT_DETAIL_ROUTE = /^\/payment\/([1-9]\d*)$/;
const ACCOUNTING_PERIOD_LIST_ROUTE = '/accounting-periods';
const APPROVAL_DETAIL_ROUTE = /^\/approvals\/([1-9]\d*)$/;

export function getSupportedNotificationRoute(
  notification: NotificationResponse,
): string | null {
  const route = notification.route?.trim();

  if (!route || !route.startsWith('/') || route.startsWith('//')) {
    return null;
  }

  switch (notification.entityType) {
    case 'BUDGET':
      return getBudgetRoute(route, notification.entityId);
    case 'JOURNAL':
      return getEntityRoute(route, notification.entityId, JOURNAL_EDIT_ROUTE);
    case 'EXPENSE':
      return getEntityRoute(route, notification.entityId, EXPENSE_DETAIL_ROUTE);
    case 'INVOICE':
      return getEntityRoute(route, notification.entityId, INVOICE_DETAIL_ROUTE);
    case 'VENDOR_BILL':
      return getEntityRoute(route, notification.entityId, VENDOR_BILL_DETAIL_ROUTE);
    case 'PAYMENT':
      return getEntityRoute(route, notification.entityId, PAYMENT_DETAIL_ROUTE);
    case 'ACCOUNTING_PERIOD':
      return route === ACCOUNTING_PERIOD_LIST_ROUTE && notification.entityId !== null
        ? route
        : null;
    case 'APPROVAL_REQUEST':
      return getEntityRoute(route, notification.entityId, APPROVAL_DETAIL_ROUTE);
    default:
      return null;
  }
}

function getBudgetRoute(route: string, entityId: number | null): string | null {
  if (route === BUDGET_LIST_ROUTE && entityId === null) {
    return route;
  }
  const varianceMatch = route.match(BUDGET_VARIANCE_ROUTE);
  const routeEntityId = varianceMatch ? Number(varianceMatch[1]) : null;
  return routeEntityId !== null && routeEntityId === entityId ? route : null;
}

function getEntityRoute(
  route: string,
  entityId: number | null,
  routePattern: RegExp,
): string | null {
  const match = route.match(routePattern);
  const routeEntityId = match ? Number(match[1]) : null;
  return routeEntityId !== null && routeEntityId === entityId ? route : null;
}
