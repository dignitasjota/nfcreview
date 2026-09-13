import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { BusinessService } from '../../core/api/business.service';
import { DeviceService } from '../../core/api/device.service';
import { toApiError } from '../../core/interceptors/error.interceptor';
import { Business, Device, DeviceType, OwnerResult } from '../../core/models';
import { ToastService } from '../../core/toast.service';
import { DEVICE_TYPE_LABEL } from '../../shared/ui/badges.component';
import { CopyButtonComponent } from '../../shared/ui/copy-button.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';
import { TIMEZONES } from '../shared/timezones';

/**
 * Alta de una venta en menos de dos minutos: negocio (+ propietario) → dispositivo → URLs y QR listos.
 * Cada paso persiste de inmediato; se puede abandonar tras el paso 1 sin perder nada.
 */
@Component({
  selector: 'app-business-wizard',
  imports: [ReactiveFormsModule, RouterLink, PageHeaderComponent, CopyButtonComponent],
  template: `
    <div class="page" style="max-width: 860px">
      <app-page-header title="Nuevo negocio" subtitle="Crea el negocio, su propietario y el primer dispositivo." />

      <ol class="steps">
        <li [class.on]="step() === 1" [class.done]="step() > 1"><span>1</span> Negocio</li>
        <li [class.on]="step() === 2" [class.done]="step() > 2"><span>2</span> Dispositivo</li>
        <li [class.on]="step() === 3"><span>3</span> Programar</li>
      </ol>

      @if (step() === 1) {
        <form class="card stack" [formGroup]="bizForm" (ngSubmit)="createBusiness()" novalidate>
          <h2>Datos del negocio</h2>
          <div class="form-grid">
            <div class="field" style="grid-column: 1 / -1"><label for="name">Nombre *</label><input id="name" class="input" formControlName="name" placeholder="Barbería Pepe" autofocus>
              @if (bizForm.controls.name.touched && bizForm.controls.name.invalid) { <span class="error">El nombre es obligatorio.</span> }</div>
            <div class="field" style="grid-column: 1 / -1"><label for="url">Google Review URL</label><input id="url" class="input" formControlName="googleReviewUrl" placeholder="https://g.page/r/…/review">
              <span class="hint">Enlace directo de Google para escribir una reseña. Debe empezar por https://. Puedes dejarlo vacío y configurarlo después.</span>
              @if (bizForm.controls.googleReviewUrl.touched && bizForm.controls.googleReviewUrl.invalid) { <span class="error">Debe ser una URL https:// válida.</span> }
              @if (fieldErrors()['googleReviewUrl']) { <span class="error">{{ fieldErrors()['googleReviewUrl'] }}</span> }</div>
            <div class="field"><label for="tz">Zona horaria</label><select id="tz" class="select" formControlName="timezone">@for (tz of timezones; track tz) { <option [value]="tz">{{ tz }}</option> }</select></div>
            <div class="field"><label for="phone">Teléfono</label><input id="phone" class="input" formControlName="phone"></div>
            <div class="field" style="grid-column: 1 / -1"><label for="address">Dirección</label><input id="address" class="input" formControlName="address"></div>
          </div>

          <h2 class="mt-2">Propietario (acceso al dashboard)</h2>
          <div class="form-grid">
            <div class="field"><label for="oemail">Email del propietario</label><input id="oemail" class="input" type="email" formControlName="ownerEmail" placeholder="pepe@email.com">
              <span class="hint">Si ya existe, se le asigna el negocio. Si no, se crea el usuario.</span>
              @if (bizForm.controls.ownerEmail.touched && bizForm.controls.ownerEmail.invalid) { <span class="error">Email no válido.</span> }</div>
            <div class="field"><label for="ofirst">Nombre</label><input id="ofirst" class="input" formControlName="ownerFirstName" placeholder="Pepe"></div>
            <div class="field"><label for="opass">Contraseña inicial</label><input id="opass" class="input" formControlName="ownerPassword" placeholder="Vacío = generar automáticamente">
              @if (bizForm.controls.ownerPassword.touched && bizForm.controls.ownerPassword.invalid) { <span class="error">Mínimo 8 caracteres con letras y números.</span> }</div>
          </div>

          @if (error()) { <div class="alert alert-error">{{ error() }}</div> }
          <div class="form-actions">
            <a routerLink="/admin/businesses" class="btn">Cancelar</a>
            <button class="btn btn-primary btn-lg" type="submit" [disabled]="busy()">{{ busy() ? 'Creando…' : 'Crear negocio' }}</button>
          </div>
        </form>
      }

      @if (step() >= 2 && business(); as b) {
        <div class="alert alert-success stack">
          <strong>Negocio "{{ b.name }}" creado.</strong>
          @if (owner(); as o) {
            <span>Propietario: <strong>{{ o.email }}</strong> {{ o.created ? '(usuario nuevo)' : '(usuario existente, asignado)' }}</span>
            @if (o.generatedPassword) {
              <div class="row">
                <span>Contraseña inicial: <code class="pw">{{ o.generatedPassword }}</code></span>
                <app-copy-button [text]="o.generatedPassword" label="Copiar contraseña" [small]="true" />
              </div>
              <span class="small">Se muestra una sola vez: entrégasela al cliente ahora.</span>
            }
          }
        </div>
      }

      @if (step() === 2) {
        <form class="card stack" [formGroup]="devForm" (ngSubmit)="createDevice()" novalidate>
          <h2>Primer dispositivo</h2>
          <div class="form-grid">
            <div class="field"><label for="dname">Nombre *</label><input id="dname" class="input" formControlName="name" placeholder="Mostrador" autofocus></div>
            <div class="field"><label for="dtype">Tipo</label><select id="dtype" class="select" formControlName="type">@for (t of types; track t) { <option [value]="t">{{ typeLabel[t] }}</option> }</select></div>
            <div class="field" style="grid-column: 1 / -1"><label for="dloc">Ubicación</label><input id="dloc" class="input" formControlName="locationDescription" placeholder="Recepción"></div>
          </div>
          @if (error()) { <div class="alert alert-error">{{ error() }}</div> }
          <div class="form-actions">
            <a [routerLink]="['/admin/businesses', business()!.id]" class="btn">Omitir por ahora</a>
            <button class="btn btn-primary btn-lg" type="submit" [disabled]="devForm.invalid || busy()">{{ busy() ? 'Creando…' : 'Crear dispositivo' }}</button>
          </div>
        </form>
      }

      @if (step() === 3 && device(); as d) {
        <div class="card stack">
          <h2>Dispositivo: {{ d.name }}</h2>
          <p class="muted">Listo para programar. Estas URLs son permanentes para este dispositivo.</p>
          <div class="grid grid-2">
            <div class="stack">
              <p class="card-title">NFC</p>
              <div class="url-box"><span>{{ d.nfcUrl }}</span></div>
              <div><app-copy-button [text]="d.nfcUrl" [primary]="true" /></div>
              <p class="small muted">Graba esta URL en el chip NFC.</p>
            </div>
            <div class="stack">
              <p class="card-title">QR</p>
              <div class="url-box"><span>{{ d.qrUrl }}</span></div>
              <div class="row">
                <app-copy-button [text]="d.qrUrl" />
                <button class="btn" type="button" (click)="download(d, 'png')">Descargar PNG</button>
                <button class="btn" type="button" (click)="download(d, 'svg')">Descargar SVG</button>
              </div>
              <div class="qr"><img [src]="qrPreview(d)" width="200" height="200" alt="QR"></div>
            </div>
          </div>
          <div class="form-actions">
            <button class="btn" type="button" (click)="anotherDevice()">+ Añadir otro dispositivo</button>
            <a [routerLink]="['/admin/devices', d.id]" class="btn">Ver ficha del dispositivo</a>
            <a [routerLink]="['/admin/businesses', business()!.id]" class="btn btn-primary">Ir al negocio</a>
          </div>
        </div>
      }
    </div>
  `,
  styles: `
    .steps { list-style: none; display: flex; gap: 1.5rem; padding: 0; margin: 0; }
    .steps li { display: flex; align-items: center; gap: 0.5rem; color: var(--muted); font-weight: 500; }
    .steps span { width: 26px; height: 26px; border-radius: 50%; display: grid; place-items: center; background: #e2e8f0; font-size: 0.8rem; font-weight: 700; }
    .steps .on { color: var(--ink); } .steps .on span { background: var(--brand); color: #fff; }
    .steps .done span { background: var(--brand-100); color: var(--brand); }
    .pw { background: #fff; padding: 0.15rem 0.4rem; border-radius: 4px; border: 1px solid #bbf7d0; font-weight: 700; }
    .qr { display: flex; justify-content: center; padding: 0.75rem; border: 1px dashed var(--line); border-radius: 10px; }
  `,
})
export class BusinessWizardComponent {
  private readonly fb = inject(FormBuilder);
  private readonly businessService = inject(BusinessService);
  private readonly deviceService = inject(DeviceService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  protected readonly step = signal<1 | 2 | 3>(1);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly fieldErrors = signal<Record<string, string>>({});
  protected readonly business = signal<Business | null>(null);
  protected readonly owner = signal<OwnerResult | null>(null);
  protected readonly device = signal<Device | null>(null);
  protected readonly timezones = TIMEZONES;
  protected readonly types: DeviceType[] = ['NFC_QR', 'NFC', 'QR'];
  protected readonly typeLabel = DEVICE_TYPE_LABEL;

  protected readonly bizForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    googleReviewUrl: ['', Validators.pattern(/^https:\/\/\S+$/)],
    timezone: ['Europe/Madrid'],
    phone: [''],
    address: [''],
    ownerEmail: ['', Validators.email],
    ownerFirstName: [''],
    ownerPassword: ['', Validators.pattern(/^(?=.*[A-Za-z])(?=.*\d).{8,72}$/)],
  });

  protected readonly devForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    type: ['NFC_QR' as DeviceType, Validators.required],
    locationDescription: ['', Validators.maxLength(200)],
  });

  async createBusiness() {
    this.bizForm.markAllAsTouched();
    if (this.bizForm.invalid || this.busy()) return;
    this.busy.set(true);
    this.error.set(null);
    this.fieldErrors.set({});
    try {
      const v = this.bizForm.getRawValue();
      const res = await this.businessService.create({
        name: v.name.trim(),
        googleReviewUrl: v.googleReviewUrl.trim() || null,
        timezone: v.timezone,
        phone: v.phone.trim() || null,
        address: v.address.trim() || null,
        ownerEmail: v.ownerEmail.trim() || null,
        ownerFirstName: v.ownerFirstName.trim() || null,
        ownerPassword: v.ownerPassword || null,
      });
      this.business.set(res.business);
      this.owner.set(res.owner);
      this.step.set(2);
      this.toast.success('Negocio creado');
    } catch (e) {
      const err = toApiError(e);
      this.error.set(err.message);
      this.fieldErrors.set(err.fields ?? {});
    } finally {
      this.busy.set(false);
    }
  }

  async createDevice() {
    const b = this.business();
    if (!b || this.devForm.invalid || this.busy()) return;
    this.busy.set(true);
    this.error.set(null);
    try {
      const v = this.devForm.getRawValue();
      const d = await this.deviceService.create(b.id, { name: v.name.trim(), type: v.type, locationDescription: v.locationDescription.trim() || null });
      this.device.set(d);
      this.step.set(3);
      this.toast.success('Dispositivo creado');
    } catch (e) {
      this.error.set(toApiError(e).message);
    } finally {
      this.busy.set(false);
    }
  }

  anotherDevice() {
    this.devForm.reset({ name: '', type: 'NFC_QR', locationDescription: '' });
    this.device.set(null);
    this.step.set(2);
  }

  qrPreview(d: Device) {
    return this.deviceService.qrPngUrl(d.id, 400);
  }

  async download(d: Device, format: 'png' | 'svg') {
    try {
      await this.deviceService.download(d.id, format, `qr-${d.publicCode}.${format}`);
    } catch (e) {
      this.toast.error(toApiError(e).message);
    }
  }
}
