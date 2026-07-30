import { DatePipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';

import { NotificationResponse } from '../../models/notification.model';
import { NotificationApiService } from '../../services/notification-api.service';
import { NotificationStore } from '../../services/notification.store';
import { getSupportedNotificationRoute } from '../../utils/notification-navigation.util';
import {
  getNotificationModuleIcon,
  getNotificationModuleLabel,
  getNotificationPriority,
} from '../../utils/notification-display.util';

@Component({
  selector: 'app-notification-center',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './notification-center.html',
  styleUrl: './notification-center.scss',
})
export class NotificationCenter implements OnInit {
  readonly priority = getNotificationPriority;
  readonly moduleLabel = getNotificationModuleLabel;
  readonly moduleIcon = getNotificationModuleIcon;
  readonly notifications = signal<NotificationResponse[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly unreadOnly = signal(false);

  readonly page = signal(0);
  readonly size = signal(20);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);

  constructor(
    private notificationApi: NotificationApiService,
    readonly notificationStore: NotificationStore,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.loadPage(0);
    this.notificationStore.loadUnreadCount();
  }

  setFilter(unreadOnly: boolean): void {
    if (unreadOnly === this.unreadOnly() && this.page() === 0) {
      return;
    }

    this.unreadOnly.set(unreadOnly);
    this.loadPage(0);
  }

  loadPage(page: number): void {
    if (this.loading() || page < 0) {
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.notificationApi
      .getNotifications(page, this.size(), this.unreadOnly())
      .subscribe({
        next: (response) => {
          const result = response.data;
          this.notifications.set(result.content);
          this.page.set(result.page);
          this.size.set(result.size);
          this.totalElements.set(result.totalElements);
          this.totalPages.set(result.totalPages);
          this.loading.set(false);
        },
        error: (error: unknown) => {
          this.error.set(this.getErrorMessage(error, 'Failed to load notifications'));
          this.loading.set(false);
        },
      });
  }

  previousPage(): void {
    if (this.page() > 0) {
      this.loadPage(this.page() - 1);
    }
  }

  nextPage(): void {
    if (this.page() + 1 < this.totalPages()) {
      this.loadPage(this.page() + 1);
    }
  }

  selectNotification(notification: NotificationResponse): void {
    if (this.notificationStore.markingReadIds().has(notification.id)) {
      return;
    }

    const supportedRoute = getSupportedNotificationRoute(notification);

    if (notification.read) {
      this.navigate(supportedRoute);
      return;
    }

    this.notificationStore.markAsRead(notification).subscribe((updatedNotification) => {
      this.applyReadNotification(updatedNotification);
      this.navigate(getSupportedNotificationRoute(updatedNotification));
    });
  }

  markAllAsRead(): void {
    this.notificationStore.markAllAsRead(() => {
      if (this.unreadOnly()) {
        this.notifications.set([]);
        this.totalElements.set(0);
        this.totalPages.set(0);
        this.page.set(0);
      } else {
        const readAt = new Date().toISOString();
        this.notifications.update((notifications) =>
          notifications.map((notification) => ({
            ...notification,
            read: true,
            readAt: notification.readAt ?? readAt,
          })),
        );
      }
    });
  }

  supportedRoute(notification: NotificationResponse): string | null {
    return getSupportedNotificationRoute(notification);
  }

  retry(): void {
    this.loadPage(this.page());
  }

  private applyReadNotification(updatedNotification: NotificationResponse): void {
    if (this.unreadOnly()) {
      this.notifications.update((notifications) =>
        notifications.filter((item) => item.id !== updatedNotification.id),
      );
      this.totalElements.update((total) => Math.max(0, total - 1));
      return;
    }

    this.notifications.update((notifications) =>
      notifications.map((item) =>
        item.id === updatedNotification.id ? updatedNotification : item,
      ),
    );
  }

  private navigate(route: string | null): void {
    if (route) {
      void this.router.navigateByUrl(route);
    }
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
