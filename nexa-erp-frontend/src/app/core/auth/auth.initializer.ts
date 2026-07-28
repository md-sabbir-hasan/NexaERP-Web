import { inject } from '@angular/core';
import { AuthService } from './auth.service';

export function initializeAuthentication() {
  return inject(AuthService).initialize();
}
