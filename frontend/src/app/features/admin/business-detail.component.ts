import { DecimalPipe } from '@angular/common';
import { Component, effect, inject, input, signal, untracked } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { BusinessService } from '../../core/api/business.service';
import { DeviceService } from '../../core/api/device.service';
import { toApiError } from '../../core/interceptors/error.interceptor';
import { ApiError, Business, BusinessRole, Device, DeviceType, Member } from '../../core/models';
import { ToastService } from '../../core/toast.service';
import { DEVICE_TYPE_LABEL, DeviceTypeBadgeComponent, StatusBadgeComponent } from '../../shared/ui/badges.component';
import { CopyButtonComponent } from '../../shared/ui/copy-button.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state.component';
import { ErrorStateComponent } from '../../shared/ui/error-state.component';
import { LoadingComponent } from '../../shared/ui/loading.component';
import { ModalComponent } from '../../shared/ui/modal.component';
import { TIMEZONES } from '../shared/timezones';

@Component({
  selector: 'app-business-detail',
  imports: [DecimalPipe, RouterLink, ReactiveFormsModule, LoadingComponent, ErrorStateComponent, EmptyStateComponent,
    StatusBadgeComponent, DeviceTypeBadgeComponent, ModalComponent, CopyButtonComponent],
  template: `
    @if (loading()) { <app-loading /> }
    @else if (error()) { <app-error-state [error]="error()" (retry)="load()" /> }
    @else if (business(); as b) {
      <div class="page">
        <a routerLink="/admin/businesses" class="small">← Negocios</a>
        <div class="row-between">
          <div>
            <h1 class="row" style="gap:.6rem">{{ b.name }} <app-status-badge [active]="b.active" /></h1>
            <p class="muted">{{ b.slug }} · {{ b.timezone }} · {{ b.deviceCount }} dispositivos</p>
          </div>
          <div class="row">
            <button class="btn" type="button" (click)="deviceModal.set(true)">+ Dispositivo</button>
            <button class="btn" [class.btn-danger]="b.active" type="button" (click)="toggleActive()" [disabled]="busy()">{{ b.active ? 'Desactivar negocio' : 'Activar negocio' }}</button>
          </div>
        </div>

        @if (!b.googleReviewUrl) { <div class="alert alert-warn">Sin Google Review URL: los dispositivos de este negocio no redirigen hasta configurarla.</div> }
        @if (!b.active) { <div class="alert alert-warn">Negocio desactivado: ninguno de sus dispositivos redirige.</div> }

        <div class="grid grid-2">
          <section class="card">
            <div class="card-header"><h2>Configuración</h2></div>
            <form [formGroup]="form" (ngSubmit)="save()" class="stack" novalidate>
              <div class="field"><label for="name">Nombre</label><input id="name" class="input" formControlName="name"></div>
              <div class="field"><label for="url">Google Review URL</label><input id="url" class="input" formControlName="googleReviewUrl" placeholder="https://g.page/r/…/review">
                @if (form.controls.googleReviewUrl.touched && form.controls.googleReviewUrl.invalid) { <span class="error">Debe ser una URL https:// válida.</span> }
                @if (fieldErrors()['googleReviewUrl']) { <span class="error">{{ fieldErrors()['googleReviewUrl'] }}</span> }
                @if (b.googleReviewUrl) { <a class="small" [href]="b.googleReviewUrl" target="_blank" rel="noopener noreferrer">Probar destino ↗</a> }</div>
              <div class="form-grid">
                <div class="field"><label for="tz">Zona horaria</label><select id="tz" class="select" formControlName="timezone">@for (tz of timezones; track tz) { <option [value]="tz">{{ tz }}</option> }</select></div>
                <div class="field"><label for="phone">Teléfono</label><input id="phone" class="input" formControlName="phone"></div>
              </div>
              <div class="field"><label for="address">Dirección</label><input id="address" class="input" formControlName="address"></div>
              <div class="field"><label for="logo">Logo (URL https)</label><input id="logo" class="input" formControlName="logoUrl" placeholder="https://…/logo.png"></div>
              <div class="form-actions"><button class="btn btn-primary" type="submit" [disabled]="form.invalid || busy()">Guardar</button></div>
            </form>
          </section>

          <section class="card">
            <div class="card-header"><h2>Usuarios con acceso</h2><button class="btn btn-sm" type="button" (click)="memberModal.set(true)">+ Añadir</button></div>
            @if (members().length === 0) { <app-empty-state title="Nadie tiene acceso todavía" text="Añade al propietario para que pueda ver su dashboard." /> }
            @else {
              <table class="table">
                <thead><tr><th>Usuario</th><th>Rol</th><th></th></tr></thead>
                <tbody>
                  @for (m of members(); track m.userId) {
                    <tr>
                      <td>{{ m.fullName }}<div class="small muted">{{ m.email }} @if (!m.enabled) { · <span class="badge badge-danger">deshabilitado</span> }</div></td>
                      <td><span class="badge badge-muted">{{ m.role === 'OWNER' ? 'Propietario' : 'Gestor' }}</span></td>
                      <td class="num"><button class="btn btn-ghost btn-sm" type="button" (click)="removeMember(m)">Quitar</button></td>
                    </tr>
                  }
                </tbody>
              </table>
            }
            @if (lastPassword(); as p) {
              <div class="alert alert-success mt-2 row-between">
                <span>Contraseña inicial de <strong>{{ p.email }}</strong>: <code>{{ p.password }}</code></span>
                <app-copy-button [text]="p.password" label="Copiar" [small]="true" />
              </div>
            }
          </section>
        </div>

        <section class="card">
          <div class="card-header"><h2>Dispositivos</h2><a routerLink="/admin/devices" class="small">Todos los dispositivos →</a></div>
          @if (devices().length === 0) {
            <app-empty-state title="Sin dispositivos" text="Crea el primero para obtener sus URLs NFC/QR." icon="◫">
              <button class="btn btn-primary" type="button" (click)="deviceModal.set(true)">Crear dispositivo</button>
            </app-empty-state>
          } @else {
            <div class="table-wrap">
              <table class="table">
                <thead><tr><th>Nombre</th><th>Tipo</th><th>Ubicación</th><th>Código</th><th>Estado</th><th class="num">30 días</th></tr></thead>
                <tbody>
                  @for (d of devices(); track d.id) {
                    <tr>
                      <td><a class="row-link" [routerLink]="['/admin/devices', d.id]">{{ d.name }}</a></td>
                      <td><app-device-type-badge [type]="d.type" /></td>
                      <td class="muted">{{ d.locationDescription || '—' }}</td>
                      <td><code>{{ d.publicCode }}</code></td>
                      <td><app-status-badge [active]="d.active" /></td>
                      <td class="num">{{ d.interactionsLast30Days | number: '1.0-0' : 'es' }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </section>
      </div>

      @if (deviceModal()) {
        <app-modal title="Nuevo dispositivo" (close)="deviceModal.set(false)">
          <form [formGroup]="devForm" (ngSubmit)="createDevice()" class="stack" novalidate>
            <div class="field"><label for="dname">Nombre</label><input id="dname" class="input" formControlName="name" placeholder="Mostrador" autofocus></div>
            <div class="field"><label for="dtype">Tipo</label><select id="dtype" class="select" formControlName="type">@for (t of types; track t) { <option [value]="t">{{ typeLabel[t] }}</option> }</select></div>
            <div class="field"><label for="dloc">Ubicación</label><input id="dloc" class="input" formControlName="locationDescription" placeholder="Recepción"></div>
            <div class="form-actions"><button type="button" class="btn" (click)="deviceModal.set(false)">Cancelar</button><button class="btn btn-primary" type="submit" [disabled]="devForm.invalid || busy()">Crear dispositivo</button></div>
          </form>
        </app-modal>
      }

      @if (memberModal()) {
        <app-modal title="Añadir usuario al negocio" (close)="memberModal.set(false)">
          <form [formGroup]="memberForm" (ngSubmit)="addMember()" class="stack" novalidate>
            <div class="field"><label for="memail">Email</label><input id="memail" class="input" type="email" formControlName="email" autofocus><span class="hint">Si no existe, se crea el usuario (contraseña generada si se deja vacía).</span></div>
            <div class="form-grid">
              <div class="field"><label for="mrole">Rol</label><select id="mrole" class="select" formControlName="role"><option value="OWNER">Propietario</option><option value="MANAGER">Gestor</option></select></div>
              <div class="field"><label for="mfirst">Nombre</label><input id="mfirst" class="input" formControlName="firstName"></div>
            </div>
            <div class="field"><label for="mpass">Contraseña inicial</label><input id="mpass" class="input" formControlName="password" placeholder="Vacío = generar"></div>
            <div class="form-actions"><button type="button" class="btn" (click)="memberModal.set(false)">Cancelar</button><button class="btn btn-primary" type="submit" [disabled]="memberForm.invalid || busy()">Añadir</button></div>
          </form>
        </app-modal>
      }
    }
  `,
})
export class BusinessDetailComponent {
  readonly id = input.required<string>();

  private readonly businessService = inject(BusinessService);
  private readonly deviceService = inject(DeviceService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  protected readonly business = signal<Business | null>(null);
  protected readonly members = signal<Member[]>([]);
  protected readonly devices = signal<Device[]>([]);
  protected readonly loading = signal(true);
  protected readonly busy = signal(false);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly fieldErrors = signal<Record<string, string>>({});
  protected readonly deviceModal = signal(false);
  protected readonly memberModal = signal(false);
  protected readonly lastPassword = signal<{ email: string; password: string } | null>(null);
  protected readonly timezones = TIMEZONES;
  protected readonly types: DeviceType[] = ['NFC_QR', 'NFC', 'QR'];
  protected readonly typeLabel = DEVICE_TYPE_LABEL;

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    googleReviewUrl: ['', Validators.pattern(/^https:\/\/\S+$/)],
    logoUrl: ['', Validators.pattern(/^https:\/\/\S+$/)],
    timezone: ['Europe/Madrid', Validators.required],
    phone: [''],
    address: [''],
  });
  protected readonly devForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    type: ['NFC_QR' as DeviceType, Validators.required],
    locationDescription: [''],
  });
  protected readonly memberForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    role: ['OWNER' as BusinessRole, Validators.required],
    firstName: [''],
    password: ['', Validators.pattern(/^(?=.*[A-Za-z])(?=.*\d).{8,72}$/)],
  });

  constructor() {
    effect(() => {
      this.id();
      untracked(() => this.load());
    });
  }

  async load() {
    this.loading.set(true);
    this.error.set(null);
    try {
      const [b, members, devices] = await Promise.all([
        this.businessService.get(this.id()),
        this.businessService.members(this.id()),
        this.deviceService.listByBusiness(this.id()),
      ]);
      this.business.set(b);
      this.members.set(members);
      this.devices.set(devices);
      this.form.reset({ name: b.name, googleReviewUrl: b.googleReviewUrl ?? '', logoUrl: b.logoUrl ?? '', timezone: b.timezone, phone: b.phone ?? '', address: b.address ?? '' });
    } catch (e) {
      this.error.set(toApiError(e));
    } finally {
      this.loading.set(false);
    }
  }

  async save() {
    const b = this.business();
    if (!b || this.form.invalid) return;
    this.busy.set(true);
    this.fieldErrors.set({});
    try {
      const v = this.form.getRawValue();
      this.business.set(await this.businessService.update(b.id, {
        name: v.name.trim(), googleReviewUrl: v.googleReviewUrl.trim() || null, logoUrl: v.logoUrl.trim() || null,
        timezone: v.timezone, phone: v.phone.trim() || null, address: v.address.trim() || null,
      }));
      this.toast.success('Negocio actualizado');
    } catch (e) {
      const err = toApiError(e);
      this.fieldErrors.set(err.fields ?? {});
      this.toast.error(err.message);
    } finally {
      this.busy.set(false);
    }
  }

  async toggleActive() {
    const b = this.business();
    if (!b) return;
    const next = !b.active;
    if (!next && !confirm(`¿Desactivar "${b.name}"? Todos sus dispositivos dejarán de redirigir.`)) return;
    this.busy.set(true);
    try {
      this.business.set(await this.businessService.setActive(b.id, next));
      this.toast.success(next ? 'Negocio activado' : 'Negocio desactivado');
    } catch (e) {
      this.toast.error(toApiError(e).message);
    } finally {
      this.busy.set(false);
    }
  }

  async createDevice() {
    const b = this.business();
    if (!b || this.devForm.invalid) return;
    this.busy.set(true);
    try {
      const v = this.devForm.getRawValue();
      const d = await this.deviceService.create(b.id, { name: v.name.trim(), type: v.type, locationDescription: v.locationDescription.trim() || null });
      this.devices.update((list) => [...list, d]);
      this.business.update((cur) => (cur ? { ...cur, deviceCount: cur.deviceCount + 1 } : cur));
      this.devForm.reset({ name: '', type: 'NFC_QR', locationDescription: '' });
      this.deviceModal.set(false);
      this.toast.success(`Dispositivo "${d.name}" creado`);
    } catch (e) {
      this.toast.error(toApiError(e).message);
    } finally {
      this.busy.set(false);
    }
  }

  async addMember() {
    const b = this.business();
    if (!b || this.memberForm.invalid) return;
    this.busy.set(true);
    try {
      const v = this.memberForm.getRawValue();
      const res = await this.businessService.addMember(b.id, { email: v.email.trim(), role: v.role, firstName: v.firstName.trim() || null, password: v.password || null });
      this.members.update((list) => [...list, res.member]);
      this.lastPassword.set(res.generatedPassword ? { email: res.member.email, password: res.generatedPassword } : null);
      this.memberForm.reset({ email: '', role: 'OWNER', firstName: '', password: '' });
      this.memberModal.set(false);
      this.toast.success(res.userCreated ? 'Usuario creado y asignado' : 'Usuario asignado');
    } catch (e) {
      this.toast.error(toApiError(e).message);
    } finally {
      this.busy.set(false);
    }
  }

  async removeMember(m: Member) {
    const b = this.business();
    if (!b || !confirm(`¿Quitar el acceso de ${m.email} a este negocio?`)) return;
    try {
      await this.businessService.removeMember(b.id, m.userId);
      this.members.update((list) => list.filter((x) => x.userId !== m.userId));
      this.toast.success('Acceso retirado');
    } catch (e) {
      this.toast.error(toApiError(e).message);
    }
  }
}
