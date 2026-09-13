import { DecimalPipe } from '@angular/common';
import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AnalyticsService } from '../../core/api/analytics.service';
import { DeviceService } from '../../core/api/device.service';
import { toApiError } from '../../core/interceptors/error.interceptor';
import { ApiError, Device, DeviceStatsRow, DeviceType, PeriodQuery } from '../../core/models';
import { isCompletePeriod } from '../../core/period';
import { SessionStore } from '../../core/session.store';
import { ToastService } from '../../core/toast.service';
import { DEVICE_TYPE_LABEL, DeviceTypeBadgeComponent, StatusBadgeComponent } from '../../shared/ui/badges.component';
import { CopyButtonComponent } from '../../shared/ui/copy-button.component';
import { ErrorStateComponent } from '../../shared/ui/error-state.component';
import { KpiCardComponent } from '../../shared/ui/kpi-card.component';
import { LoadingComponent } from '../../shared/ui/loading.component';
import { ModalComponent } from '../../shared/ui/modal.component';
import { PeriodSelectorComponent } from '../../shared/ui/period-selector.component';

@Component({
  selector: 'app-device-detail',
  imports: [DecimalPipe, RouterLink, ReactiveFormsModule, LoadingComponent, ErrorStateComponent, StatusBadgeComponent,
    DeviceTypeBadgeComponent, CopyButtonComponent, KpiCardComponent, PeriodSelectorComponent, ModalComponent],
  template: `
    @if (loading()) { <app-loading /> }
    @else if (error()) { <app-error-state [error]="error()" (retry)="load()" /> }
    @else if (device(); as d) {
      <div class="page">
        <a [routerLink]="backLink()" class="small">← {{ isAdmin() ? 'Volver al negocio' : 'Volver a dispositivos' }}</a>

        <div class="row-between">
          <div>
            <p class="eyebrow">{{ isAdmin() ? d.businessName : 'Dispositivo' }}</p>
            <h1 class="row" style="gap:.6rem">{{ d.name }} <app-device-type-badge [type]="d.type" /> <app-status-badge [active]="d.active" /></h1>
            <p class="muted">{{ d.locationDescription || 'Sin ubicación' }} · Código <code>{{ d.publicCode }}</code></p>
          </div>
          @if (isAdmin()) {
            <div class="row">
              <button class="btn" type="button" (click)="openEdit()">Editar</button>
              <button class="btn" [class.btn-danger]="d.active" type="button" (click)="toggleActive()" [disabled]="busy()">
                {{ d.active ? 'Desactivar' : 'Activar' }}
              </button>
            </div>
          }
        </div>

        @if (!d.active) {
          <div class="alert alert-warn">Este dispositivo está desactivado: su URL pública muestra una página de "enlace no disponible" y no redirige a Google.</div>
        }

        <div class="grid grid-2">
          <section class="card prog">
            <p class="card-title">Programación NFC</p>
            <p class="small muted">Graba esta URL en el chip NFC (registro NDEF de tipo URL).</p>
            <div class="url-box"><span>{{ d.nfcUrl }}</span></div>
            <div class="row"><app-copy-button [text]="d.nfcUrl" [primary]="true" /></div>
          </section>

          <section class="card prog">
            <p class="card-title">Programación QR</p>
            <p class="small muted">Este QR ya contiene la URL. Descárgalo para imprimirlo.</p>
            <div class="url-box"><span>{{ d.qrUrl }}</span></div>
            <div class="row">
              <app-copy-button [text]="d.qrUrl" />
              <button class="btn" type="button" (click)="showQr.set(!showQr())">{{ showQr() ? 'Ocultar QR' : 'Mostrar QR' }}</button>
              <button class="btn" type="button" (click)="download('png')" [disabled]="busy()">Descargar PNG</button>
              <button class="btn" type="button" (click)="download('svg')" [disabled]="busy()">Descargar SVG</button>
            </div>
            @if (showQr()) {
              <div class="qr"><img [src]="qrUrl()" width="220" height="220" alt="Código QR del dispositivo {{ d.name }}"></div>
            }
          </section>
        </div>

        <section class="card">
          <div class="card-header">
            <h2>Estadísticas del dispositivo</h2>
            <button class="btn btn-sm" type="button" (click)="exportCsv()" [disabled]="busy() || !stats()">⤓ CSV</button>
          </div>
          <app-period-selector [(query)]="query" [timezone]="device()?.businessTimezone ?? 'Europe/Madrid'" />
          <div class="grid grid-kpi mt-2">
            <app-kpi-card label="Interacciones" [value]="stats()?.total" [loading]="statsLoading()" [accent]="true" />
            <app-kpi-card label="NFC" [value]="stats()?.nfc" [loading]="statsLoading()" />
            <app-kpi-card label="QR" [value]="stats()?.qr" [loading]="statsLoading()" />
            <app-kpi-card label="Últimos 30 días" [value]="d.interactionsLast30Days" />
          </div>
          @if (statsError()) { <div class="mt-2"><app-error-state [error]="statsError()" (retry)="loadStats()" /></div> }
          <p class="small muted mt-2">Total del periodo: {{ stats()?.total ?? 0 | number: '1.0-0' : 'es' }} interacciones (aperturas del enlace, no reseñas publicadas).</p>
        </section>
      </div>

      @if (editing()) {
        <app-modal title="Editar dispositivo" (close)="editing.set(false)">
          <form [formGroup]="form" (ngSubmit)="saveEdit()" class="stack" novalidate>
            <div class="field"><label for="name">Nombre</label><input id="name" class="input" formControlName="name"></div>
            <div class="field"><label for="loc">Ubicación</label><input id="loc" class="input" formControlName="locationDescription" placeholder="Mostrador, entrada, mesa 4…"></div>
            <div class="field"><label for="type">Tipo</label>
              <select id="type" class="select" formControlName="type">
                @for (t of types; track t) { <option [value]="t">{{ typeLabel[t] }}</option> }
              </select>
            </div>
            <div class="form-actions">
              <button type="button" class="btn" (click)="editing.set(false)">Cancelar</button>
              <button type="submit" class="btn btn-primary" [disabled]="form.invalid || busy()">Guardar</button>
            </div>
          </form>
        </app-modal>
      }
    }
  `,
  styles: `
    .eyebrow { font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.08em; color: var(--brand); font-weight: 600; }
    .prog { display: grid; gap: 0.75rem; align-content: start; }
    .qr { display: flex; justify-content: center; padding: 1rem; background: #fff; border: 1px dashed var(--line); border-radius: 10px; }
    .qr img { image-rendering: pixelated; }
  `,
})
export class DeviceDetailComponent {
  readonly id = input.required<string>();

  private readonly deviceService = inject(DeviceService);
  private readonly analytics = inject(AnalyticsService);
  private readonly session = inject(SessionStore);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  protected readonly device = signal<Device | null>(null);
  protected readonly loading = signal(true);
  protected readonly busy = signal(false);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly showQr = signal(true);
  protected readonly editing = signal(false);
  protected readonly query = signal<PeriodQuery>({ period: '30d' });
  protected readonly stats = signal<DeviceStatsRow | null>(null);
  protected readonly statsLoading = signal(false);
  protected readonly statsError = signal<ApiError | null>(null);
  protected readonly isAdmin = this.session.isAdmin;
  protected readonly types: DeviceType[] = ['NFC_QR', 'NFC', 'QR'];
  protected readonly typeLabel = DEVICE_TYPE_LABEL;

  protected readonly qrUrl = computed(() => (this.device() ? this.deviceService.qrPngUrl(this.device()!.id, 440) : ''));
  protected readonly backLink = computed(() =>
    this.isAdmin() ? ['/admin/businesses', this.device()?.businessId ?? ''] : ['/app/devices'],
  );

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    locationDescription: ['', Validators.maxLength(200)],
    type: ['NFC_QR' as DeviceType, Validators.required],
  });

  constructor() {
    effect(() => {
      this.id();
      untracked(() => this.load());
    });
    effect(() => {
      const d = this.device();
      const q = this.query();
      if (d && isCompletePeriod(q)) untracked(() => this.loadStats());
    });
  }

  async load() {
    this.loading.set(true);
    this.error.set(null);
    try {
      this.device.set(await this.deviceService.get(this.id()));
    } catch (e) {
      this.error.set(toApiError(e));
    } finally {
      this.loading.set(false);
    }
  }

  async loadStats() {
    const d = this.device();
    if (!d) return;
    this.statsLoading.set(true);
    this.statsError.set(null);
    try {
      const res = await this.analytics.devices(d.businessId, this.query());
      this.stats.set(res.devices.find((r) => r.deviceId === d.id) ?? { deviceId: d.id, name: d.name, location: d.locationDescription, active: d.active, nfc: 0, qr: 0, unknown: 0, total: 0 });
    } catch (e) {
      this.statsError.set(toApiError(e));
    } finally {
      this.statsLoading.set(false);
    }
  }

  async download(format: 'png' | 'svg') {
    const d = this.device();
    if (!d) return;
    this.busy.set(true);
    try {
      await this.deviceService.download(d.id, format, `qr-${slug(d.name)}-${d.publicCode}.${format}`);
    } catch (e) {
      this.toast.error(toApiError(e).message);
    } finally {
      this.busy.set(false);
    }
  }

  async exportCsv() {
    const d = this.device();
    if (!d) return;
    const res = await this.analytics.devices(d.businessId, this.query()).catch(() => null);
    if (!res) return;
    this.busy.set(true);
    try {
      await this.analytics.exportCsv(d.businessId, res.period.from, res.period.to, d.id);
    } catch (e) {
      this.toast.error(toApiError(e).message);
    } finally {
      this.busy.set(false);
    }
  }

  async toggleActive() {
    const d = this.device();
    if (!d) return;
    const next = !d.active;
    if (!next && !confirm(`¿Desactivar "${d.name}"? Su URL dejará de redirigir a Google.`)) return;
    this.busy.set(true);
    try {
      this.device.set(await this.deviceService.setActive(d.id, next));
      this.toast.success(next ? 'Dispositivo activado' : 'Dispositivo desactivado');
    } catch (e) {
      this.toast.error(toApiError(e).message);
    } finally {
      this.busy.set(false);
    }
  }

  openEdit() {
    const d = this.device();
    if (!d) return;
    this.form.reset({ name: d.name, locationDescription: d.locationDescription ?? '', type: d.type });
    this.editing.set(true);
  }

  async saveEdit() {
    const d = this.device();
    if (!d || this.form.invalid) return;
    this.busy.set(true);
    try {
      const v = this.form.getRawValue();
      this.device.set(await this.deviceService.update(d.id, { name: v.name, locationDescription: v.locationDescription || null, type: v.type }));
      this.editing.set(false);
      this.toast.success('Dispositivo actualizado');
    } catch (e) {
      this.toast.error(toApiError(e).message);
    } finally {
      this.busy.set(false);
    }
  }
}

function slug(s: string) {
  return s.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[^A-Za-z0-9]+/g, '-').replace(/(^-|-$)/g, '').toLowerCase() || 'dispositivo';
}
