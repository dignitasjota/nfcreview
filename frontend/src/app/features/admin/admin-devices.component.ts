import { DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { DeviceService } from '../../core/api/device.service';
import { toApiError } from '../../core/interceptors/error.interceptor';
import { ApiError, Device } from '../../core/models';
import { DeviceTypeBadgeComponent, StatusBadgeComponent } from '../../shared/ui/badges.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state.component';
import { ErrorStateComponent } from '../../shared/ui/error-state.component';
import { LoadingComponent } from '../../shared/ui/loading.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';

@Component({
  selector: 'app-admin-devices',
  imports: [DecimalPipe, FormsModule, RouterLink, PageHeaderComponent, LoadingComponent, ErrorStateComponent, EmptyStateComponent, StatusBadgeComponent, DeviceTypeBadgeComponent],
  template: `
    <div class="page">
      <app-page-header title="Dispositivos" subtitle="Todos los dispositivos de la plataforma. Se crean desde la ficha de cada negocio." />
      <div class="card">
        <div class="row-between" style="margin-bottom: 1rem">
          <input class="input" style="max-width: 320px" placeholder="Buscar por nombre, negocio o código…" [ngModel]="filter()" (ngModelChange)="filter.set($event)" aria-label="Buscar">
          <span class="small muted">{{ filtered().length }} de {{ all().length }}</span>
        </div>
        @if (loading()) { <app-loading /> }
        @else if (error()) { <app-error-state [error]="error()" (retry)="load()" /> }
        @else if (all().length === 0) { <app-empty-state title="No hay dispositivos" text="Crea uno desde un negocio." icon="◫"><a routerLink="/admin/businesses" class="btn">Ir a negocios</a></app-empty-state> }
        @else {
          <div class="table-wrap">
            <table class="table">
              <thead><tr><th>Dispositivo</th><th>Negocio</th><th>Tipo</th><th>Código</th><th>Estado</th><th class="num">30 días</th></tr></thead>
              <tbody>
                @for (d of filtered(); track d.id) {
                  <tr>
                    <td><a class="row-link" [routerLink]="['/admin/devices', d.id]">{{ d.name }}</a><div class="small muted">{{ d.locationDescription || '' }}</div></td>
                    <td><a [routerLink]="['/admin/businesses', d.businessId]">{{ d.businessName }}</a></td>
                    <td><app-device-type-badge [type]="d.type" /></td>
                    <td><code>{{ d.publicCode }}</code></td>
                    <td><app-status-badge [active]="d.active" /></td>
                    <td class="num">{{ d.interactionsLast30Days | number: '1.0-0' : 'es' }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </div>
    </div>
  `,
})
export class AdminDevicesComponent {
  private readonly deviceService = inject(DeviceService);
  protected readonly all = signal<Device[]>([]);
  protected readonly filter = signal('');
  protected readonly loading = signal(true);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly filtered = computed(() => {
    const f = this.filter().trim().toLowerCase();
    return f ? this.all().filter((d) => [d.name, d.businessName, d.publicCode, d.locationDescription ?? ''].some((s) => s.toLowerCase().includes(f))) : this.all();
  });

  constructor() {
    void this.load();
  }

  async load() {
    this.loading.set(true);
    this.error.set(null);
    try {
      this.all.set(await this.deviceService.listAll());
    } catch (e) {
      this.error.set(toApiError(e));
    } finally {
      this.loading.set(false);
    }
  }
}
