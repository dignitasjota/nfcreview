import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/api/auth.service';
import { toApiError } from '../../core/interceptors/error.interceptor';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <div class="login">
      <div class="panel">
        <div class="brand"><span class="logo">✓</span> ReviewTap</div>
        <h1>Iniciar sesión</h1>
        <p class="muted">Accede al panel de tu negocio.</p>

        @if (expired()) { <div class="alert alert-info">Tu sesión ha caducado. Vuelve a identificarte.</div> }
        @if (error()) { <div class="alert alert-error" role="alert">{{ error() }}</div> }

        <form [formGroup]="form" (ngSubmit)="submit()" class="stack" novalidate>
          <div class="field">
            <label for="email">Email</label>
            <input id="email" class="input" type="email" formControlName="email" autocomplete="username" autofocus>
            @if (form.controls.email.touched && form.controls.email.invalid) { <span class="error">Introduce un email válido.</span> }
          </div>
          <div class="field">
            <label for="password">Contraseña</label>
            <input id="password" class="input" type="password" formControlName="password" autocomplete="current-password">
            @if (form.controls.password.touched && form.controls.password.invalid) { <span class="error">La contraseña es obligatoria.</span> }
          </div>
          <button class="btn btn-primary btn-lg btn-block" type="submit" [disabled]="loading()">
            {{ loading() ? 'Entrando…' : 'Entrar' }}
          </button>
        </form>
        <p class="small muted foot"><a routerLink="/privacy">Privacidad</a> · <a routerLink="/terms">Términos</a></p>
      </div>
    </div>
  `,
  styles: `
    .login { min-height: 100vh; display: grid; place-items: center; padding: 1rem; background: radial-gradient(1200px 600px at 20% -10%, #ccfbf1 0%, transparent 60%), var(--bg); }
    .panel { width: 100%; max-width: 400px; background: #fff; border: 1px solid var(--line); border-radius: 16px; padding: 2rem; display: grid; gap: 0.75rem; box-shadow: 0 20px 60px rgb(15 23 42 / 0.08); }
    .brand { display: flex; align-items: center; gap: 0.5rem; font-weight: 700; margin-bottom: 0.5rem; }
    .logo { width: 30px; height: 30px; border-radius: 9px; background: var(--brand); color: #fff; display: grid; place-items: center; }
    h1 { font-size: 1.35rem; }
    form { margin-top: 0.5rem; }
    .foot { text-align: center; margin-top: 0.25rem; }
  `,
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly expired = signal(this.route.snapshot.queryParamMap.has('expired'));

  async submit() {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.loading()) return;
    this.loading.set(true);
    this.error.set(null);
    try {
      const { email, password } = this.form.getRawValue();
      const me = await this.auth.login(email.trim(), password);
      const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
      const fallback = me.role === 'ADMIN' ? '/admin' : '/app';
      await this.router.navigateByUrl(returnUrl && returnUrl.startsWith('/') && !returnUrl.startsWith('/login') ? returnUrl : fallback);
    } catch (e) {
      this.error.set(toApiError(e).message);
    } finally {
      this.loading.set(false);
    }
  }
}
