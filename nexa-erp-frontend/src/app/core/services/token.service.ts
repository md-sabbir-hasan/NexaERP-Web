import { Injectable } from '@angular/core';
import { AuthStore } from '../auth/auth.store';

@Injectable({ providedIn: 'root' })
export class TokenService {
  constructor(private readonly authStore: AuthStore) {}

  getAccessToken(): string | null {
    return this.authStore.accessToken();
  }

  hasPermission(permission: string): boolean {
    return this.authStore.hasPermission(permission);
  }

  getPermissions(): string[] {
    return this.authStore.currentUser()?.permissions ?? [];
  }
}
