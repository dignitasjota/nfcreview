import { HttpInterceptorFn } from '@angular/common/http';

/** La sesión viaja en una cookie HttpOnly: todas las llamadas a /api la incluyen. */
export const credentialsInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.url.startsWith('/api/')) {
    return next(req.clone({ withCredentials: true, setHeaders: { 'X-Requested-With': 'XMLHttpRequest' } }));
  }
  return next(req);
};
