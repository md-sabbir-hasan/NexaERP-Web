import { inject } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { filter, map, take } from 'rxjs';
import { AuthStore } from '../auth/auth.store';

export const permissionGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const authStore = inject(AuthStore);
  const router = inject(Router);
  const requiredPermission = route.data['permission'] as string | undefined;

  return toObservable(authStore.initialized).pipe(
    filter(Boolean),
    take(1),
    map(() => {
      if (!authStore.isAuthenticated()) return router.createUrlTree(['/login']);
      if (!requiredPermission || authStore.hasPermission(requiredPermission)) return true;
      return router.createUrlTree(['/access-denied']);
    }),
  );
};
