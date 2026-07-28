import { HttpContextToken, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { APP_CONFIG } from '../config/app.config';

export const AUTH_RETRIED = new HttpContextToken<boolean>(() => false);

export const refreshTokenInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const isBackendApi = req.url.startsWith(APP_CONFIG.apiUrl);
  const isAuthEndpoint = req.url.includes('/auth/');

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (
        error.status !== 401 ||
        !isBackendApi ||
        isAuthEndpoint ||
        req.context.get(AUTH_RETRIED)
      ) {
        return throwError(() => error);
      }

      return authService.refreshAccessToken().pipe(
        switchMap((response) =>
          next(req.clone({
            context: req.context.set(AUTH_RETRIED, true),
            setHeaders: { Authorization: `Bearer ${response.accessToken}` },
          })),
        ),
        catchError((refreshError) => {
          if (authService.markSessionExpired()) {
            void router.navigate(['/login']);
          }
          return throwError(() => refreshError);
        }),
      );
    }),
  );
};
