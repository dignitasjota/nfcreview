import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AdminSummary, DeviceStats, PeriodQuery, RecentRow, Summary, Timeline } from '../models';
import { toHttpParams } from '../period';
import { triggerDownload } from './device.service';

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private readonly http = inject(HttpClient);

  summary(businessId: string, q: PeriodQuery): Promise<Summary> {
    return firstValueFrom(this.http.get<Summary>(`/api/businesses/${businessId}/analytics/summary`, { params: toHttpParams(q) }));
  }

  timeline(businessId: string, q: PeriodQuery): Promise<Timeline> {
    return firstValueFrom(this.http.get<Timeline>(`/api/businesses/${businessId}/analytics/timeline`, { params: toHttpParams(q) }));
  }

  devices(businessId: string, q: PeriodQuery): Promise<DeviceStats> {
    return firstValueFrom(this.http.get<DeviceStats>(`/api/businesses/${businessId}/analytics/devices`, { params: toHttpParams(q) }));
  }

  recent(businessId: string, limit = 20): Promise<RecentRow[]> {
    return firstValueFrom(this.http.get<RecentRow[]>(`/api/businesses/${businessId}/analytics/recent`, { params: { limit } }));
  }

  async exportCsv(businessId: string, from: string, to: string, deviceId?: string | null): Promise<void> {
    const params: Record<string, string> = { from, to };
    if (deviceId) params['deviceId'] = deviceId;
    const blob = await firstValueFrom(
      this.http.get(`/api/businesses/${businessId}/analytics/export.csv`, { params, responseType: 'blob' }),
    );
    triggerDownload(blob, `interacciones-${from}-${to}.csv`);
  }

  adminSummary(q: PeriodQuery): Promise<AdminSummary> {
    return firstValueFrom(this.http.get<AdminSummary>('/api/admin/analytics/summary', { params: toHttpParams(q) }));
  }
}
