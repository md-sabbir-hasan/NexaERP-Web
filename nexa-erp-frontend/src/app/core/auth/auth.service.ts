import { HttpClient } from '@angular/common/http';
import { Injectable, WritableSignal, computed, signal } from '@angular/core';
import { Observable, finalize, map, tap } from 'rxjs';

import { APP_CONFIG } from '../config/app.config';
import { STORAGE_KEYS } from '../constants/storage.constants';
import { ApiResponse } from '../models/api-response.model';
import {
  CurrentUserProfile,
  ForgotPasswordRequest,
  LoginRequest,
  LoginResponse,
  RefreshTokenRequest,
  ResetPasswordRequest,
  SetPasswordRequest,
} from '../models/auth.model';
import { CurrentUser } from '../models/current-user.model';
import { StorageService } from '../services/storage.service';
import { TokenService } from '../services/token.service';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly baseUrl = `${APP_CONFIG.apiUrl}/auth`;

  private readonly currentUserSignal: WritableSignal<CurrentUser | null>;

  readonly currentUser;

  readonly isLoggedIn;

  constructor(
    private http: HttpClient,
    private tokenService: TokenService,
    private storage: StorageService,
  ) {
    // পুরনো duplicate keys startup-এ clean করবে
    this.tokenService.clearLegacyTokens();

    this.currentUserSignal = signal<CurrentUser | null>(
      this.storage.get<CurrentUser>(STORAGE_KEYS.CURRENT_USER),
    );

    this.currentUser = this.currentUserSignal.asReadonly();

    this.isLoggedIn = computed(() => {
      const user = this.currentUserSignal();

      return user !== null && this.tokenService.isLoggedIn();
    });
  }

  login(request: LoginRequest): Observable<ApiResponse<LoginResponse>> {
    return this.http.post<ApiResponse<LoginResponse>>(`${this.baseUrl}/login`, request).pipe(
      tap((response) => {
        const data = response.data;

        this.tokenService.saveTokens(data.accessToken, data.refreshToken);

        const user: CurrentUser = {
          id: data.userId,
          name: data.name,
          email: data.email,
          status: 'ACTIVE',
          roles: [],
          permissions: [],
        };

        this.storage.set(STORAGE_KEYS.CURRENT_USER, user);
        this.currentUserSignal.set(user);
      }),
    );
  }

  // Full profile (real name, roles, permissions) for the logged-in user.
  // Login response only carries id/name/email — this fills in the rest.
  getMe(): Observable<ApiResponse<CurrentUserProfile>> {
    return this.http.get<ApiResponse<CurrentUserProfile>>(`${this.baseUrl}/me`);
  }

  // Fetches the full profile and updates stored/in-memory current user.
  refreshCurrentUser(): Observable<CurrentUser> {
    return this.getMe().pipe(
      map((res) => {
        const profile = res.data;
        const user: CurrentUser = {
          id: profile.id,
          name: profile.name,
          email: profile.email,
          status: profile.status,
          roles: profile.roles,
          permissions: profile.permissions,
        };
        this.storage.set(STORAGE_KEYS.CURRENT_USER, user);
        this.currentUserSignal.set(user);
        return user;
      }),
    );
  }

  refreshToken(): Observable<ApiResponse<LoginResponse>> {
    const request: RefreshTokenRequest = {
      refreshToken: this.tokenService.getRefreshToken() ?? '',
    };

    return this.http.post<ApiResponse<LoginResponse>>(`${this.baseUrl}/refresh`, request).pipe(
      tap((response) => {
        this.tokenService.saveTokens(response.data.accessToken, response.data.refreshToken);
      }),
    );
  }

  logout(): Observable<ApiResponse<null>> {
    return this.http.post<ApiResponse<null>>(`${this.baseUrl}/logout`, {}).pipe(
      // Backend logout fail হলেও browser session clear হবে
      finalize(() => this.clearSession()),
    );
  }

  clearSession(): void {
    this.tokenService.clearTokens();

    this.storage.removeMany([
      STORAGE_KEYS.CURRENT_USER,

      // পুরনো duplicate user key
      'user',

      // APP_CONFIG দিয়ে আগে save হয়ে থাকলে সম্ভাব্য keys
      'current_user',
    ]);

    this.currentUserSignal.set(null);
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
}
