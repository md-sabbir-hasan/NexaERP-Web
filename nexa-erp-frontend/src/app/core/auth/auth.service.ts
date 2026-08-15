import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import {
  Observable,
  catchError,
  finalize,
  map,
  of,
  shareReplay,
  switchMap,
  tap,
  throwError,
} from 'rxjs';

import { NotificationStore } from '../../features/notifications/services/notification.store';
import { APP_CONFIG } from '../config/app.config';
import { LEGACY_AUTH_STORAGE_KEYS } from '../constants/storage.constants';
import { ApiResponse } from '../models/api-response.model';
import {
  CurrentUserProfile,
  ForgotPasswordRequest,
  LoginRequest,
  ResetPasswordRequest,
  SetPasswordRequest,
  WebAuthResponse,
} from '../models/auth.model';
import { CurrentUser } from '../models/current-user.model';
import { StorageService } from '../services/storage.service';
import { AuthStore } from './auth.store';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly baseUrl = `${APP_CONFIG.apiUrl}/auth`;
  private refreshRequest$: Observable<WebAuthResponse> | null = null;
  private readonly http = inject(HttpClient);
  private readonly authStore = inject(AuthStore);
  private readonly storage = inject(StorageService);
  private readonly notificationStore = inject(NotificationStore);

  readonly currentUser = this.authStore.currentUser;
  readonly isLoggedIn = this.authStore.isAuthenticated;

  login(request: LoginRequest): Observable<CurrentUser> {
    return this.http
      .post<ApiResponse<WebAuthResponse>>(`${this.baseUrl}/web/login`, request, {
        withCredentials: true,
      })
      .pipe(
        tap((response) => this.authStore.setAccessToken(response.data.accessToken)),
        switchMap(() => this.loadCurrentUser()),
        catchError((error) =>
          this.expireWebSession().pipe(switchMap(() => throwError(() => error))),
        ),
      );
  }

  getMe(): Observable<ApiResponse<CurrentUserProfile>> {
    return this.http.get<ApiResponse<CurrentUserProfile>>(`${this.baseUrl}/me`);
  }

  loadCurrentUser(): Observable<CurrentUser> {
    return this.getMe().pipe(
      map((response) => this.toCurrentUser(response.data)),
      tap((user) => this.authStore.setCurrentUser(user)),
    );
  }

  refreshAccessToken(): Observable<WebAuthResponse> {
    if (this.refreshRequest$) {
      return this.refreshRequest$;
    }

    this.authStore.setRefreshInProgress(true);
    this.refreshRequest$ = this.http
      .post<ApiResponse<WebAuthResponse>>(
        `${this.baseUrl}/web/refresh`,
        {},
        { withCredentials: true },
      )
      .pipe(
        map((response) => response.data),
        tap((response) => this.authStore.setAccessToken(response.accessToken)),
        finalize(() => {
          this.refreshRequest$ = null;
          this.authStore.setRefreshInProgress(false);
        }),
        shareReplay({ bufferSize: 1, refCount: false }),
      );
    return this.refreshRequest$;
  }

  initialize(): Observable<void> {
    this.removeLegacyAuthStorage();
    this.authStore.beginInitialization();
    return this.refreshAccessToken().pipe(
      switchMap(() => this.loadCurrentUser()),
      map(() => undefined),
      catchError(() => {
        this.clearSession(false);
        return of(undefined);
      }),
      finalize(() => this.authStore.completeInitialization()),
    );
  }

  logout(): Observable<ApiResponse<null>> {
    return this.http
      .post<ApiResponse<null>>(`${this.baseUrl}/web/logout`, {}, { withCredentials: true })
      .pipe(finalize(() => this.clearSession(false)));
  }

  clearSession(sessionExpired = false): void {
    this.notificationStore.reset();
    this.authStore.clear(sessionExpired);
  }

  markSessionExpired(): boolean {
    if (this.authStore.sessionExpired()) return false;
    this.clearSession(true);
    return true;
  }

  removeLegacyAuthStorage(): void {
    this.storage.removeMany([...LEGACY_AUTH_STORAGE_KEYS]);
  }

  forgotPassword(request: ForgotPasswordRequest): Observable<ApiResponse<null>> {
    return this.http.post<ApiResponse<null>>(`${this.baseUrl}/forgot-password`, request);
  }

  resetPassword(request: ResetPasswordRequest): Observable<ApiResponse<null>> {
    return this.http.post<ApiResponse<null>>(`${this.baseUrl}/reset-password`, request);
  }

  setPassword(request: SetPasswordRequest): Observable<ApiResponse<null>> {
    return this.http.post<ApiResponse<null>>(`${this.baseUrl}/set-password`, request);
  }

  validateInvite(token: string): Observable<ApiResponse<unknown>> {
    return this.http.get<ApiResponse<unknown>>(`${this.baseUrl}/validate-invite`, {
      params: { token },
    });
  }

  private expireWebSession(): Observable<ApiResponse<null> | null> {
    return this.http
      .post<ApiResponse<null>>(`${this.baseUrl}/web/logout`, {}, { withCredentials: true })
      .pipe(
        catchError(() => of(null)),
        finalize(() => this.clearSession(false)),
      );
  }

  private toCurrentUser(profile: CurrentUserProfile): CurrentUser {
    return {
      id: profile.id,
      name: profile.name,
      email: profile.email,
      status: profile.status,
      profileImageUrl: profile.profileImageUrl ?? null,
      roles: profile.roles,
      permissions: profile.permissions,
    };
  }
}
