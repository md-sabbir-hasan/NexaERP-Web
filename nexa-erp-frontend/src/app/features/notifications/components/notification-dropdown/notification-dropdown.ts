import { DatePipe } from '@angular/common';
import { Component, EventEmitter, Output } from '@angular/core';
import { Router } from '@angular/router';

import { NotificationResponse } from '../../models/notification.model';
import { NotificationStore } from '../../services/notification.store';
import { getSupportedNotificationRoute } from '../../utils/notification-navigation.util';
import {
  getNotificationModuleIcon,
  getNotificationModuleLabel,
  getNotificationPriority,
} from '../../utils/notification-display.util';

@Component({
  selector: 'app-notification-dropdown',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './notification-dropdown.html',
  styleUrl: './notification-dropdown.scss',
})
export class NotificationDropdown {
  readonly priority = getNotificationPriority;
  readonly moduleLabel = getNotificationModuleLabel;
  readonly moduleIcon = getNotificationModuleIcon;
  @Output() closeRequested = new EventEmitter<void>();

  constructor(
    readonly store: NotificationStore,
    private router: Router,
  ) {}

  selectNotification(notification: NotificationResponse): void {
    if (this.store.markingReadIds().has(notification.id)) {
      return;
    }

    if (!notification.read) {
      this.store.markAsRead(notification).subscribe((updatedNotification) => {
        this.navigateToRoute(getSupportedNotificationRoute(updatedNotification));
      });
      return;
    }

    this.navigateToRoute(getSupportedNotificationRoute(notification));
  }

  viewAll(): void {
    this.closeRequested.emit();
    void this.router.navigateByUrl('/notifications');
  }

  retry(): void {
    this.store.loadFirstPage(this.store.unreadOnly());
  }

  supportedRoute(notification: NotificationResponse): string | null {
    return getSupportedNotificationRoute(notification);
  }

  private navigateToRoute(route: string | null): void {
    if (route) {
      void this.router.navigateByUrl(route);
    }
  }
}
