import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class LoadingService {
  readonly loading = signal(false);

  show(): void {
    this.loading.set(true);
  }

  hide(): void {
    this.loading.set(false);
  }
}
