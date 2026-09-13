import { PeriodKey, PeriodQuery } from './models';

export interface PeriodOption {
  key: PeriodKey;
  label: string;
}

export const PERIOD_OPTIONS: PeriodOption[] = [
  { key: 'today', label: 'Hoy' },
  { key: '7d', label: 'Últimos 7 días' },
  { key: '30d', label: 'Últimos 30 días' },
  { key: 'this_month', label: 'Este mes' },
  { key: 'last_month', label: 'Mes anterior' },
  { key: 'custom', label: 'Personalizado' },
];

export function periodLabel(key: string): string {
  return PERIOD_OPTIONS.find((p) => p.key === key)?.label ?? key;
}

/** Convierte la selección en query params para la API (sólo custom lleva fechas). */
export function toHttpParams(q: PeriodQuery): Record<string, string> {
  const params: Record<string, string> = { period: q.period };
  if (q.period === 'custom' && q.from && q.to) {
    params['from'] = q.from;
    params['to'] = q.to;
  }
  return params;
}

export function isCompletePeriod(q: PeriodQuery): boolean {
  return q.period !== 'custom' || (!!q.from && !!q.to && q.from <= q.to);
}

export function isoDate(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}
