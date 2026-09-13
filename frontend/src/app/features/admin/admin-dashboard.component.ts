import { DecimalPipe } from '@angular/common';
import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AnalyticsService } from '../../core/api/analytics.service';
import { toApiError } from '../../core/interceptors/error.interceptor';
import { AdminSummary, ApiError, PeriodQuery } from '../../core/models';
import { isCompletePeriod } from '../../core/period';
import { ChartDirective } from '../../shared/ui/chart.directive';
import { EmptyStateComponent } from '../../shared/ui/empty-state.component';
import { ErrorStateComponent } from '../../shared/ui/error-state.component';
import { KpiCardComponent } from '../../shared/ui/kpi-card.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';
import { PeriodSelectorComponent } from '../../shared/ui/period-selector.component';
import { changeText, distributionChart, timelineChart } from '../shared/charts';

@Component({
  selector: 'app-admin-dashboard',
  imports: [DecimalPipe, RouterLink, PageHeaderComponent, PeriodSelectorComponent, KpiCardComponent, ChartDirective,
    EmptyStateComponent, ErrorStateComponent],
  template: `
    <div class="page">
      <app-page-header title="Dashboard global" subtitle="Actividad de todos los negocios de la plataforma.">
        <a routerLink="/admin/businesses/new" class="btn btn-primary">+ Nuevo negocio</a>
      </app-page-header>
      <app-period-selector [(query)]="query" />
      @if (error()) { <app-error-state [error]="error()" (retry)="load()" /> }

      <div class="grid grid-kpi">
        <app-kpi-card label="Interacciones" [value]="data()?.current?.total" [loading]="loading()" [accent]="true"
          [hint]="data() ? changeText(data()!.changePercent, data()!.previous.total) : null" />
        <app-kpi-card label="NFC" [value]="data()?.current?.nfc" [loading]="loading()" />
        <app-kpi-card label="QR" [value]="data()?.current?.qr" [loading]="loading()" />
        <app-kpi-card label="Negocios" [value]="data()?.businesses" [loading]="loading()" [hint]="data() ? data()!.activeBusinesses + ' activos' : null" />
        <app-kpi-card label="Dispositivos" [value]="data()?.devices" [loading]="loading()" [hint]="data() ? data()!.activeDevices + ' activos' : null" />
      </div>

      <div class="grid grid-main">
        <section class="card">
          <div class="card-header"><h2>Interacciones por día</h2></div>
          @if (loading()) { <div class="skeleton" style="height: 260px"></div> }
          @else if ((data()?.current?.total ?? 0) > 0) { <div class="chart-box"><canvas [appChart]="timelineConfig()"></canvas></div> }
          @else { <app-empty-state title="Sin interacciones en este periodo" /> }
        </section>
        <section class="card">
          <div class="card-header"><h2>Negocios más activos</h2></div>
          @if (loading()) { <div class="skeleton" style="height: 200px"></div> }
          @else if (data()?.topBusinesses?.length) {
            <table class="table">
              <tbody>
                @for (b of data()!.topBusinesses; track b.id) {
                  <tr><td><a class="row-link" [routerLink]="['/admin/businesses', b.id]">{{ b.name }}</a></td><td class="num">{{ b.total | number: '1.0-0' : 'es' }}</td></tr>
                }
              </tbody>
            </table>
          } @else { <app-empty-state title="Sin datos" /> }
          <div class="chart-box small-box mt-2">
            @if (!loading() && (data()?.current?.total ?? 0) > 0) { <canvas [appChart]="distributionConfig()"></canvas> }
          </div>
        </section>
      </div>
    </div>
  `,
  styles: `.chart-box { position: relative; height: 260px; } .small-box { height: 180px; }`,
})
export class AdminDashboardComponent {
  private readonly analytics = inject(AnalyticsService);
  protected readonly query = signal<PeriodQuery>({ period: '30d' });
  protected readonly data = signal<AdminSummary | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly changeText = changeText;
  protected readonly timelineConfig = computed(() => timelineChart(this.data()?.timeline ?? []));
  protected readonly distributionConfig = computed(() => {
    const c = this.data()?.current;
    return distributionChart(c?.nfc ?? 0, c?.qr ?? 0, c?.unknown ?? 0);
  });
  private seq = 0;

  constructor() {
    effect(() => {
      const q = this.query();
      if (isCompletePeriod(q)) untracked(() => this.load());
    });
  }

  async load() {
    const seq = ++this.seq;
    this.loading.set(true);
    this.error.set(null);
    try {
      const res = await this.analytics.adminSummary(this.query());
      if (seq === this.seq) this.data.set(res);
    } catch (e) {
      if (seq === this.seq) this.error.set(toApiError(e));
    } finally {
      if (seq === this.seq) this.loading.set(false);
    }
  }
}
