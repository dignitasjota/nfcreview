import { DecimalPipe } from '@angular/common';
import { Component, effect, inject, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DeviceService } from '../../core/api/device.service';
import { toApiError } from '../../core/interceptors/error.interceptor';
import { ApiError, Device } from '../../core/models';
import { SessionStore } from '../../core/session.store';
import { DeviceTypeBadgeComponent, StatusBadgeComponent } from '../../shared/ui/badges.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state.component';
import { ErrorStateComponent } from '../../shared/ui/error-state.component';
import { LoadingComponent } from '../../shared/ui/loading.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';

@Component({
  selector: 'app-devices',
  imports: [DecimalPipe, RouterLink, PageHeaderComponent, LoadingComponent, EmptyStateComponent, ErrorStateComponent,
    StatusBadgeComponent, DeviceTypeBadgeComponent],
  template: `
    <div class="page">
      <app-page-header title="Dispositivos" subtitle="Tarjetas, placas y pegatinas NFC/QR de tu negocio." />
      @if (loading()) { <app-loading /> }
      @else if (error()) { <app-error-state [error]="error()" (retry)="load()" /> }
      @else if (devices().length === 0) {
        <div class="card"><app-empty-state title="Todavía no tienes dispositivos" text="Tu proveedor los dará de alta y aparecerán aquí." icon="◫" /></div>
      } @else {
        <div class="card">
          <div class="table-wrap">
            <table class="table">
              <thead><tr><th>Nombre</th><th>Tipo</th><th>Ubicación</th><th>Estado</th><th class="num">Últimos 30 días</th></tr></thead>
              <tbody>
                @for (d of devices(); track d.id) {
                  <tr>
                    <td><a class="row-link" [routerLink]="['/app/devices', d.id]">{{ d.name }}</a></td>
                    <td><app-device-type-badge [type]="d.type" /></td>
                    <td class="muted">{{ d.locationDescription || '—' }}</td>
                    <td><app-status-badge [active]="d.active" /></td>
                    <td class="num"><strong>{{ d.interactionsLast30Days | number: '1.0-0' : 'es' }}</strong></td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </div>
      }
    </div>
  `,
})
export class DevicesComponent {
  private readonly deviceService = inject(DeviceService);
  private readonly session = inject(SessionStore);
  protected readonly devices = signal<Device[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<ApiError | null>(null);

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
    try {
      this.devices.set(await this.deviceService.listByBusiness(biz.id));
    } catch (e) {
      this.error.set(toApiError(e));
    } finally {
      this.loading.set(false);
    }
  }
}
