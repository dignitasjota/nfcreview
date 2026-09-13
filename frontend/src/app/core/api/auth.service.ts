import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Me } from '../models';
import { SessionStore } from '../session.store';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly session = inject(SessionStore);
  private pending: Promise<Me | null> | null = null;

  /** Carga /me una sola vez (los guards la comparten). */
  ensureLoaded(): Promise<Me | null> {
    if (this.session.loaded()) return Promise.resolve(this.session.user());
    if (!this.pending) {
      this.pending = firstValueFrom(this.http.get<Me>('/api/auth/me'))
        .then((me) => {
          this.session.set(me);
          return me;
        })
        .catch(() => {
          this.session.clear();
          return null;
        })
        .finally(() => (this.pending = null));
    }
    return this.pending;
  }

  async login(email: string, password: string): Promise<Me> {
    const me = await firstValueFrom(this.http.post<Me>('/api/auth/login', { email, password }));
    this.session.set(me);
    return me;
  }

  async logout(): Promise<void> {
    try {
      await firstValueFrom(this.http.post<void>('/api/auth/logout', {}));
    } finally {
      this.session.clear();
    }
  }

  async refreshMe(): Promise<Me> {
    const me = await firstValueFrom(this.http.get<Me>('/api/auth/me'));
    this.session.set(me);
    return me;
  }

  forgotPassword(email: string): Promise<void> {
    return firstValueFrom(this.http.post<void>('/api/auth/forgot-password', { email }));
  }

  resetPassword(token: string, newPassword: string): Promise<void> {
    return firstValueFrom(this.http.post<void>('/api/auth/reset-password', { token, newPassword }));
  }

  changePassword(currentPassword: string, newPassword: string): Promise<void> {
    return firstValueFrom(this.http.post<void>('/api/auth/change-password', { currentPassword, newPassword }));
  }
}
