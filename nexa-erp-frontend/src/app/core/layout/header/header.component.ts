import { CommonModule } from '@angular/common';
import { Component, DestroyRef, ElementRef, EventEmitter, HostListener, Output, Signal, ViewChild, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../../auth/auth.service';
import { resolveFileUrl } from '../../../shared/utils/file-url.util';
import { AuthStore } from '../../auth/auth.store';
import {
  QUICK_CREATE_ITEMS,
  QuickCreateItem,
} from '../../constants/quick-create.constants';

import { CurrentUser } from '../../models/current-user.model';
import { NotificationBell } from '../../../features/notifications/components/notification-bell/notification-bell';
import { GlobalSearch } from '../../../features/global-search/components/global-search/global-search';


const PAGE_TITLES: Record<string, string> = {
  dashboard: 'Financial Overview', accounts: 'Chart of Accounts', journals: 'Journal Entries', invoice: 'Invoices',
  'vendor-bill': 'Vendor Bills', expense: 'Expenses', payment: 'Payments', party: 'Parties', banking: 'Banking',
  budget: 'Budgets', 'recurring-expense': 'Recurring Expenses', 'fixed-assets': 'Fixed Assets', reports: 'Reports',
  audit: 'Audit Log', users: 'Users', roles: 'Roles', permissions: 'Permissions', settings: 'Settings',
  'fiscal-years': 'Fiscal Years', 'accounting-periods': 'Accounting Periods', 'credit-notes': 'Credit Notes', 'debit-notes': 'Debit Notes',
  notifications: 'Notification Center',
};

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, NotificationBell, GlobalSearch],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
})
export class HeaderComponent {
  @ViewChild(GlobalSearch) private globalSearch?: GlobalSearch;
  @Output() toggleSidebar = new EventEmitter<void>();
  private readonly destroyRef = inject(DestroyRef);
  private readonly elementRef = inject(ElementRef<HTMLElement>);
  readonly currentUser: Signal<CurrentUser | null>;
  readonly displayName: Signal<string>;
  readonly displayRole: Signal<string>;
  readonly initials: Signal<string>;

  readonly avatarUrl: Signal<string | null>;

  readonly pageTitle = signal('Financial Overview');
  readonly breadcrumb = signal<string[]>(['NexaERP', 'Financial Overview']);
  readonly profileOpen = signal(false);

  private readonly authStore = inject(AuthStore);
  readonly quickCreateOpen = signal(false);

  readonly quickCreateItems = computed(() =>
    QUICK_CREATE_ITEMS.filter((item) => this.authStore.hasPermission(item.permission)),
  );

  constructor(
    private authService: AuthService,
    private router: Router,
  ) {
    this.currentUser = this.authService.currentUser;
    this.displayName = computed(() => this.currentUser()?.name ?? 'User');
    this.displayRole = computed(() => {
      const roles = this.currentUser()?.roles ?? [];
      if (roles.length === 0) return 'Team Member';
      return roles[0]
        .toLowerCase()
        .split('_')
        .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
        .join(' ');
    });
    this.initials = computed(() =>
      this.displayName()
        .split(' ')
        .filter(Boolean)
        .slice(0, 2)
        .map((part) => part[0]?.toUpperCase())
        .join(''),
    );
    this.avatarUrl = computed(() => resolveFileUrl(this.currentUser()?.profileImageUrl));

    this.updateRouteContext(this.router.url);
    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((event) => {
        this.updateRouteContext(event.urlAfterRedirects);
        this.closeOverlays();
      });
  }

  toggleProfile(): void {
    this.globalSearch?.close();
    this.profileOpen.update((open) => !open);
  }
  openGlobalSearch(trigger: HTMLElement): void {
    this.profileOpen.set(false);
    this.globalSearch?.open(trigger);
  }

  @HostListener('document:click', ['$event'])
  closeOnOutsideClick(event: MouseEvent): void {
    const target = event.target;
    if (target instanceof Node && !this.elementRef.nativeElement.contains(target))
      this.closeOverlays();
  }

  @HostListener('document:keydown.escape') closeOnEscape(): void {
    this.closeOverlays();
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => void this.router.navigate(['/login']),
      error: () => void this.router.navigate(['/login']),
    });
  }

  private updateRouteContext(url: string): void {
    const segments = url.split('?')[0].split('#')[0].split('/').filter(Boolean);
    const firstSegment = segments[0] ?? 'dashboard';
    const title = PAGE_TITLES[firstSegment] ?? this.titleCase(firstSegment);
    const trail = segments
      .slice(1)
      .filter((segment) => !/^\d+$/.test(segment))
      .map((segment) => this.titleCase(segment));
    this.pageTitle.set(title);
    this.breadcrumb.set(['NexaERP', title, ...trail]);
  }

  private titleCase(segment: string): string {
    return segment
      .split('-')
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }

  private closeOverlays(): void {
    this.profileOpen.set(false);
    this.quickCreateOpen.set(false);
    this.globalSearch?.close();
  }

  toggleQuickCreate(): void {
    this.globalSearch?.close();
    this.profileOpen.set(false);
    this.quickCreateOpen.update((open) => !open);
  }

  openQuickCreate(item: QuickCreateItem): void {
    this.quickCreateOpen.set(false);
    void this.router.navigateByUrl(item.route);
  }

  goToProfileSettings(): void {
    this.profileOpen.set(false);
    void this.router.navigate(['/profile']);
  }
}

