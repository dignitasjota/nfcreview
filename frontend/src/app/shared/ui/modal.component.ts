import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-modal',
  template: `
    <div class="modal-backdrop" (click)="onBackdrop($event)">
      <div class="modal" role="dialog" aria-modal="true" [attr.aria-label]="title()">
        <div class="row-between" style="margin-bottom: 1rem">
          <h2 style="margin: 0">{{ title() }}</h2>
          <button type="button" class="btn btn-ghost btn-sm" aria-label="Cerrar" (click)="close.emit()">✕</button>
        </div>
        <ng-content />
      </div>
    </div>
  `,
  host: { '(document:keydown.escape)': 'close.emit()' },
})
export class ModalComponent {
  readonly title = input.required<string>();
  readonly close = output<void>();

  onBackdrop(e: MouseEvent) {
    if ((e.target as HTMLElement).classList.contains('modal-backdrop')) this.close.emit();
  }
}
