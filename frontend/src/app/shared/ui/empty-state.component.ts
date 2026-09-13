import { Component, input } from '@angular/core';

@Component({
  selector: 'app-empty-state',
  template: `
    <div class="empty">
      <div class="icon">{{ icon() }}</div>
      <h3>{{ title() }}</h3>
      @if (text()) { <p class="muted">{{ text() }}</p> }
      <div class="actions"><ng-content /></div>
    </div>
  `,
  styles: `
    .empty { text-align: center; padding: 2.5rem 1rem; display: grid; gap: 0.4rem; justify-items: center; }
    .icon { width: 48px; height: 48px; border-radius: 12px; background: #f1f5f9; display: grid; place-items: center; font-size: 1.4rem; margin-bottom: 0.4rem; }
    .actions { margin-top: 0.75rem; display: flex; gap: 0.5rem; }
    .actions:empty { display: none; }
  `,
})
export class EmptyStateComponent {
  readonly title = input.required<string>();
  readonly text = input<string>();
  readonly icon = input('◌');
}
