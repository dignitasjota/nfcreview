import { Component, effect, inject, signal, untracked } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { BusinessService } from '../../core/api/business.service';
import { toApiError } from '../../core/interceptors/error.interceptor';
import { ApiError, Business } from '../../core/models';
import { SessionStore } from '../../core/session.store';
import { ToastService } from '../../core/toast.service';
import { ErrorStateComponent } from '../../shared/ui/error-state.component';
import { LoadingComponent } from '../../shared/ui/loading.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';
import { StatusBadgeComponent } from '../../shared/ui/badges.component';
import { TIMEZONES } from '../shared/timezones';

@Component({
  selector: 'app-my-business',
  imports: [ReactiveFormsModule, PageHeaderComponent, LoadingComponent, ErrorStateComponent, StatusBadgeComponent],
  template: `
    <div class="page">
      <app-page-header title="Mi negocio" subtitle="Datos del establecimiento y destino de las reseñas." />
      @if (loading()) { <app-loading /> }
      @else if (error()) { <app-error-state [error]="error()" (retry)="load()" /> }
      @else if (business(); as b) {
        <div class="grid grid-2">
          <section class="card">
            <div class="card-header"><h2>Datos</h2><app-status-badge [active]="b.active" /></div>
            @if (canEdit()) {
              <form [formGroup]="form" (ngSubmit)="save()" class="stack" novalidate>
                <div class="field"><label for="name">Nombre</label><input id="name" class="input" formControlName="name"></div>
                <div class="field"><label for="address">Dirección</label><input id="address" class="input" formControlName="address"></div>
                <div class="field"><label for="phone">Teléfono</label><input id="phone" class="input" formControlName="phone"></div>
                <div class="field"><label for="tz">Zona horaria</label>
                  <select id="tz" class="select" formControlName="timezone">@for (tz of timezones; track tz) { <option [value]="tz">{{ tz }}</option> }</select>
                  <span class="hint">Las estadísticas y fechas se muestran en esta zona.</span>
                </div>
                <div class="form-actions"><button class="btn btn-primary" type="submit" [disabled]="form.invalid || busy()">Guardar cambios</button></div>
              </form>
            } @else {
              <dl class="dl">
                <dt>Nombre</dt><dd>{{ b.name }}</dd>
                <dt>Dirección</dt><dd>{{ b.address || '—' }}</dd>
                <dt>Teléfono</dt><dd>{{ b.phone || '—' }}</dd>
                <dt>Zona horaria</dt><dd>{{ b.timezone }}</dd>
              </dl>
              <p class="small muted mt-2">Sólo el propietario puede editar estos datos.</p>
            }
          </section>

          <section class="card stack">
            <h2>Destino de las reseñas</h2>
            <p class="small muted">Es la página oficial de Google a la que llegan tus clientes al usar cualquier dispositivo.</p>
            @if (b.googleReviewUrl) {
              <div class="url-box"><span>{{ b.googleReviewUrl }}</span></div>
              <a class="btn" [href]="b.googleReviewUrl" target="_blank" rel="noopener noreferrer">Abrir en Google ↗</a>
            } @else {
              <div class="alert alert-warn">Todavía no hay una URL de reseñas configurada: los dispositivos no redirigirán hasta que se configure.</div>
            }
            <p class="small muted">Para cambiar la URL de destino contacta con tu proveedor.</p>
            <p class="small muted">Dispositivos activos: {{ b.deviceCount }}</p>
          </section>
        </div>
      }
    </div>
  `,
  styles: `.dl { display: grid; grid-template-columns: max-content 1fr; gap: 0.4rem 1.25rem; margin: 0; } .dl dt { color: var(--muted); } .dl dd { margin: 0; }`,
})
export class MyBusinessComponent {
  private readonly businessService = inject(BusinessService);
  private readonly session = inject(SessionStore);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  protected readonly business = signal<Business | null>(null);
  protected readonly loading = signal(true);
  protected readonly busy = signal(false);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly timezones = TIMEZONES;
  protected readonly canEdit = signal(false);

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(150)]],
    address: ['', Validators.maxLength(255)],
    phone: ['', Validators.maxLength(40)],
    timezone: ['Europe/Madrid', Validators.required],
  });

  constructor() {
    effect(() => {
      const biz = this.session.currentBusiness();
      if (biz) untracked(() => this.load());
      else this.loading.set(false);
    });
  }

  async load() {
    const biz = this.session.currentBusiness();
    if (!biz) return;
    this.loading.set(true);
    this.error.set(null);
    this.canEdit.set(biz.role === 'OWNER');
    try {
      const b = await this.businessService.get(biz.id);
      this.business.set(b);
      this.form.reset({ name: b.name, address: b.address ?? '', phone: b.phone ?? '', timezone: b.timezone });
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
    try {
      const v = this.form.getRawValue();
      const updated = await this.businessService.updateProfile(b.id, {
        name: v.name, address: v.address || null, phone: v.phone || null, timezone: v.timezone, logoUrl: b.logoUrl,
      });
      this.business.set(updated);
      this.session.patchBusiness(b.id, { name: updated.name, timezone: updated.timezone });
      this.toast.success('Datos guardados');
    } catch (e) {
      this.toast.error(toApiError(e).message);
    } finally {
      this.busy.set(false);
    }
  }
}
