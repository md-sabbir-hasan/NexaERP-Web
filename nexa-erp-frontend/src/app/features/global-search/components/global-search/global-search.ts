import { CommonModule } from '@angular/common';
import {
  Component,
  DestroyRef,
  ElementRef,
  HostListener,
  ViewChild,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import {
  Subject,
  catchError,
  map,
  of,
  switchMap,
  tap,
  timer,
} from 'rxjs';
import { AuthService } from '../../../../core/auth/auth.service';
import {
  GlobalSearchGroup,
  GlobalSearchResult,
  GlobalSearchResultType,
} from '../../models/global-search.model';
import { GlobalSearchService } from '../../services/global-search.service';

interface SearchCommand {
  query: string;
  immediate: boolean;
}

interface SearchOutcome {
  query: string;
  groups: GlobalSearchGroup[];
  error: boolean;
}

@Component({
  selector: 'app-global-search',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './global-search.html',
  styleUrl: './global-search.scss',
})
export class GlobalSearch {
  @ViewChild('searchInput') private searchInput?: ElementRef<HTMLInputElement>;

  private readonly destroyRef = inject(DestroyRef);
  private readonly commands = new Subject<SearchCommand>();
  private restoreFocusTo: HTMLElement | null = null;

  readonly isOpen = signal(false);
  readonly query = signal('');
  readonly groups = signal<GlobalSearchGroup[]>([]);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly searched = signal(false);
  readonly activeIndex = signal(-1);
  readonly recentQueries = signal<string[]>([]);
  readonly flatResults = computed(() => this.groups().flatMap((group) => group.results));

  constructor(
    private readonly searchService: GlobalSearchService,
    private readonly router: Router,
    private readonly authService: AuthService,
  ) {
    this.commands
      .pipe(
        switchMap((command) => {
          const normalized = command.query.trim();
          if (normalized.length < 2) {
            return of<SearchOutcome>({ query: normalized, groups: [], error: false });
          }
          const delay$ = command.immediate ? of(0) : timer(275);
          return delay$.pipe(
            tap(() => {
              this.loading.set(true);
              this.errorMessage.set(null);
              this.searched.set(true);
            }),
            switchMap(() =>
              this.searchService.search(normalized).pipe(
                map((response) => ({
                  query: normalized,
                  groups: response.data.groups,
                  error: false,
                })),
                catchError(() =>
                  of<SearchOutcome>({ query: normalized, groups: [], error: true }),
                ),
              ),
            ),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((outcome) => {
        this.loading.set(false);
        this.activeIndex.set(-1);
        if (outcome.query.length < 2) {
          this.groups.set([]);
          this.searched.set(false);
          this.errorMessage.set(null);
          return;
        }
        if (outcome.error) {
          this.groups.set([]);
          this.errorMessage.set('Search is temporarily unavailable. Please try again.');
          return;
        }
        this.groups.set(outcome.groups);
        this.errorMessage.set(null);
      });
  }

  open(trigger?: HTMLElement): void {
    this.restoreFocusTo = trigger ?? (document.activeElement as HTMLElement | null);
    this.loadRecentQueries();
    this.isOpen.set(true);
    setTimeout(() => this.searchInput?.nativeElement.focus());
  }

  close(): void {
    if (!this.isOpen()) return;
    this.isOpen.set(false);
    this.loading.set(false);
    this.restoreFocusTo?.focus();
    this.restoreFocusTo = null;
  }

  onInput(value: string): void {
    this.query.set(value);
    this.commands.next({ query: value, immediate: false });
  }

  retry(): void {
    this.commands.next({ query: this.query(), immediate: true });
  }

  selectRecent(query: string): void {
    this.query.set(query);
    this.commands.next({ query, immediate: true });
    setTimeout(() => this.searchInput?.nativeElement.focus());
  }

  onInputKeydown(event: KeyboardEvent): void {
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      this.moveActive(1);
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      this.moveActive(-1);
    } else if (event.key === 'Enter') {
      event.preventDefault();
      const results = this.flatResults();
      const result = results[this.activeIndex()] ?? results[0];
      if (result) this.navigate(result);
    } else if (event.key === 'Escape') {
      event.preventDefault();
      this.close();
    }
  }

  setActive(result: GlobalSearchResult): void {
    this.activeIndex.set(this.flatResults().indexOf(result));
  }

  navigate(result: GlobalSearchResult): void {
    const commands = this.routeFor(result);
    if (!commands) return;
    this.rememberQuery(this.query());
    this.close();
    void this.router.navigate(commands.path, { queryParams: commands.queryParams });
  }

  resultId(result: GlobalSearchResult): string {
    return `global-search-result-${result.type}-${result.id}`;
  }

  groupLabel(type: GlobalSearchResultType): string {
    const labels: Record<GlobalSearchResultType, string> = {
      INVOICE: 'Invoices',
      VENDOR_BILL: 'Vendor Bills',
      PAYMENT: 'Payments',
      PARTY: 'Parties',
      ACCOUNT: 'Chart of Accounts',
      JOURNAL_ENTRY: 'Journal Entries',
    };
    return labels[type];
  }

  icon(type: GlobalSearchResultType): string {
    const icons: Record<GlobalSearchResultType, string> = {
      INVOICE: 'bi-receipt',
      VENDOR_BILL: 'bi-file-earmark-text',
      PAYMENT: 'bi-credit-card',
      PARTY: 'bi-people',
      ACCOUNT: 'bi-diagram-3',
      JOURNAL_ENTRY: 'bi-journal-text',
    };
    return icons[type];
  }

  statusClass(status: string): string {
    const normalized = status.toLowerCase();
    return /^[a-z_]+$/.test(normalized) ? normalized.replaceAll('_', '-') : 'unknown';
  }

  @HostListener('document:keydown', ['$event'])
  handleGlobalShortcut(event: KeyboardEvent): void {
    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
      event.preventDefault();
      if (!this.isOpen()) this.open();
    } else if (event.key === 'Escape' && this.isOpen()) {
      event.preventDefault();
      this.close();
    }
  }

  private moveActive(direction: 1 | -1): void {
    const count = this.flatResults().length;
    if (!count) return;
    const current = this.activeIndex();
    const next = current < 0
      ? direction === 1 ? 0 : count - 1
      : (current + direction + count) % count;
    this.activeIndex.set(next);
    setTimeout(() => document.getElementById(
      this.resultId(this.flatResults()[next]),
    )?.scrollIntoView({ block: 'nearest' }));
  }

  private routeFor(result: GlobalSearchResult):
    { path: Array<string | number>; queryParams?: Record<string, string> } | null {
    switch (result.type) {
      case 'INVOICE':
        return { path: ['/invoice', result.id] };
      case 'VENDOR_BILL':
        return { path: ['/vendor-bill', result.id] };
      case 'PAYMENT':
        return { path: ['/payment', result.id] };
      case 'PARTY':
        return { path: ['/party', result.id] };
      case 'ACCOUNT':
        return {
          path: ['/accounts'],
          queryParams: { q: result.title.split(' - ', 1)[0].trim().slice(0, 100) },
        };
      case 'JOURNAL_ENTRY':
        return { path: ['/journals'], queryParams: { q: result.title.slice(0, 100) } };
      default:
        return null;
    }
  }

  private recentStorageKey(): string | null {
    const email = this.authService.currentUser()?.email?.trim().toLowerCase();
    return email ? `nexa_global_search_recent:${email}` : null;
  }

  private loadRecentQueries(): void {
    const key = this.recentStorageKey();
    if (!key) {
      this.recentQueries.set([]);
      return;
    }
    try {
      const stored: unknown = JSON.parse(localStorage.getItem(key) ?? '[]');
      const safe = Array.isArray(stored)
        ? stored.filter((value): value is string => typeof value === 'string')
            .map((value) => value.trim().slice(0, 100))
            .filter((value) => value.length >= 2)
            .slice(0, 5)
        : [];
      this.recentQueries.set(safe);
    } catch {
      this.recentQueries.set([]);
    }
  }

  private rememberQuery(query: string): void {
    const normalized = query.trim().slice(0, 100);
    const key = this.recentStorageKey();
    if (!key || normalized.length < 2) return;
    const next = [
      normalized,
      ...this.recentQueries().filter(
        (item) => item.toLowerCase() !== normalized.toLowerCase(),
      ),
    ].slice(0, 5);
    this.recentQueries.set(next);
    try {
      localStorage.setItem(key, JSON.stringify(next));
    } catch {
      // Search remains usable when storage is unavailable.
    }
  }
}
