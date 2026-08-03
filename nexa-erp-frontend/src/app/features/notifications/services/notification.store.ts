import { computed, Injectable, signal } from '@angular/core';
import { EMPTY, Observable, catchError, finalize, map, tap } from 'rxjs';

import { AlertService } from '../../../core/services/alert.service';
import { NotificationResponse } from '../models/notification.model';
import { NotificationApiService } from './notification-api.service';

@Injectable({
  providedIn: 'root',
})
export class NotificationStore {
  readonly notifications = signal<NotificationResponse[]>([]);
  readonly unreadCount = signal(0);
  readonly loading = signal(false);
  readonly loadingMore = signal(false);
  readonly loaded = signal(false);
  readonly error = signal<string | null>(null);
  readonly currentPage = signal(0);
  readonly totalPages = signal(0);
  readonly last = signal(true);
  readonly unreadOnly = signal(false);
  readonly markingReadIds = signal<ReadonlySet<number>>(new Set<number>());
  readonly markingAllRead = signal(false);
  readonly dashboardPreview = signal<NotificationResponse[]>([]);
  readonly dashboardPreviewLoading = signal(false);
  readonly dashboardPreviewError = signal<string | null>(null);

  readonly hasUnread = computed(() => this.unreadCount() > 0);
  readonly canLoadMore = computed(
    () =>
      this.loaded() &&
      !this.loading() &&
      !this.loadingMore() &&
      !this.last() &&
      this.currentPage() + 1 < this.totalPages(),
  );
  readonly isEmpty = computed(
    () => this.loaded() && !this.loading() && this.notifications().length === 0,
  );

  private unreadCountLoading = false;

  constructor(
    private notificationApi: NotificationApiService,
    private alert: AlertService,
  ) {}

  loadUnreadCount(): void {
    if (this.unreadCountLoading) {
      return;
    }

    this.unreadCountLoading = true;

    this.notificationApi.getUnreadCount().subscribe({
      next: (response) => {
        this.unreadCount.set(Math.max(0, response.data));
        this.unreadCountLoading = false;
      },
      error: () => {
        this.unreadCountLoading = false;
      },
    });
  }

  loadDashboardPreview(): void {
    if (this.dashboardPreviewLoading()) {
      return;
    }

    this.dashboardPreviewLoading.set(true);
    this.dashboardPreviewError.set(null);
    this.notificationApi.getNotifications(0, 3, true).subscribe({
      next: (response) => {
        this.dashboardPreview.set(response.data.content.filter((item) => Boolean(item.route)).slice(0, 3));
        this.dashboardPreviewLoading.set(false);
      },
      error: (error: unknown) => {
        this.dashboardPreviewError.set(this.getErrorMessage(error, 'Failed to load notifications'));
        this.dashboardPreviewLoading.set(false);
      },
    });
  }

  loadFirstPage(unreadOnly = this.unreadOnly()): void {
    if (this.loading() || this.loadingMore()) {
      return;
    }

    const filterChanged = unreadOnly !== this.unreadOnly();

    if (filterChanged) {
      this.notifications.set([]);
      this.loaded.set(false);
      this.currentPage.set(0);
      this.totalPages.set(0);
      this.last.set(true);
    }

    this.unreadOnly.set(unreadOnly);
    this.loading.set(true);
    this.error.set(null);

    this.notificationApi.getNotifications(0, 20, unreadOnly).subscribe({
      next: (response) => {
        const page = response.data;

        this.notifications.set(page.content);
        this.currentPage.set(page.page);
        this.totalPages.set(page.totalPages);
        this.last.set(page.last ?? page.page + 1 >= page.totalPages);
        this.loaded.set(true);
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(this.getErrorMessage(error, 'Failed to load notifications'));
        this.loading.set(false);
      },
    });
  }

  loadMore(): void {
    if (!this.canLoadMore()) {
      return;
    }

    this.loadingMore.set(true);
    this.error.set(null);

    const nextPage = this.currentPage() + 1;

    this.notificationApi.getNotifications(nextPage, 20, this.unreadOnly()).subscribe({
      next: (response) => {
        const page = response.data;
        const existingIds = new Set(this.notifications().map((notification) => notification.id));
        const newNotifications = page.content.filter(
          (notification) => !existingIds.has(notification.id),
        );

        this.notifications.update((notifications) => [...notifications, ...newNotifications]);
        this.currentPage.set(page.page);
        this.totalPages.set(page.totalPages);
        this.last.set(page.last ?? page.page + 1 >= page.totalPages);
        this.loadingMore.set(false);
      },
      error: (error: unknown) => {
        this.error.set(this.getErrorMessage(error, 'Failed to load more notifications'));
        this.loadingMore.set(false);
      },
    });
  }

  setFilter(unreadOnly: boolean): void {
    if (unreadOnly === this.unreadOnly() && this.loaded()) {
      return;
    }

    this.loadFirstPage(unreadOnly);
  }

  markAsRead(notification: NotificationResponse): Observable<NotificationResponse> {
    if (notification.read || this.markingReadIds().has(notification.id)) {
      return EMPTY;
    }

    this.markingReadIds.update((ids) => {
      const nextIds = new Set(ids);
      nextIds.add(notification.id);
      return nextIds;
    });

    return this.notificationApi.markAsRead(notification.id).pipe(
      tap((response) => {
        const updatedNotification = response.data;

        if (this.unreadOnly()) {
          this.notifications.update((notifications) =>
            notifications.filter((item) => item.id !== updatedNotification.id),
          );
        } else {
          this.notifications.update((notifications) =>
            notifications.map((item) =>
              item.id === updatedNotification.id ? updatedNotification : item,
            ),
          );
        }

        this.loadUnreadCount();
      }),
      catchError((error: unknown) => {
        this.alert.error(this.getErrorMessage(error, 'Failed to mark notification as read'));
        return EMPTY;
      }),
      finalize(() => this.removeMarkingReadId(notification.id)),
      map((response) => response.data),
    );
  }

  markAllAsRead(onSuccess?: () => void): void {
    if (this.markingAllRead() || !this.hasUnread()) {
      return;
    }

    this.markingAllRead.set(true);

    this.notificationApi.markAllAsRead().subscribe({
      next: () => {
        if (this.unreadOnly()) {
          this.notifications.set([]);
        } else {
          this.notifications.update((notifications) =>
            notifications.map((notification) => ({
              ...notification,
              read: true,
            })),
          );
        }

        this.unreadCount.set(0);
        onSuccess?.();
        this.markingAllRead.set(false);
      },
      error: (error: unknown) => {
        this.markingAllRead.set(false);
        this.alert.error(this.getErrorMessage(error, 'Failed to mark all notifications as read'));
      },
    });
  }

  reset(): void {
    this.notifications.set([]);
    this.unreadCount.set(0);
    this.loading.set(false);
    this.loadingMore.set(false);
    this.loaded.set(false);
    this.error.set(null);
    this.currentPage.set(0);
    this.totalPages.set(0);
    this.last.set(true);
    this.unreadOnly.set(false);
    this.markingReadIds.set(new Set<number>());
    this.markingAllRead.set(false);
    this.dashboardPreview.set([]);
    this.dashboardPreviewLoading.set(false);
    this.dashboardPreviewError.set(null);
    this.unreadCountLoading = false;
  }

  private removeMarkingReadId(id: number): void {
    this.markingReadIds.update((ids) => {
      const nextIds = new Set(ids);
      nextIds.delete(id);
      return nextIds;
    });
  }

  private getErrorMessage(error: unknown, fallback: string): string {
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

    return fallback;
  }
}
