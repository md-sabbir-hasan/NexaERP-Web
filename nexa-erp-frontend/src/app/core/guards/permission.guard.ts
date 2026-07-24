import { inject } from '@angular/core';
import {
  ActivatedRouteSnapshot,
  CanActivateFn,
  Router,
  UrlTree,
} from '@angular/router';

import { TokenService } from '../services/token.service';

export const permissionGuard: CanActivateFn = (
  route: ActivatedRouteSnapshot,
): boolean | UrlTree => {
  const tokenService = inject(TokenService);
  const router = inject(Router);

  const requiredPermission = route.data['permission'] as string | undefined;

  if (!requiredPermission) {
    return true;
  }

  if (tokenService.hasPermission(requiredPermission)) {
    return true;
  }

  return router.createUrlTree(['/access-denied']);
};