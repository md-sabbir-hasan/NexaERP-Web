import { Injectable, WritableSignal, computed, signal } from '@angular/core';
import { CurrentUser } from '../models/current-user.model';

@Injectable({
  providedIn: 'root',
})
export class AuthStore {
  private readonly currentUserSignal: WritableSignal<CurrentUser | null> =
    signal<CurrentUser | null>(null);

  readonly currentUser = this.currentUserSignal.asReadonly();

  readonly isAuthenticated = computed(() => this.currentUser() !== null);

  setCurrentUser(user: CurrentUser): void {
    this.currentUserSignal.set(user);
  }

  clear(): void {
    this.currentUserSignal.set(null);
  }

  getCurrentUser(): CurrentUser | null {
    return this.currentUserSignal();
  }
}
