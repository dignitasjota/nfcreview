import { Component, computed, input, model } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PeriodKey, PeriodQuery } from '../../core/models';
import { PERIOD_OPTIONS, todayIn } from '../../core/period';

@Component({
  selector: 'app-period-selector',
  imports: [FormsModule],
  template: `
    <div class="ps">
      <div class="presets" role="tablist">
        @for (opt of options; track opt.key) {
          <button type="button" class="preset" [class.on]="query().period === opt.key" (click)="select(opt.key)">{{ opt.label }}</button>
        }
      </div>
      @if (query().period === 'custom') {
        <div class="custom">
          <input class="input" type="date" [ngModel]="query().from" (ngModelChange)="setFrom($event)" [max]="today()" aria-label="Desde">
          <span class="muted">→</span>
          <input class="input" type="date" [ngModel]="query().to" (ngModelChange)="setTo($event)" [max]="today()" aria-label="Hasta">
        </div>
        @if (invalid()) { <p class="error small">Elige un rango válido (máximo 366 días).</p> }
      }
    </div>
  `,
  styles: `
    .ps { display: grid; gap: 0.6rem; }
    .presets { display: flex; gap: 0.25rem; flex-wrap: wrap; background: #fff; border: 1px solid var(--line); border-radius: 10px; padding: 0.25rem; width: fit-content; max-width: 100%; }
    .preset { border: 0; background: transparent; font: inherit; font-size: 0.8125rem; font-weight: 500; color: var(--ink-2); padding: 0.35rem 0.7rem; border-radius: 7px; cursor: pointer; }
    .preset:hover { background: #f1f5f9; }
    .preset.on { background: var(--brand); color: #fff; }
    .custom { display: flex; gap: 0.5rem; align-items: center; flex-wrap: wrap; }
    .custom .input { width: 160px; }
    .error { color: var(--danger); }
  `,
})
export class PeriodSelectorComponent {
  readonly options = PERIOD_OPTIONS;
  readonly query = model.required<PeriodQuery>();
  readonly compact = input(false);
  /** Zona horaria del negocio: "hoy" y el máximo del calendario se calculan en ella, no en la del navegador. */
  readonly timezone = input<string>('Europe/Madrid');
  readonly today = computed(() => todayIn(this.timezone()));

  readonly invalid = computed(() => {
    const q = this.query();
    if (q.period !== 'custom' || !q.from || !q.to) return false;
    const days = (new Date(q.to).getTime() - new Date(q.from).getTime()) / 86_400_000 + 1;
    return q.from > q.to || days > 366;
  });

  select(key: PeriodKey) {
    if (key === 'custom') {
      const to = this.today();
      const from = new Date(to + 'T12:00:00Z');
      from.setUTCDate(from.getUTCDate() - 13);
      this.query.set({ period: 'custom', from: from.toISOString().slice(0, 10), to });
    } else {
      this.query.set({ period: key });
    }
  }

  setFrom(from: string) {
    this.query.update((q) => ({ ...q, from }));
  }

  setTo(to: string) {
    this.query.update((q) => ({ ...q, to }));
  }
}
