import { NotificationResponse } from '../models/notification.model';

const BUDGET_LIST_ROUTE = '/budget';
const BUDGET_VARIANCE_ROUTE = /^\/budget\/([1-9]\d*)\/variance$/;

export function getSupportedNotificationRoute(
  notification: NotificationResponse,
): string | null {
  const route = notification.route?.trim();

  if (!route || notification.entityType !== 'BUDGET') {
    return null;
  }

  if (route === BUDGET_LIST_ROUTE && notification.entityId === null) {
    return route;
  }

  const varianceMatch = route.match(BUDGET_VARIANCE_ROUTE);
  const routeEntityId = varianceMatch ? Number(varianceMatch[1]) : null;

  return routeEntityId !== null && routeEntityId === notification.entityId ? route : null;
}
