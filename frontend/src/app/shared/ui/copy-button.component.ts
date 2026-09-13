import { Component, inject, input, signal } from '@angular/core';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-copy-button',
  template: `
    <button type="button" class="btn" [class.btn-primary]="primary()" [class.btn-sm]="small()" (click)="copy()">
      {{ copied() ? '✓ Copiado' : label() }}
    </button>
  `,
})
export class CopyButtonComponent {
  private readonly toast = inject(ToastService);
  readonly text = input.required<string>();
  readonly label = input('Copiar URL');
  readonly primary = input(false);
  readonly small = input(false);
  protected readonly copied = signal(false);

  async copy() {
    try {
      await navigator.clipboard.writeText(this.text());
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 1800);
    } catch {
      this.toast.error('No se pudo copiar. Selecciona la URL y cópiala manualmente.');
    }
  }
}
