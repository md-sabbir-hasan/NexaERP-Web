import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { NotificationStore } from '../../features/notifications/services/notification.store';
import { AuthStore } from '../auth/auth.store';
import { APP_CONFIG } from '../config/app.config';
import { authInterceptor } from './auth.interceptor';
import { refreshTokenInterceptor } from './refresh-token.interceptor';

describe('refreshTokenInterceptor', () => {
  let http: HttpTestingController;
  let store: AuthStore;
  const router = { navigate: vi.fn().mockResolvedValue(true) };
  const notifications = { reset: vi.fn() };

  beforeEach(() => {
    router.navigate.mockClear();
    notifications.reset.mockClear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor, refreshTokenInterceptor])),
        provideHttpClientTesting(),
        { provide: Router, useValue: router },
        { provide: NotificationStore, useValue: notifications },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    store = TestBed.inject(AuthStore);
    store.setAccessToken('expired-access');
    store.setCurrentUser({
      id: 1, name: 'User', email: 'user@test', status: 'ACTIVE', roles: [], permissions: [],
    });
  });

  afterEach(() => http.verify());

  it('shares one refresh across concurrent 401 requests and retries both once', () => {
    const client = TestBed.inject(HttpClient);
    client.get(`${APP_CONFIG.apiUrl}/one`).subscribe();
    client.get(`${APP_CONFIG.apiUrl}/two`).subscribe();

    http.expectOne(`${APP_CONFIG.apiUrl}/one`)
      .flush({}, { status: 401, statusText: 'Unauthorized' });
    http.expectOne(`${APP_CONFIG.apiUrl}/two`)
      .flush({}, { status: 401, statusText: 'Unauthorized' });

    const refresh = http.expectOne(`${APP_CONFIG.apiUrl}/auth/web/refresh`);
    expect(refresh.request.withCredentials).toBe(true);
    refresh.flush({
      success: true,
      message: 'Token refreshed',
      data: {
        accessToken: 'new-access',
        expiresIn: 900000,
        userId: 1,
        name: 'User',
        email: 'user@test',
      },
    });

    const retries = http.match((request) =>
      request.url === `${APP_CONFIG.apiUrl}/one` ||
      request.url === `${APP_CONFIG.apiUrl}/two`,
    );
    expect(retries).toHaveLength(2);
    for (const retry of retries) {
      expect(retry.request.headers.get('Authorization')).toBe('Bearer new-access');
      retry.flush({});
    }
    http.expectNone(`${APP_CONFIG.apiUrl}/auth/web/refresh`);
  });

  it('does not refresh or clear the session for a 403 permission response', () => {
    const client = TestBed.inject(HttpClient);
    client.get(`${APP_CONFIG.apiUrl}/forbidden`).subscribe({ error: () => undefined });
    http.expectOne(`${APP_CONFIG.apiUrl}/forbidden`)
      .flush({}, { status: 403, statusText: 'Forbidden' });

    http.expectNone(`${APP_CONFIG.apiUrl}/auth/web/refresh`);
    expect(store.isAuthenticated()).toBe(true);
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
