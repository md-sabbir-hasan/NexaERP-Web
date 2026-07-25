import { DatePipe } from '@angular/common';
import { Component } from '@angular/core';
import { Router } from '@angular/router';

import { NotificationResponse } from '../../models/notification.model';
import { NotificationStore } from '../../services/notification.store';

@Component({
  selector: 'app-notification-dropdown',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './notification-dropdown.html',
  styleUrl: './notification-dropdown.scss',
})
export class NotificationDropdown {
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
        this.navigateToRoute(updatedNotification.route);
      });
      return;
    }

    this.navigateToRoute(notification.route);
  }

  retry(): void {
    this.store.loadFirstPage(this.store.unreadOnly());
  }

  private navigateToRoute(route: string | null): void {
    const internalRoute = route?.trim();

    if (!internalRoute || !internalRoute.startsWith('/') || internalRoute.startsWith('//')) {
      return;
    }

    void this.router.navigateByUrl(internalRoute);
  }
}
