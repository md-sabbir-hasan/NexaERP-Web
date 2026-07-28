import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthStore } from '../auth/auth.store';
import { APP_CONFIG } from '../config/app.config';

const PUBLIC_AUTH_PATHS = [
  '/auth/web/login',
  '/auth/web/refresh',
  '/auth/forgot-password',
  '/auth/reset-password',
  '/auth/set-password',
  '/auth/validate-invite',
];

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authStore = inject(AuthStore);
  const isBackendApi = req.url.startsWith(APP_CONFIG.apiUrl);
  const isPublicAuth = PUBLIC_AUTH_PATHS.some((path) => req.url.includes(path));
  const token = authStore.accessToken();

  if (!isBackendApi || isPublicAuth || !token) return next(req);
  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
