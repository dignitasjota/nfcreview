import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AppUser, UserCreateRequest, UserCreated } from '../models';

/** Endpoints /api/admin/users (sólo ADMIN). */
@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);

  list(): Promise<AppUser[]> {
    return firstValueFrom(this.http.get<AppUser[]>('/api/admin/users'));
  }

  create(req: UserCreateRequest): Promise<UserCreated> {
    return firstValueFrom(this.http.post<UserCreated>('/api/admin/users', req));
  }

  setEnabled(id: string, enabled: boolean): Promise<AppUser> {
    return firstValueFrom(this.http.patch<AppUser>(`/api/admin/users/${id}/status`, { enabled }));
  }

  resetPassword(id: string, newPassword: string): Promise<void> {
    return firstValueFrom(this.http.post<void>(`/api/admin/users/${id}/reset-password`, { newPassword }));
  }
}
