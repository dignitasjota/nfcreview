import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/api/auth.service';
import { toApiError } from '../../core/interceptors/error.interceptor';

@Component({
  selector: 'app-forgot-password',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div class="auth-page">
      <div class="panel">
        <div class="brand"><span class="logo">✓</span> ReviewTap</div>
        <h1>¿Has olvidado tu contraseña?</h1>
        @if (sent()) {
          <div class="alert alert-success">Si existe una cuenta con ese email, recibirás en unos minutos un enlace para crear una contraseña nueva. Revisa también la carpeta de spam.</div>
          <a routerLink="/login" class="btn btn-block">Volver al acceso</a>
        } @else {
          <p class="muted">Escribe tu email y te enviaremos un enlace para restablecerla.</p>
          @if (error()) { <div class="alert alert-error" role="alert">{{ error() }}</div> }
          <form [formGroup]="form" (ngSubmit)="submit()" class="stack" novalidate>
            <div class="field">
              <label for="email">Email</label>
              <input id="email" class="input" type="email" formControlName="email" autocomplete="username" autofocus>
              @if (form.controls.email.touched && form.controls.email.invalid) { <span class="error">Introduce un email válido.</span> }
            </div>
            <button class="btn btn-primary btn-lg btn-block" type="submit" [disabled]="loading()">{{ loading() ? 'Enviando…' : 'Enviar enlace' }}</button>
          </form>
          <p class="small muted foot"><a routerLink="/login">← Volver al acceso</a></p>
        }
      </div>
    </div>
  `,
  styleUrl: './auth.scss',
})
export class ForgotPasswordComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  protected readonly form = this.fb.nonNullable.group({ email: ['', [Validators.required, Validators.email]] });
  protected readonly loading = signal(false);
  protected readonly sent = signal(false);
  protected readonly error = signal<string | null>(null);

  async submit() {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.loading()) return;
    this.loading.set(true);
    this.error.set(null);
    try {
      await this.auth.forgotPassword(this.form.getRawValue().email.trim());
      this.sent.set(true);
    } catch (e) {
      this.error.set(toApiError(e).message);
    } finally {
      this.loading.set(false);
    }
  }
}
