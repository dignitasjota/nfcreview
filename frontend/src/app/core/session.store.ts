import { Injectable, computed, signal } from '@angular/core';
import { BusinessMembership, Me } from './models';

const BUSINESS_KEY = 'rt.currentBusiness';

/** Estado de sesión en memoria (señales). El token nunca llega aquí: vive en la cookie HttpOnly. */
@Injectable({ providedIn: 'root' })
export class SessionStore {
  readonly user = signal<Me | null>(null);
  readonly loaded = signal(false);
  private readonly selectedBusinessId = signal<string | null>(readStored());

  readonly isAdmin = computed(() => this.user()?.role === 'ADMIN');
  readonly businesses = computed<BusinessMembership[]>(() => this.user()?.businesses ?? []);

  /** Negocio activo para un BUSINESS_USER (el primero si no hay selección válida). */
  readonly currentBusiness = computed<BusinessMembership | null>(() => {
    const list = this.businesses();
    if (list.length === 0) return null;
    const wanted = this.selectedBusinessId();
    return list.find((b) => b.id === wanted) ?? list[0];
  });

  readonly displayName = computed(() => {
    const u = this.user();
    return u ? `${u.firstName} ${u.lastName}`.trim() || u.email : '';
  });

  set(user: Me | null) {
    this.user.set(user);
    this.loaded.set(true);
  }

  clear() {
    this.user.set(null);
    this.loaded.set(true);
  }

  selectBusiness(id: string) {
    this.selectedBusinessId.set(id);
    try {
      localStorage.setItem(BUSINESS_KEY, id);
    } catch {
      /* almacenamiento no disponible: la selección sólo dura la sesión */
    }
  }

  /** Aplica cambios locales (p. ej. renombrar el negocio) sin recargar /me. */
  patchBusiness(id: string, patch: Partial<BusinessMembership>) {
    const u = this.user();
    if (!u) return;
    this.user.set({ ...u, businesses: u.businesses.map((b) => (b.id === id ? { ...b, ...patch } : b)) });
  }
}

function readStored(): string | null {
  try {
    return localStorage.getItem(BUSINESS_KEY);
  } catch {
    return null;
  }
}
