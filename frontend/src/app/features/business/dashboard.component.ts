import { DecimalPipe } from '@angular/common';
import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AnalyticsService } from '../../core/api/analytics.service';
import { toApiError } from '../../core/interceptors/error.interceptor';
import { ApiError, DeviceStats, DeviceStatsRow, PeriodQuery, RecentRow, Summary, Timeline } from '../../core/models';
import { isCompletePeriod, periodLabel } from '../../core/period';
import { SessionStore } from '../../core/session.store';
import { ToastService } from '../../core/toast.service';
import { TzDatePipe } from '../../core/tz-date.pipe';
import { InteractionBadgeComponent } from '../../shared/ui/badges.component';
import { ChartDirective } from '../../shared/ui/chart.directive';
import { EmptyStateComponent } from '../../shared/ui/empty-state.component';
import { ErrorStateComponent } from '../../shared/ui/error-state.component';
import { KpiCardComponent } from '../../shared/ui/kpi-card.component';
import { PeriodSelectorComponent } from '../../shared/ui/period-selector.component';
import { changeText, distributionChart, percent, timelineChart } from '../shared/charts';

type SortKey = 'name' | 'nfc' | 'qr' | 'total';

@Component({
  selector: 'app-dashboard',
  imports: [DecimalPipe, RouterLink, TzDatePipe, KpiCardComponent, PeriodSelectorComponent, ChartDirective,
    EmptyStateComponent, ErrorStateComponent, InteractionBadgeComponent],
  template: `
    @if (business(); as biz) {
      <div class="page">
        <div class="row-between">
          <div>
            <h1>Hola, {{ biz.name }}</h1>
            <p class="muted">Interacciones con tus dispositivos · {{ periodLabel(query().period) }}</p>
          </div>
          <button class="btn" type="button" (click)="exportCsv()" [disabled]="exporting()">{{ exporting() ? 'Exportando…' : '⤓ Exportar CSV' }}</button>
        </div>

        <app-period-selector [(query)]="query" [timezone]="biz.timezone" />

        @if (error()) { <app-error-state [error]="error()" (retry)="load()" /> }

        <div class="grid grid-kpi">
          <app-kpi-card label="Interacciones" [value]="summary()?.current?.total" [loading]="loading()" [accent]="true"
            [hint]="summary() ? changeText(summary()!.changePercent, summary()!.previous.total) : null" />
          <app-kpi-card label="NFC" [value]="summary()?.current?.nfc" [loading]="loading()"
            [hint]="summary() ? percent(summary()!.current.nfc, summary()!.current.total) + ' del total' : null" />
          <app-kpi-card label="QR" [value]="summary()?.current?.qr" [loading]="loading()"
            [hint]="summary() ? percent(summary()!.current.qr, summary()!.current.total) + ' del total' : null" />
          <app-kpi-card label="Dispositivo más utilizado" [value]="summary()?.topDevice?.name ?? (summary() ? 'Sin datos' : null)" [loading]="loading()"
            [hint]="summary()?.topDevice ? (summary()!.topDevice!.total | number: '1.0-0' : 'es') + ' interacciones' : null" />
        </div>

        <div class="grid grid-main">
          <section class="card">
            <div class="card-header"><h2>Interacciones por día</h2></div>
            @if (loading()) { <div class="skeleton" style="height: 260px"></div> }
            @else if (hasData()) { <div class="chart-box"><canvas [appChart]="timelineConfig()"></canvas></div> }
            @else { <app-empty-state title="Sin interacciones en este periodo" text="Cuando alguien use tus dispositivos aparecerán aquí." /> }
          </section>
          <section class="card">
            <div class="card-header"><h2>Distribución NFC / QR</h2></div>
            @if (loading()) { <div class="skeleton" style="height: 260px"></div> }
            @else if (hasData()) { <div class="chart-box"><canvas [appChart]="distributionConfig()"></canvas></div> }
            @else { <app-empty-state title="Sin datos" /> }
          </section>
        </div>

        <section class="card">
          <div class="card-header">
            <h2>Rendimiento por dispositivo</h2>
            <a routerLink="/app/devices" class="small">Ver dispositivos →</a>
          </div>
          @if (loading()) { <div class="skeleton" style="height: 120px"></div> }
          @else if (devices().length === 0) { <app-empty-state title="Todavía no tienes dispositivos" text="Tu proveedor los dará de alta y aparecerán aquí." /> }
          @else {
            <div class="table-wrap">
              <table class="table">
                <thead>
                  <tr>
                    <th class="sortable" (click)="sortBy('name')">Dispositivo {{ arrow('name') }}</th>
                    <th>Ubicación</th>
                    <th class="num sortable" (click)="sortBy('nfc')">NFC {{ arrow('nfc') }}</th>
                    <th class="num sortable" (click)="sortBy('qr')">QR {{ arrow('qr') }}</th>
                    <th class="num sortable" (click)="sortBy('total')">Total {{ arrow('total') }}</th>
                  </tr>
                </thead>
                <tbody>
                  @for (d of sortedDevices(); track d.deviceId) {
                    <tr>
                      <td><a class="row-link" [routerLink]="['/app/devices', d.deviceId]">{{ d.name }}</a>
                        @if (!d.active) { <span class="badge badge-muted" style="margin-left:.4rem">Inactivo</span> }</td>
                      <td class="muted">{{ d.location || '—' }}</td>
                      <td class="num">{{ d.nfc | number: '1.0-0' : 'es' }}</td>
                      <td class="num">{{ d.qr | number: '1.0-0' : 'es' }}</td>
                      <td class="num"><strong>{{ d.total | number: '1.0-0' : 'es' }}</strong></td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </section>

        <section class="card">
          <div class="card-header"><h2>Últimas interacciones</h2><span class="small muted">Hora local · {{ biz.timezone }}</span></div>
          @if (loading()) { <div class="skeleton" style="height: 120px"></div> }
          @else if (recent().length === 0) { <app-empty-state title="Aún no hay interacciones" /> }
          @else {
            <div class="table-wrap">
              <table class="table">
                <thead><tr><th>Fecha / hora</th><th>Dispositivo</th><th>Tipo</th></tr></thead>
                <tbody>
                  @for (r of recent(); track $index) {
                    <tr>
                      <td>{{ r.createdAt | tzDate: biz.timezone }}</td>
                      <td>{{ r.deviceName }} <span class="muted small">{{ r.location ? '· ' + r.location : '' }}</span></td>
                      <td><app-interaction-badge [type]="r.type" /></td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </section>

        <p class="small muted">Una interacción es una apertura del enlace de reseñas desde un dispositivo. No equivale a una reseña publicada en Google.</p>
      </div>
    } @else {
      <app-empty-state title="No tienes ningún negocio asignado" text="Pide a tu proveedor que te asocie a tu negocio." icon="⌂" />
    }
  `,
  styles: `.chart-box { position: relative; height: 260px; }`,
})
export class DashboardComponent {
  private readonly analytics = inject(AnalyticsService);
  private readonly session = inject(SessionStore);
  private readonly toast = inject(ToastService);

  protected readonly business = this.session.currentBusiness;
  protected readonly query = signal<PeriodQuery>({ period: '30d' });
  protected readonly loading = signal(true);
  protected readonly exporting = signal(false);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly summary = signal<Summary | null>(null);
  protected readonly timeline = signal<Timeline | null>(null);
  protected readonly deviceStats = signal<DeviceStats | null>(null);
  protected readonly recent = signal<RecentRow[]>([]);
  protected readonly sort = signal<{ key: SortKey; dir: 1 | -1 }>({ key: 'total', dir: -1 });

  protected readonly periodLabel = periodLabel;
  protected readonly percent = percent;
  protected readonly changeText = changeText;

  protected readonly hasData = computed(() => (this.summary()?.current.total ?? 0) > 0);
  protected readonly timelineConfig = computed(() => timelineChart(this.timeline()?.points ?? []));
  protected readonly distributionConfig = computed(() => {
    const c = this.summary()?.current;
    return distributionChart(c?.nfc ?? 0, c?.qr ?? 0, c?.unknown ?? 0);
  });
  protected readonly devices = computed<DeviceStatsRow[]>(() => this.deviceStats()?.devices ?? []);
  protected readonly sortedDevices = computed(() => {
    const { key, dir } = this.sort();
    return [...this.devices()].sort((a, b) => {
      const va = a[key];
      const vb = b[key];
      const cmp = typeof va === 'string' ? va.localeCompare(String(vb), 'es') : Number(va) - Number(vb);
      return cmp * dir;
    });
  });

  private requestSeq = 0;

  constructor() {
    effect(() => {
      const biz = this.business();
      const q = this.query();
      if (!biz || !isCompletePeriod(q)) return;
      untracked(() => this.load());
    });
  }

  async load() {
    const biz = this.business();
    if (!biz) return;
    const seq = ++this.requestSeq;
    this.loading.set(true);
    this.error.set(null);
    try {
      const q = this.query();
      const [summary, timeline, devices, recent] = await Promise.all([
        this.analytics.summary(biz.id, q),
        this.analytics.timeline(biz.id, q),
        this.analytics.devices(biz.id, q),
        this.analytics.recent(biz.id, 15),
      ]);
      if (seq !== this.requestSeq) return;
      this.summary.set(summary);
      this.timeline.set(timeline);
      this.deviceStats.set(devices);
      this.recent.set(recent);
    } catch (e) {
      if (seq === this.requestSeq) this.error.set(toApiError(e));
    } finally {
      if (seq === this.requestSeq) this.loading.set(false);
    }
  }

  sortBy(key: SortKey) {
    this.sort.update((s) => (s.key === key ? { key, dir: s.dir === 1 ? -1 : 1 } : { key, dir: key === 'name' ? 1 : -1 }));
  }

  arrow(key: SortKey) {
    const s = this.sort();
    return s.key === key ? (s.dir === 1 ? '↑' : '↓') : '';
  }

  async exportCsv() {
    const biz = this.business();
    const period = this.summary()?.period;
    if (!biz || !period) return;
    this.exporting.set(true);
    try {
      await this.analytics.exportCsv(biz.id, period.from, period.to);
    } catch (e) {
      this.toast.error(toApiError(e).message);
    } finally {
      this.exporting.set(false);
    }
  }
}
