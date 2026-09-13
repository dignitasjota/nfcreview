import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-public-layout',
  imports: [RouterOutlet, RouterLink],
  template: `
    <div class="pub">
      <header class="pub-head">
        <a routerLink="/" class="brand"><span class="logo">✓</span> ReviewTap</a>
        <a routerLink="/login" class="btn btn-sm">Iniciar sesión</a>
      </header>
      <main class="pub-main card"><router-outlet /></main>
      <footer class="small muted">© {{ year }} ReviewTap · <a routerLink="/privacy">Privacidad</a> · <a routerLink="/terms">Términos</a></footer>
    </div>
  `,
  styles: `
    .pub { max-width: 820px; margin: 0 auto; padding: 1.5rem 1rem 3rem; display: grid; gap: 1.25rem; }
    .pub-head { display: flex; justify-content: space-between; align-items: center; }
    .brand { display: flex; align-items: center; gap: 0.5rem; font-weight: 700; color: var(--ink); font-size: 1.05rem; }
    .logo { width: 28px; height: 28px; border-radius: 8px; background: var(--brand); color: #fff; display: grid; place-items: center; }
    .pub-main { padding: 2rem; line-height: 1.65; }
    .pub-main :is(h2) { margin-top: 1.5rem; margin-bottom: 0.4rem; }
    .pub-main p, .pub-main ul { margin: 0.4rem 0; }
    footer { text-align: center; }
  `,
})
export class PublicLayoutComponent {
  readonly year = new Date().getFullYear();
}
