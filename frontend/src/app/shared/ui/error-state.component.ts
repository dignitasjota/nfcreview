import { Component, input, output } from '@angular/core';
import { ApiError } from '../../core/models';

@Component({
  selector: 'app-error-state',
  template: `
    <div class="alert alert-error row-between">
      <span>{{ error()?.message ?? 'Se ha producido un error inesperado.' }}</span>
      @if (retryable()) { <button class="btn btn-sm" type="button" (click)="retry.emit()">Reintentar</button> }
    </div>
  `,
})
export class ErrorStateComponent {
  readonly error = input<ApiError | null>();
  readonly retryable = input(true);
  readonly retry = output<void>();
}
