import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { TokenService } from '../services/token.service';

export const refreshTokenInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const tokenService = inject(TokenService);
  const router = inject(Router);

  const isAuthUrl =
    req.url.includes('/auth/login') ||
    req.url.includes('/auth/refresh') ||
    req.url.includes('/auth/logout');

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if ((error.status !== 401 && error.status !== 403) || isAuthUrl) {
        return throwError(() => error);
      }

      const refreshToken = tokenService.getRefreshToken();

      if (!refreshToken) {
        authService.clearSession();
        router.navigate(['/login']);
        return throwError(() => error);
      }

      return authService.refreshToken().pipe(
        switchMap((response) => {
          const newAccessToken = response.data.accessToken;

          const retryRequest = req.clone({
            setHeaders: {
              Authorization: `Bearer ${newAccessToken}`,
            },
          });

          return next(retryRequest);
        }),
        catchError((refreshError) => {
          authService.clearSession();
          router.navigate(['/login']);
          return throwError(() => refreshError);
        }),
      );
    }),
  );
};
