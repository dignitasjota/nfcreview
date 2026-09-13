import { DecimalPipe } from '@angular/common';
import { Component, input } from '@angular/core';

@Component({
  selector: 'app-kpi-card',
  imports: [DecimalPipe],
  template: `
    <div class="card kpi" [class.accent]="accent()">
      <p class="card-title">{{ label() }}</p>
      @if (loading()) {
        <div class="skeleton" style="height: 2rem; width: 60%"></div>
      } @else {
        <p class="value">
          @if (value() !== null && value() !== undefined) {
            {{ isNumber() ? (value() | number: '1.0-0' : 'es') : value() }}
          } @else { — }
        </p>
        @if (hint()) { <p class="hint">{{ hint() }}</p> }
      }
    </div>
  `,
  styles: `
    .kpi { display: grid; align-content: start; gap: 0.15rem; min-height: 108px; }
    .value { font-size: 1.85rem; font-weight: 700; letter-spacing: -0.02em; line-height: 1.1; font-variant-numeric: tabular-nums; }
    .hint { font-size: 0.8125rem; color: var(--muted); margin-top: 0.25rem; }
    .accent { border-color: var(--brand-100); background: linear-gradient(180deg, #fff, var(--brand-50)); }
  `,
})
export class KpiCardComponent {
  readonly label = input.required<string>();
  readonly value = input<number | string | null | undefined>();
  readonly hint = input<string | null>();
  readonly loading = input(false);
  readonly accent = input(false);

  protected isNumber() {
    return typeof this.value() === 'number';
  }
}
