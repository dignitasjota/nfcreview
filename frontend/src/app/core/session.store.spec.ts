import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';
import { Me } from './models';
import { SessionStore } from './session.store';

const me: Me = {
  id: 'u1', email: 'pepe@test.local', firstName: 'Pepe', lastName: 'García', role: 'BUSINESS_USER',
  businesses: [
    { id: 'b1', name: 'Barbería', slug: 'barberia', timezone: 'Europe/Madrid', active: true, role: 'OWNER' },
    { id: 'b2', name: 'Cafetería', slug: 'cafeteria', timezone: 'Europe/Madrid', active: true, role: 'MANAGER' },
  ],
};

describe('SessionStore', () => {
  let store: SessionStore;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
    store = TestBed.inject(SessionStore);
  });

  it('selecciona el primer negocio por defecto y respeta la selección', () => {
    store.set(me);
    expect(store.currentBusiness()?.id).toBe('b1');
    store.selectBusiness('b2');
    expect(store.currentBusiness()?.id).toBe('b2');
    store.selectBusiness('inexistente');
    expect(store.currentBusiness()?.id).toBe('b1');
  });

  it('distingue administradores y limpia la sesión', () => {
    store.set({ ...me, role: 'ADMIN', businesses: [] });
    expect(store.isAdmin()).toBe(true);
    expect(store.currentBusiness()).toBeNull();
    store.clear();
    expect(store.user()).toBeNull();
    expect(store.loaded()).toBe(true);
  });

  it('aplica parches locales al negocio', () => {
    store.set(me);
    store.patchBusiness('b1', { name: 'Barbería Nueva' });
    expect(store.currentBusiness()?.name).toBe('Barbería Nueva');
    expect(store.displayName()).toBe('Pepe García');
  });
});
