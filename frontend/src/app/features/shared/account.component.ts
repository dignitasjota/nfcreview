import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { AuthService } from '../../core/api/auth.service';
import { toApiError } from '../../core/interceptors/error.interceptor';
import { SessionStore } from '../../core/session.store';
import { ToastService } from '../../core/toast.service';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';

export const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d).{8,72}$/;

function matchPasswords(group: AbstractControl): ValidationErrors | null {
  const a = group.get('newPassword')?.value;
  const b = group.get('confirm')?.value;
  return a && b && a !== b ? { mismatch: true } : null;
}

@Component({
  selector: 'app-account',
  imports: [ReactiveFormsModule, PageHeaderComponent],
  template: `
    <div class="page">
      <app-page-header title="Cuenta" subtitle="Tus datos de acceso." />
      <div class="grid grid-2">
        <section class="card stack">
          <p class="card-title">Perfil</p>
          <div><p class="small muted">Nombre</p><p>{{ session.displayName() }}</p></div>
          <div><p class="small muted">Email</p><p>{{ session.user()?.email }}</p></div>
          <div><p class="small muted">Rol</p><p>{{ session.isAdmin() ? 'Administrador' : 'Usuario de negocio' }}</p></div>
          @if (!session.isAdmin()) {
            <div><p class="small muted">Negocios</p>
              <ul style="margin:.2rem 0 0 1rem; padding:0">
                @for (b of session.businesses(); track b.id) { <li>{{ b.name }} <span class="badge badge-muted">{{ b.role === 'OWNER' ? 'Propietario' : 'Gestor' }}</span></li> }
              </ul>
            </div>
          }
        </section>

        <section class="card">
          <p class="card-title">Cambiar contraseña</p>
          <form [formGroup]="form" (ngSubmit)="submit()" class="stack" novalidate>
            <div class="field"><label for="cur">Contraseña actual</label><input id="cur" class="input" type="password" formControlName="currentPassword" autocomplete="current-password"></div>
            <div class="field"><label for="new">Nueva contraseña</label><input id="new" class="input" type="password" formControlName="newPassword" autocomplete="new-password">
              <span class="hint">Mínimo 8 caracteres, con letras y números.</span>
              @if (form.controls.newPassword.touched && form.controls.newPassword.hasError('pattern')) { <span class="error">No cumple los requisitos.</span> }
            </div>
            <div class="field"><label for="conf">Repetir nueva contraseña</label><input id="conf" class="input" type="password" formControlName="confirm" autocomplete="new-password">
              @if (form.controls.confirm.touched && form.hasError('mismatch')) { <span class="error">Las contraseñas no coinciden.</span> }
            </div>
            <div class="form-actions"><button class="btn btn-primary" type="submit" [disabled]="form.invalid || busy()">Actualizar contraseña</button></div>
          </form>
        </section>
      </div>
    </div>
  `,
})
export class AccountComponent {
  protected readonly session = inject(SessionStore);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);
  protected readonly busy = signal(false);

  protected readonly form = this.fb.nonNullable.group(
    {
      currentPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.pattern(PASSWORD_PATTERN)]],
      confirm: ['', Validators.required],
    },
    { validators: matchPasswords },
  );

  async submit() {
    if (this.form.invalid) return;
    this.busy.set(true);
    try {
      const v = this.form.getRawValue();
      await this.auth.changePassword(v.currentPassword, v.newPassword);
      this.form.reset();
      this.toast.success('Contraseña actualizada');
    } catch (e) {
      const err = toApiError(e);
      this.toast.error(err.fields?.['newPassword'] ?? err.message);
    } finally {
      this.busy.set(false);
    }
  }
}
