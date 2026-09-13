import { describe, expect, it } from 'vitest';
import { isCompletePeriod, isoDate, periodLabel, toHttpParams } from './period';

describe('period utils', () => {
  it('sólo envía fechas en el periodo personalizado', () => {
    expect(toHttpParams({ period: '7d', from: '2026-01-01', to: '2026-01-31' })).toEqual({ period: '7d' });
    expect(toHttpParams({ period: 'custom', from: '2026-01-01', to: '2026-01-31' })).toEqual({ period: 'custom', from: '2026-01-01', to: '2026-01-31' });
  });

  it('valida que el rango personalizado esté completo y ordenado', () => {
    expect(isCompletePeriod({ period: 'today' })).toBe(true);
    expect(isCompletePeriod({ period: 'custom' })).toBe(false);
    expect(isCompletePeriod({ period: 'custom', from: '2026-02-01', to: '2026-01-01' })).toBe(false);
    expect(isCompletePeriod({ period: 'custom', from: '2026-01-01', to: '2026-01-31' })).toBe(true);
  });

  it('formatea fechas locales sin desplazamiento de zona', () => {
    expect(isoDate(new Date(2026, 0, 5))).toBe('2026-01-05');
    expect(periodLabel('last_month')).toBe('Mes anterior');
    expect(periodLabel('otro')).toBe('otro');
  });
});
