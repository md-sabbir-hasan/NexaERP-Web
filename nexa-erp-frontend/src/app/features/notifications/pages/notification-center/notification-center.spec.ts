import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { signal } from '@angular/core';
import { Observable, Subject, of, throwError } from 'rxjs';

import { ApiResponse } from '../../../../core/models/api-response.model';
import { PageResponse } from '../../../../core/models/page.model';
import { NotificationResponse } from '../../models/notification.model';
import { NotificationApiService } from '../../services/notification-api.service';
import { NotificationStore } from '../../services/notification.store';
import { NotificationCenter } from './notification-center';

const unreadBudgetNotification: NotificationResponse = {
  id: 1,
  type: 'BUDGET_EXCEEDED',
  title: 'Budget exceeded',
  message: 'Travel budget was exceeded.',
  route: '/budget/7/variance',
  entityType: 'BUDGET',
  entityId: 7,
  read: false,
  readAt: null,
  expiresAt: null,
  createdAt: '2026-07-25T10:00:00',
};

const readNotification: NotificationResponse = {
  ...unreadBudgetNotification,
  id: 2,
  title: 'Earlier budget update',
  route: '/budget',
  entityId: null,
  read: true,
  readAt: '2026-07-25T11:00:00',
};

function pageResponse(
  content: NotificationResponse[],
  page = 0,
  totalElements = content.length,
  totalPages = content.length ? 1 : 0,
): ApiResponse<PageResponse<NotificationResponse>> {
  return {
    success: true,
    message: 'Success',
    data: {
      content,
      page,
      size: 20,
      totalElements,
      totalPages,
      first: page === 0,
      last: page + 1 >= totalPages,
    },
  };
}

describe('NotificationCenter', () => {
  let fixture: ComponentFixture<NotificationCenter>;
  let component: NotificationCenter;
  let api: {
    getNotifications: ReturnType<typeof vi.fn>;
  };
  let store: {
    unreadCount: ReturnType<typeof signal<number>>;
    markingReadIds: ReturnType<typeof signal<ReadonlySet<number>>>;
    markingAllRead: ReturnType<typeof signal<boolean>>;
    hasUnread: ReturnType<typeof signal<boolean>>;
    loadUnreadCount: ReturnType<typeof vi.fn>;
    markAsRead: ReturnType<typeof vi.fn>;
    markAllAsRead: ReturnType<typeof vi.fn>;
  };
  let router: {
    navigateByUrl: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    api = {
      getNotifications: vi.fn(() => of(pageResponse([unreadBudgetNotification]))),
    };
    store = {
      unreadCount: signal(1),
      markingReadIds: signal<ReadonlySet<number>>(new Set<number>()),
      markingAllRead: signal(false),
      hasUnread: signal(true),
      loadUnreadCount: vi.fn(),
      markAsRead: vi.fn((notification: NotificationResponse) => {
        const updated = {
          ...notification,
          read: true,
          readAt: '2026-07-25T12:00:00',
        };
        store.unreadCount.update((count) => Math.max(0, count - 1));
        store.hasUnread.set(store.unreadCount() > 0);
        return of(updated);
      }),
      markAllAsRead: vi.fn((onSuccess?: () => void) => {
        store.unreadCount.set(0);
        store.hasUnread.set(false);
        onSuccess?.();
      }),
    };
    router = {
      navigateByUrl: vi.fn(() => Promise.resolve(true)),
    };

    await TestBed.configureTestingModule({
      imports: [NotificationCenter],
      providers: [
        { provide: NotificationApiService, useValue: api },
        { provide: NotificationStore, useValue: store },
        { provide: Router, useValue: router },
      ],
    }).compileComponents();
  });

  function createComponent(): void {
    fixture = TestBed.createComponent(NotificationCenter);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('loads all notifications by default and requests unread notifications when filtered', () => {
    createComponent();

    expect(api.getNotifications).toHaveBeenNthCalledWith(1, 0, 20, false);

    component.setFilter(true);

    expect(component.unreadOnly()).toBe(true);
    expect(api.getNotifications).toHaveBeenLastCalledWith(0, 20, true);
  });

  it('resets pagination to the first page when switching filters', () => {
    api.getNotifications.mockImplementation(
      (page: number, _size: number, unreadOnly: boolean) =>
        of(pageResponse(unreadOnly ? [unreadBudgetNotification] : [readNotification], page, 40, 2)),
    );
    createComponent();
    component.loadPage(1);

    expect(component.page()).toBe(1);

    component.setFilter(true);

    expect(api.getNotifications).toHaveBeenLastCalledWith(0, 20, true);
    expect(component.page()).toBe(0);
  });

  it('marks one notification read after a successful backend response and syncs unread count', () => {
    createComponent();

    component.selectNotification(unreadBudgetNotification);

    expect(store.markAsRead).toHaveBeenCalledWith(unreadBudgetNotification);
    expect(component.notifications()[0].read).toBe(true);
    expect(store.unreadCount()).toBe(0);
  });

  it('marks all loaded notifications read and syncs the header count', () => {
    api.getNotifications.mockReturnValue(
      of(pageResponse([unreadBudgetNotification, readNotification])),
    );
    createComponent();

    component.markAllAsRead();

    expect(store.markAllAsRead).toHaveBeenCalledOnce();
    expect(component.notifications().every((notification) => notification.read)).toBe(true);
    expect(store.unreadCount()).toBe(0);
  });

  it('navigates to a confirmed supported internal route', () => {
    createComponent();

    component.selectNotification(readNotification);

    expect(router.navigateByUrl).toHaveBeenCalledWith('/budget');
  });

  it('does not navigate to an unsupported or external route', () => {
    createComponent();
    const unsupported = {
      ...readNotification,
      route: 'https://example.com',
      entityType: 'SYSTEM',
    };

    component.selectNotification(unsupported);

    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('renders a loading state while the page request is pending', () => {
    const pending = new Subject<ApiResponse<PageResponse<NotificationResponse>>>();
    api.getNotifications.mockReturnValue(pending.asObservable());

    createComponent();

    expect(fixture.nativeElement.textContent).toContain('Loading notifications');
  });

  it('renders the empty state when no notifications are returned', () => {
    api.getNotifications.mockReturnValue(of(pageResponse([])));

    createComponent();

    expect(fixture.nativeElement.textContent).toContain('No notifications yet');
  });

  it('renders fallback priority and module badges for an old payload', () => {
    api.getNotifications.mockReturnValue(of(pageResponse([unreadBudgetNotification])));
    createComponent();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('MEDIUM');
    expect(text).toContain('SYSTEM');
  });

  it('renders explicit priority and module badges', () => {
    api.getNotifications.mockReturnValue(
      of(
        pageResponse([
          {
            ...unreadBudgetNotification,
            priority: 'CRITICAL',
            module: 'ACCOUNTING_PERIOD',
          },
        ]),
      ),
    );
    createComponent();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('CRITICAL');
    expect(text).toContain('ACCOUNTING PERIOD');
  });

  it('renders the unread empty state for an empty unread result', () => {
    api.getNotifications.mockReturnValue(of(pageResponse([])));
    createComponent();

    component.setFilter(true);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain("You're all caught up");
  });

  it('renders an error and retries the current page', () => {
    api.getNotifications.mockReturnValueOnce(
      throwError(() => ({ error: { message: 'Notifications unavailable' } })),
    );
    createComponent();

    expect(fixture.nativeElement.textContent).toContain('Notifications unavailable');

    api.getNotifications.mockReturnValueOnce(of(pageResponse([readNotification])));
    component.retry();
    fixture.detectChanges();

    expect(api.getNotifications).toHaveBeenCalledTimes(2);
    expect(component.notifications()).toEqual([readNotification]);
    expect(component.error()).toBeNull();
  });
});
