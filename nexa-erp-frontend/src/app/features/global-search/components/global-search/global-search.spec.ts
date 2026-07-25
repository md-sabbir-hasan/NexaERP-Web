import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { AuthService } from '../../../../core/auth/auth.service';
import { ApiResponse } from '../../../../core/models/api-response.model';
import {
  GlobalSearchGroup,
  GlobalSearchResponse,
  GlobalSearchResult,
} from '../../models/global-search.model';
import { GlobalSearchService } from '../../services/global-search.service';
import { GlobalSearch } from './global-search';

const invoice: GlobalSearchResult = {
  id: 11,
  type: 'INVOICE',
  title: 'INV-0011',
  subtitle: 'Acme',
  status: 'POSTED',
};
const account: GlobalSearchResult = {
  id: 22,
  type: 'ACCOUNT',
  title: '1000 - Cash',
  subtitle: 'ASSET',
  status: 'ACTIVE',
};
const journal: GlobalSearchResult = {
  id: 33,
  type: 'JOURNAL_ENTRY',
  title: 'JE-0033',
  subtitle: 'GENERAL',
  status: 'DRAFT',
};

function response(groups: GlobalSearchGroup[]): ApiResponse<GlobalSearchResponse> {
  return {
    success: true,
    message: 'OK',
    data: { query: 'in', groups },
  };
}

describe('GlobalSearch', () => {
  let fixture: ComponentFixture<GlobalSearch>;
  let component: GlobalSearch;
  let service: { search: ReturnType<typeof vi.fn> };
  let router: { navigate: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    vi.useFakeTimers();
    localStorage.clear();
    service = {
      search: vi.fn(() => of(response([{ type: 'INVOICE', results: [invoice] }]))),
    };
    router = { navigate: vi.fn(() => Promise.resolve(true)) };

    await TestBed.configureTestingModule({
      imports: [GlobalSearch],
      providers: [
        { provide: GlobalSearchService, useValue: service },
        { provide: Router, useValue: router },
        {
          provide: AuthService,
          useValue: {
            currentUser: signal({
              id: 1,
              name: 'Amina',
              email: 'amina@example.com',
              status: 'ACTIVE',
              roles: [],
              permissions: [],
            }),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(GlobalSearch);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => vi.useRealTimers());

  function open(): void {
    component.open();
    vi.runOnlyPendingTimers();
    fixture.detectChanges();
  }

  function search(query = 'in'): void {
    component.onInput(query);
    vi.advanceTimersByTime(275);
    fixture.detectChanges();
  }

  it('does not request below two characters and debounces valid searches', () => {
    open();
    component.onInput('i');
    vi.advanceTimersByTime(500);
    expect(service.search).not.toHaveBeenCalled();

    component.onInput('in');
    vi.advanceTimersByTime(274);
    expect(service.search).not.toHaveBeenCalled();
    vi.advanceTimersByTime(1);
    expect(service.search).toHaveBeenCalledWith('in');
  });

  it('cancels stale requests so older results cannot replace newer results', () => {
    const first = new Subject<ApiResponse<GlobalSearchResponse>>();
    const second = new Subject<ApiResponse<GlobalSearchResponse>>();
    service.search.mockReturnValueOnce(first).mockReturnValueOnce(second);
    open();

    search('in');
    search('pay');
    second.next(response([{ type: 'PAYMENT', results: [{ ...invoice, type: 'PAYMENT' }] }]));
    first.next(response([{ type: 'INVOICE', results: [invoice] }]));

    expect(component.groups()[0].type).toBe('PAYMENT');
  });

  it('renders grouped results and never renders unexpected sensitive response fields', () => {
    const unsafe = {
      ...invoice,
      notes: 'RAW_SECRET_NOTE',
      amount: 999,
      bankAccount: 'RAW_BANK',
      fileUrl: 'RAW_FILE',
    } as GlobalSearchResult;
    service.search.mockReturnValue(of(response([{ type: 'INVOICE', results: [unsafe] }])));
    open();
    search();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Invoices');
    expect(text).toContain('INV-0011');
    expect(text).not.toContain('RAW_SECRET_NOTE');
    expect(text).not.toContain('RAW_BANK');
    expect(text).not.toContain('RAW_FILE');
  });

  it('shows loading, empty, error and retry states', () => {
    const pending = new Subject<ApiResponse<GlobalSearchResponse>>();
    service.search.mockReturnValueOnce(pending);
    open();
    search();
    expect(fixture.nativeElement.textContent).toContain('Searching NexaERP');

    pending.next(response([]));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No matches found');

    service.search.mockReturnValueOnce(throwError(() => new Error('offline')));
    search('err');
    expect(fixture.nativeElement.textContent).toContain('temporarily unavailable');

    service.search.mockReturnValueOnce(of(response([{ type: 'INVOICE', results: [invoice] }])));
    component.retry();
    fixture.detectChanges();
    expect(service.search).toHaveBeenLastCalledWith('err');
    expect(fixture.nativeElement.textContent).toContain('INV-0011');
  });

  it('supports arrow navigation and Enter opens active or first result', () => {
    service.search.mockReturnValue(of(response([{
      type: 'INVOICE',
      results: [invoice, { ...invoice, id: 12, title: 'INV-0012' }],
    }])));
    open();
    search();

    component.onInputKeydown(new KeyboardEvent('keydown', { key: 'ArrowDown' }));
    component.onInputKeydown(new KeyboardEvent('keydown', { key: 'ArrowDown' }));
    expect(component.activeIndex()).toBe(1);
    component.onInputKeydown(new KeyboardEvent('keydown', { key: 'ArrowUp' }));
    expect(component.activeIndex()).toBe(0);
    component.onInputKeydown(new KeyboardEvent('keydown', { key: 'Enter' }));
    expect(router.navigate).toHaveBeenCalledWith(['/invoice', 11], { queryParams: undefined });

    component.open();
    component.activeIndex.set(-1);
    component.onInputKeydown(new KeyboardEvent('keydown', { key: 'Enter' }));
    expect(router.navigate).toHaveBeenLastCalledWith(['/invoice', 11], { queryParams: undefined });
  });

  it('opens with Ctrl+K and Cmd+K, closes with Escape and outside click', () => {
    const ctrl = new KeyboardEvent('keydown', { key: 'k', ctrlKey: true, cancelable: true });
    component.handleGlobalShortcut(ctrl);
    expect(ctrl.defaultPrevented).toBe(true);
    expect(component.isOpen()).toBe(true);

    component.onInputKeydown(new KeyboardEvent('keydown', { key: 'Escape' }));
    expect(component.isOpen()).toBe(false);

    const meta = new KeyboardEvent('keydown', { key: 'k', metaKey: true, cancelable: true });
    component.handleGlobalShortcut(meta);
    expect(component.isOpen()).toBe(true);
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('.search-backdrop') as HTMLElement)
      .dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));
    expect(component.isOpen()).toBe(false);
  });

  it('uses only the safe fixed route map for direct, Account and Journal navigation', () => {
    open();
    component.query.set('cash');
    component.navigate(invoice);
    expect(router.navigate).toHaveBeenCalledWith(['/invoice', 11], { queryParams: undefined });

    component.open();
    component.navigate(account);
    expect(router.navigate).toHaveBeenCalledWith(['/accounts'], {
      queryParams: { q: '1000' },
    });

    component.open();
    component.navigate(journal);
    expect(router.navigate).toHaveBeenCalledWith(['/journals'], {
      queryParams: { q: 'JE-0033' },
    });

    component.open();
    component.navigate({ ...invoice, type: 'UNSUPPORTED' as 'INVOICE' });
    expect(router.navigate).toHaveBeenCalledTimes(3);
  });

  it('keeps at most five normalized recent strings and fresh-searches a selection', () => {
    open();
    for (let index = 0; index < 6; index += 1) {
      component.query.set(` query ${index} `);
      component.navigate(invoice);
      component.open();
    }

    expect(component.recentQueries()).toHaveLength(5);
    const stored = localStorage.getItem('nexa_global_search_recent:amina@example.com') ?? '';
    expect(stored).toContain('query 5');
    expect(stored).not.toContain('INV-0011');
    expect(stored).not.toContain('POSTED');

    component.selectRecent('query 4');
    expect(component.query()).toBe('query 4');
    expect(service.search).toHaveBeenLastCalledWith('query 4');
  });
});
