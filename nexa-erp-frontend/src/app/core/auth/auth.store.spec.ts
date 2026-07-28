import { TestBed } from '@angular/core/testing';
import { AuthStore } from './auth.store';

describe('AuthStore', () => {
  let store: AuthStore;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    store = TestBed.inject(AuthStore);
  });

  it('keeps token, current user, and permissions in memory', () => {
    store.setAccessToken('access');
    store.setCurrentUser({
      id: 1,
      name: 'Web User',
      email: 'web@nexaerp.test',
      status: 'ACTIVE',
      roles: ['ADMIN'],
      permissions: ['VIEW_DASHBOARD'],
    });

    expect(store.accessToken()).toBe('access');
    expect(store.isAuthenticated()).toBe(true);
    expect(store.hasPermission('VIEW_DASHBOARD')).toBe(true);
    expect(localStorage.getItem('nexa_access_token')).toBeNull();
  });

  it('always completes initialization and can clear an expired session', () => {
    store.beginInitialization();
    store.completeInitialization();
    store.clear(true);

    expect(store.initialized()).toBe(true);
    expect(store.initializing()).toBe(false);
    expect(store.sessionExpired()).toBe(true);
    expect(store.isAuthenticated()).toBe(false);
  });
});
