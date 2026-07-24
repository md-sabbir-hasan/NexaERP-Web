import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class StorageService {
  set(key: string, value: unknown): void {
    localStorage.setItem(key, JSON.stringify(value));
  }

  get<T>(key: string): T | null {
    const value = localStorage.getItem(key);

    if (value === null) {
      return null;
    }

    try {
      return JSON.parse(value) as T;
    } catch {
      return null;
    }
  }

  remove(key: string): void {
    localStorage.removeItem(key);
  }

  removeMany(keys: string[]): void {
    keys.forEach((key) => localStorage.removeItem(key));
  }

  clear(): void {
    localStorage.clear();
  }

  has(key: string): boolean {
    return localStorage.getItem(key) !== null;
  }
}
