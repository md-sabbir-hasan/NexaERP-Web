import { CommonModule } from '@angular/common';
import {
  Component,
  EventEmitter,
  HostListener,
  Output,
  Signal,
  computed,
  signal,
} from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../../auth/auth.service';
import { CurrentUser } from '../../models/current-user.model';

const PAGE_TITLES: Record<string, string> = {
  dashboard: 'Dashboard',
  accounts: 'Chart of Accounts',
  journals: 'Journal Entries',
  invoice: 'Invoices',
  'vendor-bill': 'Vendor Bills',
  payment: 'Payments',
  party: 'Parties',
  banking: 'Banking',
  'fixed-assets': 'Fixed Assets',
  reports: 'Reports',
  audit: 'Audit Log',
  users: 'Users',
  roles: 'Roles',
  permissions: 'Permissions',
  settings: 'Settings',
  'fiscal-years': 'Fiscal Years',
  'accounting-periods': 'Accounting Periods',
};

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
})
export class HeaderComponent {
  @Output() toggleSidebar = new EventEmitter<void>();

  readonly currentUser: Signal<CurrentUser | null>;
  readonly displayName: Signal<string>;
  readonly displayRole: Signal<string>;
  readonly initials: Signal<string>;

  readonly pageTitle = signal('Dashboard');
  readonly menuOpen = signal(false);

  constructor(
    private authService: AuthService,
    private router: Router,
  ) {
    this.currentUser = this.authService.currentUser;

    this.displayName = computed(() => this.currentUser()?.name ?? 'User');

    this.displayRole = computed(() => {
      const roles = this.currentUser()?.roles ?? [];
      if (roles.length === 0) return 'User';
      return roles[0]
        .toLowerCase()
        .split('_')
        .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
        .join(' ');
    });

    this.initials = computed(() => {
      const name = this.displayName();
      return name
        .split(' ')
        .filter(Boolean)
        .slice(0, 2)
        .map((part) => part[0]?.toUpperCase())
        .join('');
    });

    this.updateTitle(this.router.url);
    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe((event) => this.updateTitle(event.urlAfterRedirects));
  }

  private updateTitle(url: string): void {
    const firstSegment = url.split('/').filter(Boolean)[0] ?? 'dashboard';
    this.pageTitle.set(PAGE_TITLES[firstSegment] ?? this.titleCase(firstSegment));
  }

  private titleCase(segment: string): string {
    if (!segment) return 'Dashboard';
    return segment
      .split('-')
      .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
      .join(' ');
  }

  toggleMenu(): void {
    this.menuOpen.update((v) => !v);
  }

  closeMenu(): void {
    this.menuOpen.set(false);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.profile-chip') && !target.closest('.profile-menu')) {
      this.menuOpen.set(false);
    }
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/login']),
      error: () => this.router.navigate(['/login']),
    });
  }
}
