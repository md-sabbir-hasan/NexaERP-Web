import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map, tap } from 'rxjs';

import { AuthStore } from '../../../core/auth/auth.store';
import { APP_CONFIG } from '../../../core/config/app.config';
import { ApiResponse } from '../../../core/models/api-response.model';
import { CurrentUserProfile } from '../../../core/models/auth.model';
import { CurrentUser } from '../../../core/models/current-user.model';
import { ProfileUpdateRequest } from '../models/profile.model';

@Injectable({ providedIn: 'root' })
export class ProfileService {
  private readonly baseUrl = `${APP_CONFIG.apiUrl}/profile`;
  private readonly http = inject(HttpClient);
  private readonly authStore = inject(AuthStore);

  updateName(request: ProfileUpdateRequest): Observable<CurrentUser> {
    return this.http.put<ApiResponse<CurrentUserProfile>>(this.baseUrl, request).pipe(
      map((response) => this.toCurrentUser(response.data)),
      tap((user) => this.authStore.setCurrentUser(user)),
    );
  }

  uploadPhoto(file: File): Observable<CurrentUser> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<ApiResponse<CurrentUserProfile>>(`${this.baseUrl}/photo`, formData).pipe(
      map((response) => this.toCurrentUser(response.data)),
      tap((user) => this.authStore.setCurrentUser(user)),
    );
  }

  removePhoto(): Observable<CurrentUser> {
    return this.http.delete<ApiResponse<CurrentUserProfile>>(`${this.baseUrl}/photo`).pipe(
      map((response) => this.toCurrentUser(response.data)),
      tap((user) => this.authStore.setCurrentUser(user)),
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
