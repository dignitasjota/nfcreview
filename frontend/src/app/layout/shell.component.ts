import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/api/auth.service';
import { SessionStore } from '../core/session.store';
import { ToastService } from '../core/toast.service';

interface NavItem {
  label: string;
  path: string;
  icon: string;
  exact?: boolean;
}

const BUSINESS_NAV: NavItem[] = [
  { label: 'Dashboard', path: '/app/dashboard', icon: '▤' },
  { label: 'Dispositivos', path: '/app/devices', icon: '◫' },
  { label: 'Mi negocio', path: '/app/business', icon: '⌂' },
  { label: 'Cuenta', path: '/app/account', icon: '◯' },
];

const ADMIN_NAV: NavItem[] = [
  { label: 'Dashboard', path: '/admin/dashboard', icon: '▤' },
  { label: 'Negocios', path: '/admin/businesses', icon: '⌂' },
  { label: 'Usuarios', path: '/admin/users', icon: '◯' },
  { label: 'Dispositivos', path: '/admin/devices', icon: '◫' },
  { label: 'Cuenta', path: '/admin/account', icon: '⚙' },
];

/** Layout autenticado: sidebar + barra superior + contenido. */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="shell" [class.nav-open]="navOpen()">
      <aside class="sidebar">
        <a class="brand" routerLink="/">
          <span class="logo">✓</span>
          <span>ReviewTap</span>
          @if (session.isAdmin()) { <span class="badge badge-muted" style="margin-left:auto">Admin</span> }
        </a>
        <nav>
          @for (item of nav(); track item.path) {
            <a [routerLink]="item.path" routerLinkActive="active" (click)="navOpen.set(false)">
              <span class="icon" aria-hidden="true">{{ item.icon }}</span>{{ item.label }}
            </a>
          }
        </nav>
        <div class="sidebar-foot small">
          <a routerLink="/privacy">Privacidad</a> · <a routerLink="/terms">Términos</a>
        </div>
      </aside>
      <div class="backdrop" (click)="navOpen.set(false)"></div>

      <div class="main">
        <header class="topbar">
          <button class="btn btn-ghost menu" type="button" aria-label="Menú" (click)="navOpen.set(!navOpen())">☰</button>
          <div class="context truncate">
            @if (!session.isAdmin()) {
              @if (session.businesses().length > 1) {
                <select class="select biz-select" [value]="session.currentBusiness()?.id" (change)="switchBusiness($event)" aria-label="Negocio">
                  @for (b of session.businesses(); track b.id) { <option [value]="b.id">{{ b.name }}</option> }
                </select>
              } @else {
                <strong class="truncate">{{ session.currentBusiness()?.name ?? 'Sin negocio asignado' }}</strong>
              }
            } @else {
              <strong>Panel de administración</strong>
            }
          </div>
          <div class="user">
            <span class="truncate small muted">{{ session.displayName() }}</span>
            <button class="btn btn-sm" type="button" (click)="logout()">Salir</button>
          </div>
        </header>
        <main class="content">
          <router-outlet />
        </main>
      </div>
    </div>
  `,
  styles: `
    .shell { min-height: 100vh; display: grid; grid-template-columns: var(--sidebar-w) 1fr; }
    .sidebar { background: #0b1220; color: #cbd5e1; display: flex; flex-direction: column; padding: 1rem 0.75rem; position: sticky; top: 0; height: 100vh; }
    .brand { display: flex; align-items: center; gap: 0.6rem; color: #fff; font-weight: 700; font-size: 1.05rem; padding: 0.4rem 0.6rem 1.2rem; text-decoration: none; }
    .logo { width: 28px; height: 28px; border-radius: 8px; background: var(--brand-600); display: grid; place-items: center; font-size: 0.95rem; }
    nav { display: grid; gap: 0.15rem; }
    nav a { display: flex; align-items: center; gap: 0.7rem; padding: 0.6rem 0.7rem; border-radius: 8px; color: #cbd5e1; text-decoration: none; font-weight: 500; }
    nav a:hover { background: rgb(255 255 255 / 0.06); color: #fff; }
    nav a.active { background: rgb(13 148 136 / 0.22); color: #fff; }
    .icon { width: 1.2rem; text-align: center; opacity: 0.8; }
    .sidebar-foot { margin-top: auto; padding: 0.75rem 0.6rem 0; color: #64748b; }
    .sidebar-foot a { color: #94a3b8; }
    .main { min-width: 0; display: flex; flex-direction: column; }
    .topbar { height: 60px; display: flex; align-items: center; gap: 1rem; padding: 0 1.5rem; background: #fff; border-bottom: 1px solid var(--line); position: sticky; top: 0; z-index: 10; }
    .context { flex: 1; min-width: 0; }
    .biz-select { height: 34px; max-width: 280px; font-weight: 600; }
    .user { display: flex; align-items: center; gap: 0.75rem; min-width: 0; }
    .user .truncate { max-width: 180px; }
    .content { padding: 1.5rem; max-width: 1240px; width: 100%; }
    .menu { display: none; }
    .backdrop { display: none; }
    @media (max-width: 900px) {
      .shell { grid-template-columns: 1fr; }
      .sidebar { position: fixed; left: 0; top: 0; bottom: 0; width: var(--sidebar-w); transform: translateX(-100%); transition: transform 0.2s; z-index: 30; }
      .nav-open .sidebar { transform: none; }
      .nav-open .backdrop { display: block; position: fixed; inset: 0; background: rgb(15 23 42 / 0.4); z-index: 20; }
      .menu { display: inline-flex; }
      .topbar { padding: 0 1rem; }
      .content { padding: 1rem; }
      .user .truncate { display: none; }
    }
  `,
})
export class ShellComponent {
  protected readonly session = inject(SessionStore);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);
  protected readonly navOpen = signal(false);
  protected readonly nav = computed(() => (this.session.isAdmin() ? ADMIN_NAV : BUSINESS_NAV));

  switchBusiness(e: Event) {
    this.session.selectBusiness((e.target as HTMLSelectElement).value);
  }

  async logout() {
    try {
      await this.auth.logout();
    } catch {
      /* la sesión local ya se ha limpiado */
    }
    this.toast.info('Sesión cerrada');
    void this.router.navigate(['/login']);
  }
}
