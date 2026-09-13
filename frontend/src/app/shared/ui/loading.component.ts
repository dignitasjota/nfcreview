import { Component, input } from '@angular/core';

@Component({
  selector: 'app-loading',
  template: `
    <div class="loading" role="status" aria-live="polite">
      <span class="spinner"></span>
      <span>{{ text() }}</span>
    </div>
  `,
  styles: `
    .loading { display: flex; align-items: center; gap: 0.6rem; color: var(--muted); padding: 1.5rem 0; justify-content: center; }
    .spinner { width: 18px; height: 18px; border-radius: 50%; border: 2px solid var(--line); border-top-color: var(--brand); animation: spin 0.8s linear infinite; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `,
})
export class LoadingComponent {
  readonly text = input('Cargando…');
}
