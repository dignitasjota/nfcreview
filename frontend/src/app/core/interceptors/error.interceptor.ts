import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { ApiError } from '../models';
import { SessionStore } from '../session.store';

const GENERIC: Record<number, string> = {
  0: 'No se ha podido conectar con el servidor. Comprueba tu conexión.',
  400: 'La petición no es válida.',
  401: 'Tu sesión ha caducado. Vuelve a iniciar sesión.',
  403: 'No tienes permiso para realizar esta acción.',
  404: 'El recurso solicitado no existe.',
  409: 'La operación entra en conflicto con datos existentes.',
  429: 'Demasiadas peticiones. Espera unos minutos.',
  500: 'Se ha producido un error inesperado. Inténtalo de nuevo.',
};

/**
 * Normaliza cualquier fallo HTTP a {@link ApiError} (el usuario nunca ve un error crudo) y,
 * ante un 401 fuera del login, limpia la sesión local y redirige a /login.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const session = inject(SessionStore);
  return next(req).pipe(
    catchError((err: unknown) => {
      const apiError = toApiError(err);
      if (apiError.status === 401 && !req.url.endsWith('/api/auth/login') && !req.url.endsWith('/api/auth/me')) {
        session.clear();
        void router.navigate(['/login'], { queryParams: { expired: 1 } });
      }
      return throwError(() => apiError);
    }),
  );
};

export function toApiError(err: unknown): ApiError {
  if (err instanceof HttpErrorResponse) {
    const body = err.error;
    if (body && typeof body === 'object' && typeof body.code === 'string' && typeof body.message === 'string') {
      return { code: body.code, message: body.message, fields: body.fields, status: err.status };
    }
    return { code: 'HTTP_' + err.status, message: GENERIC[err.status] ?? GENERIC[500], status: err.status };
  }
  if (isApiError(err)) {
    return err;
  }
  return { code: 'UNKNOWN', message: GENERIC[500] };
}

export function isApiError(e: unknown): e is ApiError {
  return !!e && typeof e === 'object' && 'code' in e && 'message' in e;
}
