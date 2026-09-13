import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { UserService } from '../../core/api/user.service';
import { toApiError } from '../../core/interceptors/error.interceptor';
import { ApiError, AppUser, UserRole } from '../../core/models';
import { SessionStore } from '../../core/session.store';
import { ToastService } from '../../core/toast.service';
import { CopyButtonComponent } from '../../shared/ui/copy-button.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state.component';
import { ErrorStateComponent } from '../../shared/ui/error-state.component';
import { LoadingComponent } from '../../shared/ui/loading.component';
import { ModalComponent } from '../../shared/ui/modal.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';

@Component({
  selector: 'app-users',
  imports: [ReactiveFormsModule, RouterLink, PageHeaderComponent, LoadingComponent, ErrorStateComponent, EmptyStateComponent, ModalComponent, CopyButtonComponent],
  template: `
    <div class="page">
      <app-page-header title="Usuarios" subtitle="Administradores y usuarios de negocio.">
        <button class="btn btn-primary" type="button" (click)="openCreate()">+ Nuevo usuario</button>
      </app-page-header>

      @if (lastPassword(); as p) {
        <div class="alert alert-success row-between">
          <span>Contraseña inicial de <strong>{{ p.email }}</strong>: <code>{{ p.password }}</code> (se muestra una sola vez)</span>
          <app-copy-button [text]="p.password" label="Copiar" [small]="true" />
        </div>
      }

      <div class="card">
        @if (loading()) { <app-loading /> }
        @else if (error()) { <app-error-state [error]="error()" (retry)="load()" /> }
        @else if (users().length === 0) { <app-empty-state title="No hay usuarios" /> }
        @else {
          <div class="table-wrap">
            <table class="table">
              <thead><tr><th>Usuario</th><th>Rol</th><th>Negocios</th><th>Estado</th><th></th></tr></thead>
              <tbody>
                @for (u of users(); track u.id) {
                  <tr>
                    <td>{{ u.firstName }} {{ u.lastName }}<div class="small muted">{{ u.email }}</div></td>
                    <td><span class="badge" [class.badge-info]="u.role === 'ADMIN'" [class.badge-muted]="u.role !== 'ADMIN'">{{ u.role === 'ADMIN' ? 'Admin' : 'Negocio' }}</span></td>
                    <td>@if (u.role === 'ADMIN') { <span class="muted small">Todos</span> } @else { @for (b of u.businesses; track b.id) { <a class="small" [routerLink]="['/admin/businesses', b.id]">{{ b.name }}</a>@if (!$last) { <span class="muted">, </span> } } @empty { <span class="muted small">—</span> } }</td>
                    <td><span class="badge" [class.badge-success]="u.enabled" [class.badge-danger]="!u.enabled">{{ u.enabled ? 'Habilitado' : 'Deshabilitado' }}</span></td>
                    <td class="num">
                      <button class="btn btn-ghost btn-sm" type="button" (click)="openReset(u)">Contraseña</button>
                      @if (u.id !== me()?.id) {
                        <button class="btn btn-ghost btn-sm" type="button" (click)="toggle(u)">{{ u.enabled ? 'Deshabilitar' : 'Habilitar' }}</button>
                      }
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </div>

      @if (createModal()) {
        <app-modal title="Nuevo usuario" (close)="createModal.set(false)">
          <form [formGroup]="form" (ngSubmit)="create()" class="stack" novalidate>
            <div class="field"><label for="email">Email</label><input id="email" class="input" type="email" formControlName="email" autofocus></div>
            <div class="form-grid">
              <div class="field"><label for="first">Nombre</label><input id="first" class="input" formControlName="firstName"></div>
              <div class="field"><label for="last">Apellidos</label><input id="last" class="input" formControlName="lastName"></div>
            </div>
            <div class="field"><label for="role">Rol</label><select id="role" class="select" formControlName="role"><option value="BUSINESS_USER">Usuario de negocio</option><option value="ADMIN">Administrador</option></select>
              <span class="hint">A un usuario de negocio se le da acceso desde la ficha de cada negocio.</span></div>
            <div class="field"><label for="pass">Contraseña inicial</label><input id="pass" class="input" formControlName="password" placeholder="Vacío = generar automáticamente">
              @if (form.controls.password.touched && form.controls.password.invalid) { <span class="error">Mínimo 8 caracteres con letras y números.</span> }</div>
            <div class="form-actions"><button type="button" class="btn" (click)="createModal.set(false)">Cancelar</button><button class="btn btn-primary" type="submit" [disabled]="form.invalid || busy()">Crear</button></div>
          </form>
        </app-modal>
      }

      @if (resetTarget(); as u) {
        <app-modal title="Restablecer contraseña" (close)="resetTarget.set(null)">
          <form [formGroup]="resetForm" (ngSubmit)="reset()" class="stack" novalidate>
            <p class="muted small">Usuario: {{ u.email }}</p>
            <div class="field"><label for="np">Nueva contraseña</label><input id="np" class="input" formControlName="newPassword" autofocus>
              @if (resetForm.controls.newPassword.touched && resetForm.controls.newPassword.invalid) { <span class="error">Mínimo 8 caracteres con letras y números.</span> }</div>
            <div class="form-actions"><button type="button" class="btn" (click)="resetTarget.set(null)">Cancelar</button><button class="btn btn-primary" type="submit" [disabled]="resetForm.invalid || busy()">Guardar</button></div>
          </form>
        </app-modal>
      }
    </div>
  `,
})
export class UsersComponent {
  private readonly userService = inject(UserService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);
  protected readonly me = inject(SessionStore).user;

  protected readonly users = signal<AppUser[]>([]);
  protected readonly loading = signal(true);
  protected readonly busy = signal(false);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly createModal = signal(false);
  protected readonly resetTarget = signal<AppUser | null>(null);
  protected readonly lastPassword = signal<{ email: string; password: string } | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    firstName: ['', Validators.required],
    lastName: [''],
    role: ['BUSINESS_USER' as UserRole, Validators.required],
    password: ['', Validators.pattern(/^(?=.*[A-Za-z])(?=.*\d).{8,72}$/)],
  });
  protected readonly resetForm = this.fb.nonNullable.group({
    newPassword: ['', [Validators.required, Validators.pattern(/^(?=.*[A-Za-z])(?=.*\d).{8,72}$/)]],
  });

  constructor() {
    void this.load();
  }

  async load() {
    this.loading.set(true);
    this.error.set(null);
    try {
      this.users.set(await this.userService.list());
    } catch (e) {
      this.error.set(toApiError(e));
    } finally {
      this.loading.set(false);
    }
  }

  openCreate() {
    this.form.reset({ email: '', firstName: '', lastName: '', role: 'BUSINESS_USER', password: '' });
    this.createModal.set(true);
  }

  async create() {
    if (this.form.invalid) return;
    this.busy.set(true);
    try {
      const v = this.form.getRawValue();
      const res = await this.userService.create({ email: v.email.trim(), firstName: v.firstName.trim(), lastName: v.lastName.trim() || null, role: v.role, password: v.password || null });
      this.users.update((list) => [res.user, ...list]);
      this.lastPassword.set(res.generatedPassword ? { email: res.user.email, password: res.generatedPassword } : null);
      this.createModal.set(false);
      this.toast.success('Usuario creado');
    } catch (e) {
      this.toast.error(toApiError(e).message);
    } finally {
      this.busy.set(false);
    }
  }

  async toggle(u: AppUser) {
    try {
      const updated = await this.userService.setEnabled(u.id, !u.enabled);
      this.users.update((list) => list.map((x) => (x.id === u.id ? updated : x)));
    } catch (e) {
      this.toast.error(toApiError(e).message);
    }
  }

  openReset(u: AppUser) {
    this.resetForm.reset({ newPassword: '' });
    this.resetTarget.set(u);
  }

  async reset() {
    const u = this.resetTarget();
    if (!u || this.resetForm.invalid) return;
    this.busy.set(true);
    try {
      await this.userService.resetPassword(u.id, this.resetForm.getRawValue().newPassword);
      this.resetTarget.set(null);
      this.toast.success('Contraseña restablecida');
    } catch (e) {
      this.toast.error(toApiError(e).message);
    } finally {
      this.busy.set(false);
    }
  }
}
