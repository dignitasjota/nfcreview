import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './api/auth.service';

// inject() sólo es válido antes del primer await: se resuelven las dependencias al principio.

export const authGuard: CanActivateFn = async (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const me = await auth.ensureLoaded();
  if (me) return true;
  return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

export const adminGuard: CanActivateFn = async (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const me = await auth.ensureLoaded();
  if (!me) return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
  return me.role === 'ADMIN' ? true : router.createUrlTree(['/app']);
};

export const businessGuard: CanActivateFn = async (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const me = await auth.ensureLoaded();
  if (!me) return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
  return me.role === 'BUSINESS_USER' ? true : router.createUrlTree(['/admin']);
};

/** Raíz: reparte por rol. */
export const homeGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const me = await auth.ensureLoaded();
  if (!me) return router.createUrlTree(['/login']);
  return router.createUrlTree([me.role === 'ADMIN' ? '/admin' : '/app']);
};

export const guestGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const me = await auth.ensureLoaded();
  if (!me) return true;
  return router.createUrlTree([me.role === 'ADMIN' ? '/admin' : '/app']);
};
