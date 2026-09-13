import { Pipe, PipeTransform } from '@angular/core';

/** Formatea un instante ISO (UTC) en la zona horaria del negocio. */
@Pipe({ name: 'tzDate' })
export class TzDatePipe implements PipeTransform {
  transform(value: string | Date | null | undefined, timezone = 'Europe/Madrid', style: 'datetime' | 'date' | 'time' = 'datetime'): string {
    if (!value) return '';
    const date = typeof value === 'string' ? new Date(value) : value;
    if (Number.isNaN(date.getTime())) return '';
    const opts: Intl.DateTimeFormatOptions =
      style === 'date'
        ? { dateStyle: 'medium' }
        : style === 'time'
          ? { timeStyle: 'short' }
          : { dateStyle: 'medium', timeStyle: 'short' };
    try {
      return new Intl.DateTimeFormat('es-ES', { ...opts, timeZone: timezone }).format(date);
    } catch {
      return new Intl.DateTimeFormat('es-ES', opts).format(date);
    }
  }
}
