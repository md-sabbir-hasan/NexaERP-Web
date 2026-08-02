import { Component, ElementRef, HostListener, OnInit, signal } from '@angular/core';

import { NotificationStore } from '../../services/notification.store';
import { NotificationDropdown } from '../notification-dropdown/notification-dropdown';

@Component({
  selector: 'app-notification-bell',
  standalone: true,
  imports: [NotificationDropdown],
  templateUrl: './notification-bell.html',
  styleUrl: './notification-bell.scss',
})
export class NotificationBell implements OnInit {
  readonly open = signal(false);

  constructor(
    readonly store: NotificationStore,
    private elementRef: ElementRef<HTMLElement>,
  ) {}

  ngOnInit(): void {
    this.store.loadUnreadCount();
  }

  // toggleDropdown(): void {
  //   const willOpen = !this.open();

  //   this.open.set(willOpen);

  //   if (willOpen && !this.store.loaded()) {
  //     this.store.loadFirstPage(false);
  //   }
  // }

  toggleDropdown(): void {
  const willOpen = !this.open();
  this.open.set(willOpen);

  if (!willOpen) {
    return;
  }

  this.store.loadUnreadCount();
  this.store.loadFirstPage(this.store.unreadOnly());
}

  @HostListener('document:keydown.escape')
  closeOnEscape(): void {
    this.open.set(false);
  }

  @HostListener('document:click', ['$event'])
  closeOnOutsideClick(event: MouseEvent): void {
    const target = event.target;

    if (target instanceof Node && !this.elementRef.nativeElement.contains(target)) {
      this.open.set(false);
    }
  }
}
