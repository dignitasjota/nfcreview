import { HttpErrorResponse } from '@angular/common/http';
import { describe, expect, it } from 'vitest';
import { toApiError } from './error.interceptor';

describe('toApiError', () => {
  it('conserva el formato ApiError del backend', () => {
    const err = new HttpErrorResponse({ status: 400, error: { code: 'VALIDATION_ERROR', message: 'Datos no válidos', fields: { googleReviewUrl: 'https obligatorio' } } });
    expect(toApiError(err)).toEqual({ code: 'VALIDATION_ERROR', message: 'Datos no válidos', fields: { googleReviewUrl: 'https obligatorio' }, status: 400 });
  });

  it('traduce errores HTTP sin cuerpo a mensajes amigables', () => {
    expect(toApiError(new HttpErrorResponse({ status: 403, error: '<html>' })).message).toContain('permiso');
    expect(toApiError(new HttpErrorResponse({ status: 0 })).message).toContain('conectar');
    expect(toApiError(new HttpErrorResponse({ status: 503 })).code).toBe('HTTP_503');
  });

  it('nunca expone errores desconocidos en crudo', () => {
    const e = toApiError(new Error('boom'));
    expect(e.code).toBe('UNKNOWN');
    expect(e.message).not.toContain('boom');
  });
});
