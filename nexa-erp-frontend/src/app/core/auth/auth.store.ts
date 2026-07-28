import { Injectable, computed, signal } from '@angular/core';
import { CurrentUser } from '../models/current-user.model';

@Injectable({ providedIn: 'root' })
export class AuthStore {
  private readonly accessTokenSignal = signal<string | null>(null);
  private readonly currentUserSignal = signal<CurrentUser | null>(null);
  private readonly initializedSignal = signal(false);
  private readonly initializingSignal = signal(false);
  private readonly refreshInProgressSignal = signal(false);
  private readonly sessionExpiredSignal = signal(false);

  readonly accessToken = this.accessTokenSignal.asReadonly();
  readonly currentUser = this.currentUserSignal.asReadonly();
  readonly initialized = this.initializedSignal.asReadonly();
  readonly initializing = this.initializingSignal.asReadonly();
  readonly refreshInProgress = this.refreshInProgressSignal.asReadonly();
  readonly sessionExpired = this.sessionExpiredSignal.asReadonly();
  readonly isAuthenticated = computed(
    () => this.accessToken() !== null && this.currentUser() !== null,
  );

  setAccessToken(token: string): void {
    this.accessTokenSignal.set(token);
    this.sessionExpiredSignal.set(false);
  }

  setCurrentUser(user: CurrentUser): void {
    this.currentUserSignal.set(user);
  }

  beginInitialization(): void {
    this.initializingSignal.set(true);
  }

  completeInitialization(): void {
    this.initializingSignal.set(false);
    this.initializedSignal.set(true);
  }

  setRefreshInProgress(inProgress: boolean): void {
    this.refreshInProgressSignal.set(inProgress);
  }

  clear(sessionExpired = false): void {
    this.accessTokenSignal.set(null);
    this.currentUserSignal.set(null);
    this.refreshInProgressSignal.set(false);
    this.sessionExpiredSignal.set(sessionExpired);
  }

  hasPermission(permission: string): boolean {
    return this.currentUser()?.permissions.includes(permission) ?? false;
  }
}
