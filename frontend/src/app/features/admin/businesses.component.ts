import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { BusinessService } from '../../core/api/business.service';
import { toApiError } from '../../core/interceptors/error.interceptor';
import { ApiError, Business } from '../../core/models';
import { StatusBadgeComponent } from '../../shared/ui/badges.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state.component';
import { ErrorStateComponent } from '../../shared/ui/error-state.component';
import { LoadingComponent } from '../../shared/ui/loading.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';

@Component({
  selector: 'app-businesses',
  imports: [FormsModule, RouterLink, PageHeaderComponent, LoadingComponent, EmptyStateComponent, ErrorStateComponent, StatusBadgeComponent],
  template: `
    <div class="page">
      <app-page-header title="Negocios" subtitle="Clientes con dispositivos NFC/QR.">
        <a routerLink="/admin/businesses/new" class="btn btn-primary">+ Nuevo negocio</a>
      </app-page-header>
      <div class="card">
        <div class="row-between" style="margin-bottom: 1rem">
          <input class="input" style="max-width: 320px" placeholder="Buscar por nombre…" [ngModel]="filter()" (ngModelChange)="filter.set($event)" aria-label="Buscar">
          <span class="small muted">{{ filtered().length }} de {{ all().length }}</span>
        </div>
        @if (loading()) { <app-loading /> }
        @else if (error()) { <app-error-state [error]="error()" (retry)="load()" /> }
        @else if (all().length === 0) {
          <app-empty-state title="Todavía no hay negocios" text="Da de alta el primero para empezar a generar dispositivos." icon="⌂">
            <a routerLink="/admin/businesses/new" class="btn btn-primary">Crear negocio</a>
          </app-empty-state>
        } @else {
          <div class="table-wrap">
            <table class="table">
              <thead><tr><th>Nombre</th><th>URL de reseñas</th><th>Zona horaria</th><th class="num">Dispositivos</th><th>Estado</th></tr></thead>
              <tbody>
                @for (b of filtered(); track b.id) {
                  <tr>
                    <td><a class="row-link" [routerLink]="['/admin/businesses', b.id]">{{ b.name }}</a><div class="small muted">{{ b.slug }}</div></td>
                    <td>@if (b.googleReviewUrl) { <span class="badge badge-success">Configurada</span> } @else { <span class="badge badge-danger">Sin configurar</span> }</td>
                    <td class="muted">{{ b.timezone }}</td>
                    <td class="num">{{ b.deviceCount }}</td>
                    <td><app-status-badge [active]="b.active" /></td>
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
export class BusinessesComponent {
  private readonly businessService = inject(BusinessService);
  protected readonly all = signal<Business[]>([]);
  protected readonly filter = signal('');
  protected readonly loading = signal(true);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly filtered = computed(() => {
    const f = this.filter().trim().toLowerCase();
    return f ? this.all().filter((b) => b.name.toLowerCase().includes(f) || b.slug.includes(f)) : this.all();
  });

  constructor() {
    void this.load();
  }

  async load() {
    this.loading.set(true);
    this.error.set(null);
    try {
      this.all.set(await this.businessService.list());
    } catch (e) {
      this.error.set(toApiError(e));
    } finally {
      this.loading.set(false);
    }
  }
}
