import { Component, inject } from '@angular/core';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-toast-container',
  template: `
    <div class="toasts" aria-live="polite">
      @for (t of toasts.toasts(); track t.id) {
        <div class="toast" [class]="'toast toast-' + t.kind" (click)="toasts.dismiss(t.id)">{{ t.text }}</div>
      }
    </div>
  `,
  styles: `
    .toasts { position: fixed; right: 1rem; bottom: 1rem; display: grid; gap: 0.5rem; z-index: 100; max-width: min(360px, calc(100vw - 2rem)); }
    .toast { padding: 0.7rem 1rem; border-radius: 10px; color: #fff; font-size: 0.875rem; box-shadow: 0 8px 24px rgb(15 23 42 / 0.2); cursor: pointer; animation: in 0.2s ease-out; }
    .toast-success { background: #15803d; } .toast-error { background: #b91c1c; } .toast-info { background: #1e293b; }
    @keyframes in { from { transform: translateY(8px); opacity: 0; } to { transform: none; opacity: 1; } }
  `,
})
export class ToastContainerComponent {
  protected readonly toasts = inject(ToastService);
}
