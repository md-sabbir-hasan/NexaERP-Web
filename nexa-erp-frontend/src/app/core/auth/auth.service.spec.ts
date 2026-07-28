import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { NotificationStore } from '../../features/notifications/services/notification.store';
import { APP_CONFIG } from '../config/app.config';
import { AuthStore } from './auth.store';
import { AuthService } from './auth.service';

describe('AuthService browser session', () => {
  let service: AuthService;
  let store: AuthStore;
  let http: HttpTestingController;
  const notificationStore = { reset: vi.fn() };

  beforeEach(() => {
    localStorage.clear();
    notificationStore.reset.mockClear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: NotificationStore, useValue: notificationStore },
      ],
    });
    service = TestBed.inject(AuthService);
    store = TestBed.inject(AuthStore);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('logs in with credentials and restores the user without localStorage writes', () => {
    let completed = false;
    service.login({ email: 'web@nexaerp.test', password: 'secret' })
      .subscribe(() => completed = true);

    const login = http.expectOne(`${APP_CONFIG.apiUrl}/auth/web/login`);
    expect(login.request.withCredentials).toBe(true);
    login.flush({
      success: true,
      message: 'Login successful',
      data: {
        accessToken: 'access',
        expiresIn: 900000,
        userId: 1,
        name: 'Web User',
        email: 'web@nexaerp.test',
      },
    });

    http.expectOne(`${APP_CONFIG.apiUrl}/auth/me`).flush({
      success: true,
      message: 'OK',
      data: {
        id: 1,
        name: 'Web User',
        email: 'web@nexaerp.test',
        status: 'ACTIVE',
        roles: ['ADMIN'],
        permissions: ['VIEW_DASHBOARD'],
      },
    });

    expect(completed).toBe(true);
    expect(store.accessToken()).toBe('access');
    expect(store.hasPermission('VIEW_DASHBOARD')).toBe(true);
    expect(localStorage.getItem('nexa_access_token')).toBeNull();
    expect(localStorage.getItem('nexa_refresh_token')).toBeNull();
    expect(localStorage.getItem('nexa_current_user')).toBeNull();
  });

  it('removes only legacy auth keys and performs one startup refresh', () => {
    localStorage.setItem('nexa_access_token', '"old"');
    localStorage.setItem('nexa_refresh_token', '"old"');
    localStorage.setItem('nexa_current_user', '{}');
    localStorage.setItem('unrelated_preference', 'keep');

    service.initialize().subscribe();

    const refresh = http.expectOne(`${APP_CONFIG.apiUrl}/auth/web/refresh`);
    expect(refresh.request.withCredentials).toBe(true);
    expect(refresh.request.body).toEqual({});
    refresh.flush({
      success: true,
      message: 'Token refreshed',
      data: {
        accessToken: 'new-access',
        expiresIn: 900000,
        userId: 1,
        name: 'Web User',
        email: 'web@nexaerp.test',
      },
    });
    http.expectOne(`${APP_CONFIG.apiUrl}/auth/me`).flush({
      success: true,
      message: 'OK',
      data: {
        id: 1,
        name: 'Web User',
        email: 'web@nexaerp.test',
        status: 'ACTIVE',
        roles: [],
        permissions: [],
      },
    });

    expect(store.initialized()).toBe(true);
    expect(store.accessToken()).toBe('new-access');
    expect(localStorage.getItem('nexa_access_token')).toBeNull();
    expect(localStorage.getItem('nexa_refresh_token')).toBeNull();
    expect(localStorage.getItem('nexa_current_user')).toBeNull();
    expect(localStorage.getItem('unrelated_preference')).toBe('keep');
  });

  it('settles startup as guest when refresh is unavailable', () => {
    service.initialize().subscribe();
    http.expectOne(`${APP_CONFIG.apiUrl}/auth/web/refresh`)
      .flush({ success: false, message: 'No session' }, { status: 401, statusText: 'Unauthorized' });

    expect(store.initialized()).toBe(true);
    expect(store.isAuthenticated()).toBe(false);
  });

  it('clears memory and notification state when logout fails', () => {
    store.setAccessToken('access');
    store.setCurrentUser({
      id: 1, name: 'User', email: 'user@test', status: 'ACTIVE', roles: [], permissions: [],
    });
    service.logout().subscribe({ error: () => undefined });
    const request = http.expectOne(`${APP_CONFIG.apiUrl}/auth/web/logout`);
    expect(request.request.withCredentials).toBe(true);
    request.flush({}, { status: 500, statusText: 'Error' });

    expect(store.isAuthenticated()).toBe(false);
    expect(notificationStore.reset).toHaveBeenCalledOnce();
  });
});
