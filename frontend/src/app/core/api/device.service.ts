import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Device, DeviceRequest } from '../models';

@Injectable({ providedIn: 'root' })
export class DeviceService {
  private readonly http = inject(HttpClient);

  listByBusiness(businessId: string): Promise<Device[]> {
    return firstValueFrom(this.http.get<Device[]>(`/api/businesses/${businessId}/devices`));
  }

  listAll(): Promise<Device[]> {
    return firstValueFrom(this.http.get<Device[]>('/api/admin/devices'));
  }

  get(id: string): Promise<Device> {
    return firstValueFrom(this.http.get<Device>(`/api/devices/${id}`));
  }

  create(businessId: string, req: DeviceRequest): Promise<Device> {
    return firstValueFrom(this.http.post<Device>(`/api/businesses/${businessId}/devices`, req));
  }

  update(id: string, req: DeviceRequest): Promise<Device> {
    return firstValueFrom(this.http.put<Device>(`/api/devices/${id}`, req));
  }

  setActive(id: string, active: boolean): Promise<Device> {
    return firstValueFrom(this.http.patch<Device>(`/api/devices/${id}/status`, { active }));
  }

  qrPngUrl(id: string, size = 512): string {
    return `/api/devices/${id}/qr.png?size=${size}`;
  }

  /** Descarga con la cookie de sesión: se pide como blob y se dispara un enlace temporal. */
  async download(id: string, format: 'png' | 'svg', filename: string): Promise<void> {
    const url = format === 'png' ? `/api/devices/${id}/qr.png?size=1024` : `/api/devices/${id}/qr.svg`;
    const blob = await firstValueFrom(this.http.get(url, { responseType: 'blob' }));
    triggerDownload(blob, filename);
  }
}

export function triggerDownload(blob: Blob, filename: string) {
  const href = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = href;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(href), 1000);
}
