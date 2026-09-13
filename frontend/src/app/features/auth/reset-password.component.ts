import { Component, inject, input, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/api/auth.service';
import { toApiError } from '../../core/interceptors/error.interceptor';
import { PASSWORD_PATTERN } from '../shared/account.component';

function matchPasswords(group: AbstractControl): ValidationErrors | null {
  const a = group.get('newPassword')?.value;
  const b = group.get('confirm')?.value;
  return a && b && a !== b ? { mismatch: true } : null;
}

@Component({
  selector: 'app-reset-password',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div class="auth-page">
      <div class="panel">
        <div class="brand"><span class="logo">✓</span> ReviewTap</div>
        <h1>Nueva contraseña</h1>
        @if (!token()) {
          <div class="alert alert-error">El enlace no es válido. Solicita uno nuevo desde <a routerLink="/forgot-password">¿Has olvidado tu contraseña?</a></div>
        } @else if (done()) {
          <div class="alert alert-success">Contraseña actualizada. Ya puedes iniciar sesión con ella.</div>
          <a routerLink="/login" class="btn btn-primary btn-block">Iniciar sesión</a>
        } @else {
          @if (error()) { <div class="alert alert-error" role="alert">{{ error() }} <a routerLink="/forgot-password">Solicitar otro enlace</a></div> }
          <form [formGroup]="form" (ngSubmit)="submit()" class="stack" novalidate>
            <div class="field">
              <label for="new">Nueva contraseña</label>
              <input id="new" class="input" type="password" formControlName="newPassword" autocomplete="new-password" autofocus>
              <span class="hint">Mínimo 8 caracteres, con letras y números.</span>
              @if (form.controls.newPassword.touched && form.controls.newPassword.hasError('pattern')) { <span class="error">No cumple los requisitos.</span> }
            </div>
            <div class="field">
              <label for="conf">Repetir contraseña</label>
              <input id="conf" class="input" type="password" formControlName="confirm" autocomplete="new-password">
              @if (form.controls.confirm.touched && form.hasError('mismatch')) { <span class="error">Las contraseñas no coinciden.</span> }
            </div>
            <button class="btn btn-primary btn-lg btn-block" type="submit" [disabled]="form.invalid || loading()">{{ loading() ? 'Guardando…' : 'Guardar contraseña' }}</button>
          </form>
        }
      </div>
    </div>
  `,
  styleUrl: './auth.scss',
})
export class ResetPasswordComponent {
  readonly token = input<string>();
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  protected readonly form = this.fb.nonNullable.group(
    { newPassword: ['', [Validators.required, Validators.pattern(PASSWORD_PATTERN)]], confirm: ['', Validators.required] },
    { validators: matchPasswords },
  );
  protected readonly loading = signal(false);
  protected readonly done = signal(false);
  protected readonly error = signal<string | null>(null);

  async submit() {
    const token = this.token();
    if (!token || this.form.invalid || this.loading()) return;
    this.loading.set(true);
    this.error.set(null);
    try {
      await this.auth.resetPassword(token, this.form.getRawValue().newPassword);
      this.done.set(true);
    } catch (e) {
      const err = toApiError(e);
      this.error.set(err.fields?.['newPassword'] ?? err.message);
    } finally {
      this.loading.set(false);
    }
  }
}
