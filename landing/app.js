/* =====================================================================
   ReviewTap — landing pública. TODO LO EDITABLE ESTÁ EN ESTE BLOQUE.
   Precios, textos comerciales, contacto y testimonios. Sin build: guarda y recarga.
   ===================================================================== */
const SITE = {
  brand: 'ReviewTap',
  appUrl: 'https://app.midominio.com/login',        // panel de clientes
  privacyUrl: 'https://app.midominio.com/privacy',
  termsUrl: 'https://app.midominio.com/terms',
  email: 'hola@midominio.com',
  whatsapp: '34600000000',                            // sólo dígitos, con prefijo de país
  instagram: 'https://instagram.com/',
  facebook: 'https://facebook.com/',
  currency: 'EUR',
  shippingShort: 'Envío gratis 24–48 h',
  shippingText: 'El envío es gratuito y el plazo de entrega es de 24 a 48 horas laborables en España peninsular.',
  guaranteeShort: 'Garantía de 30 días',
  guaranteeTitle: 'Garantía de 30 días',
  guaranteeText: 'Si no consigues más reseñas que antes, te devolvemos el 100 % del importe.',
  legalLine: 'ReviewTap es una marca de [Tu empresa, S.L.] · [Dirección fiscal]',
  legalNotice: 'Titular: [Tu empresa, S.L.] · CIF [B00000000] · [Dirección] · [email]. Devoluciones: dispones de 30 días naturales desde la recepción para devolver el producto en su estado original; los gastos de envío de la devolución corren a cargo del comprador salvo defecto de fabricación.',
  // Cifras reales o null para ocultar la línea. Ejemplo: { rating: 4.9, reviews: 120, businesses: 350 }
  stats: null,
  // Testimonios REALES (con permiso). Si la lista está vacía, la sección no se muestra.
  // { name: 'Nombre A.', business: 'Barbería X, Valencia', rating: 5, text: '…', date: '2026-09' }
  testimonials: [],
  products: [
    { id: 'expositor', name: 'Expositor Reseñas Google', price: 39.9, compareAt: 49.9, img: 'img/device-stand.svg',
      desc: 'Sobremesa, NFC + QR. El más vendido para mostradores y recepciones.', tag: 'Más vendido' },
    { id: 'placa', name: 'Placa Reseñas Google', price: 34.9, compareAt: null, img: 'img/device-plate.svg',
      desc: 'Adhesiva, para pared, mostrador o junto a la caja. NFC + QR.', tag: null },
    { id: 'tarjeta', name: 'Tarjeta NFC Reseñas', price: 24.9, compareAt: null, img: 'img/device-card.svg',
      desc: 'Formato tarjeta para empleados, camareros o entregar con la cuenta.', tag: null },
    { id: 'pack3', name: 'Pack 3 Expositores', price: 99.9, compareAt: 119.7, img: 'img/device-pack.svg',
      desc: 'Tres expositores para varios puntos del local o varios locales.', tag: 'Ahorra 17 %' },
  ],
  featuredId: 'expositor',
};

/* ---------------------------------------------------------------------
   A partir de aquí no hace falta tocar nada.
   --------------------------------------------------------------------- */
const $ = (s, r = document) => r.querySelector(s);
const $$ = (s, r = document) => [...r.querySelectorAll(s)];
const fmt = (n) => new Intl.NumberFormat('es-ES', { style: 'currency', currency: SITE.currency }).format(n);
const product = (id) => SITE.products.find((p) => p.id === id);

/* ---- Textos y enlaces configurables ---- */
$$('[data-site]').forEach((el) => { const v = SITE[el.dataset.site]; if (v) el.textContent = v; });
$$('[data-site-href]').forEach((el) => { const v = SITE[el.dataset.siteHref]; if (v) el.href = v; else el.closest('a') && (el.style.display = 'none'); });
$$('[data-site-mailto]').forEach((el) => { el.href = 'mailto:' + SITE[el.dataset.siteMailto]; });
$('#year').textContent = new Date().getFullYear();
$('#wa-link').href = waLink('Hola, tengo una duda sobre los dispositivos ' + SITE.brand + '.');
$('#mail-link').href = 'mailto:' + SITE.email + '?subject=' + encodeURIComponent('Consulta sobre ' + SITE.brand);

if (SITE.stats) {
  const s = SITE.stats;
  const line = $('#hero-stats');
  line.innerHTML = `<span class="stars">★★★★★</span> ${s.rating.toLocaleString('es-ES')}/5 · ${s.reviews} opiniones · Funcionando en +${s.businesses.toLocaleString('es-ES')} negocios`;
  line.hidden = false;
  const pr = $('#featured-rating');
  pr.innerHTML = `<span class="stars">★★★★★</span> ${s.rating.toLocaleString('es-ES')}/5 · +${s.reviews} reseñas`;
  pr.hidden = false;
}

/* ---- Producto destacado ---- */
const featured = product(SITE.featuredId);
$('#featured-name').textContent = featured.name;
$('#featured-price').innerHTML = featured.compareAt
  ? `<span class="price-old">${fmt(featured.compareAt)}</span> ${fmt(featured.price)}`
  : fmt(featured.price);
$('#gallery-main').src = featured.img;
$$('#gallery-thumbs button').forEach((b) => b.addEventListener('click', () => {
  $$('#gallery-thumbs button').forEach((x) => x.classList.remove('on'));
  b.classList.add('on');
  const img = $('#gallery-main');
  img.style.opacity = 0;
  setTimeout(() => { img.src = b.dataset.src; img.style.opacity = 1; }, 120);
}));
const qty = $('#qty');
$('#qty-minus').addEventListener('click', () => (qty.value = Math.max(1, +qty.value - 1)));
$('#qty-plus').addEventListener('click', () => (qty.value = Math.min(99, +qty.value + 1)));
$('#add-featured').addEventListener('click', () => { cart.add(featured.id, Math.max(1, +qty.value || 1)); openDrawer(); });

/* ---- Tienda ---- */
$('#shop-grid').innerHTML = SITE.products.map((p) => `
  <article class="card-product reveal">
    ${p.tag ? `<span class="tag">${p.tag}</span>` : ''}
    <div class="pic"><img src="${p.img}" alt="${p.name}" width="300" height="300" loading="lazy"></div>
    <h3>${p.name}</h3>
    <p class="desc">${p.desc}</p>
    <div class="card-price"><b>${fmt(p.price)}</b>${p.compareAt ? `<span class="price-old">${fmt(p.compareAt)}</span>` : ''}</div>
    <button class="btn btn-primary" type="button" data-add="${p.id}">Añadir al carrito</button>
  </article>`).join('');
$$('[data-add]').forEach((b) => b.addEventListener('click', () => { cart.add(b.dataset.add, 1); openDrawer(); }));

/* ---- Testimonios (sólo reales) ---- */
if (SITE.testimonials.length) {
  $('#testimonios').hidden = false;
  $('#testi-grid').innerHTML = SITE.testimonials.map((t) => `
    <article class="testi reveal">
      <div class="stars">${'★'.repeat(t.rating)}${'☆'.repeat(5 - t.rating)}</div>
      <p>${escapeHtml(t.text)}</p>
      <footer><span>${escapeHtml(t.name)} · ${escapeHtml(t.business)}</span><span>${t.date || ''}</span></footer>
    </article>`).join('');
}

/* ---- Carrito (localStorage) ---- */
const cart = {
  key: 'rt_cart',
  items: [],
  load() { try { this.items = JSON.parse(localStorage.getItem(this.key) || '[]'); } catch { this.items = []; } this.items = this.items.filter((i) => product(i.id)); },
  save() { try { localStorage.setItem(this.key, JSON.stringify(this.items)); } catch { /* sin almacenamiento: el carrito dura la sesión */ } this.render(); },
  add(id, n) { const it = this.items.find((i) => i.id === id); if (it) it.qty = Math.min(99, it.qty + n); else this.items.push({ id, qty: n }); this.save(); toast(`${product(id).name} añadido`); },
  setQty(id, n) { const it = this.items.find((i) => i.id === id); if (!it) return; it.qty = Math.max(1, Math.min(99, n)); this.save(); },
  remove(id) { this.items = this.items.filter((i) => i.id !== id); this.save(); },
  count() { return this.items.reduce((a, i) => a + i.qty, 0); },
  total() { return this.items.reduce((a, i) => a + i.qty * product(i.id).price, 0); },
  render() {
    const count = this.count();
    const badge = $('#cart-count');
    badge.textContent = count;
    badge.hidden = count === 0;
    const body = $('#drawer-body');
    const foot = $('#drawer-foot');
    if (!this.items.length) {
      body.innerHTML = '<p class="empty-cart">Tu carrito está vacío.<br>Añade un dispositivo para empezar.</p>';
      foot.innerHTML = '<a class="btn btn-ghost btn-block" href="#tienda" id="go-shop">Ver la tienda</a>';
      $('#go-shop').addEventListener('click', closeDrawer);
      return;
    }
    body.innerHTML = this.items.map((i) => { const p = product(i.id); return `
      <div class="cart-item">
        <img src="${p.img}" alt="">
        <div><div class="name">${p.name}</div><div class="sub">${fmt(p.price)} / ud.</div>
          <div class="qty"><button type="button" data-dec="${p.id}" aria-label="Menos">−</button><input type="number" min="1" max="99" value="${i.qty}" data-qty="${p.id}" aria-label="Cantidad"><button type="button" data-inc="${p.id}" aria-label="Más">+</button></div></div>
        <div><div class="line">${fmt(i.qty * p.price)}</div><button class="remove" type="button" data-rm="${p.id}">Quitar</button></div>
      </div>`; }).join('');
    foot.innerHTML = `<div class="total-row"><span>Total <small>IVA incl. · envío gratis</small></span><span>${fmt(this.total())}</span></div>
      <button class="btn btn-primary btn-lg btn-block" type="button" id="to-checkout">Finalizar pedido</button>`;
    $$('[data-dec]', body).forEach((b) => b.addEventListener('click', () => this.setQty(b.dataset.dec, this.items.find((i) => i.id === b.dataset.dec).qty - 1)));
    $$('[data-inc]', body).forEach((b) => b.addEventListener('click', () => this.setQty(b.dataset.inc, this.items.find((i) => i.id === b.dataset.inc).qty + 1)));
    $$('[data-qty]', body).forEach((el) => el.addEventListener('change', () => this.setQty(el.dataset.qty, +el.value || 1)));
    $$('[data-rm]', body).forEach((b) => b.addEventListener('click', () => this.remove(b.dataset.rm)));
    $('#to-checkout').addEventListener('click', openCheckout);
  },
};
cart.load();
cart.render();

/* ---- Drawer ---- */
const drawer = $('#drawer'), backdrop = $('#drawer-backdrop');
function openDrawer() { drawer.classList.add('open'); drawer.setAttribute('aria-hidden', 'false'); backdrop.hidden = false; document.body.style.overflow = 'hidden'; }
function closeDrawer() { drawer.classList.remove('open'); drawer.setAttribute('aria-hidden', 'true'); backdrop.hidden = true; document.body.style.overflow = ''; }
$('#cart-btn').addEventListener('click', openDrawer);
$('#drawer-close').addEventListener('click', closeDrawer);
backdrop.addEventListener('click', closeDrawer);

/* ---- Checkout: genera el pedido por WhatsApp o email (sin pasarela en el MVP) ---- */
const checkout = $('#checkout');
function openCheckout() {
  closeDrawer();
  $('#checkout-summary').innerHTML = cart.items.map((i) => `<div class="total-row" style="font-weight:500;font-size:.9rem"><span>${i.qty} × ${product(i.id).name}</span><span>${fmt(i.qty * product(i.id).price)}</span></div>`).join('')
    + `<div class="total-row"><span>Total</span><span>${fmt(cart.total())}</span></div>`;
  checkout.hidden = false;
  document.body.style.overflow = 'hidden';
  $('#checkout-form input[name=name]').focus();
}
function closeCheckout() { checkout.hidden = true; document.body.style.overflow = ''; }
$('#checkout-close').addEventListener('click', closeCheckout);
checkout.addEventListener('click', (e) => { if (e.target === checkout) closeCheckout(); });
document.addEventListener('keydown', (e) => { if (e.key === 'Escape') { closeCheckout(); closeDrawer(); } });

let via = 'whatsapp';
$$('#checkout-form [data-via]').forEach((b) => b.addEventListener('click', () => (via = b.dataset.via)));
$('#checkout-form').addEventListener('submit', (e) => {
  e.preventDefault();
  if (!cart.items.length) return;
  const f = Object.fromEntries(new FormData(e.target).entries());
  const lines = cart.items.map((i) => `• ${i.qty} × ${product(i.id).name} — ${fmt(i.qty * product(i.id).price)}`).join('\n');
  const msg = `Nuevo pedido ${SITE.brand}\n\n${lines}\nTotal: ${fmt(cart.total())}\n\n`
    + `Nombre: ${f.name}\nTeléfono: ${f.phone}\nEmail: ${f.email}\n`
    + `Negocio: ${f.business}\nFicha de Google: ${f.google}\nEnvío: ${f.address}\n`
    + (f.notes ? `Notas: ${f.notes}\n` : '');
  if (via === 'whatsapp' && SITE.whatsapp) {
    window.open(waLink(msg), '_blank', 'noopener');
  } else {
    location.href = 'mailto:' + SITE.email + '?subject=' + encodeURIComponent('Pedido ' + SITE.brand + ' — ' + f.business) + '&body=' + encodeURIComponent(msg);
  }
  toast('Pedido preparado. Te confirmamos en menos de 24 h.');
  closeCheckout();
});

/* ---- Formulario de contacto (mailto) ---- */
$('#contact-form').addEventListener('submit', (e) => {
  e.preventDefault();
  const f = Object.fromEntries(new FormData(e.target).entries());
  const body = `${f.message}\n\n— ${f.name}${f.business ? ' (' + f.business + ')' : ''}\nContacto: ${f.reply}`;
  location.href = 'mailto:' + SITE.email + '?subject=' + encodeURIComponent('Consulta desde la web') + '&body=' + encodeURIComponent(body);
});

/* ---- Navegación móvil + reveal ---- */
const burger = $('#burger'), nav = $('#nav');
burger.addEventListener('click', () => { const open = nav.classList.toggle('open'); burger.setAttribute('aria-expanded', String(open)); });
$$('#nav a').forEach((a) => a.addEventListener('click', () => nav.classList.remove('open')));
const io = new IntersectionObserver((entries) => entries.forEach((en) => { if (en.isIntersecting) { en.target.classList.add('in'); io.unobserve(en.target); } }), { threshold: 0.12 });
$$('.reveal').forEach((el) => io.observe(el));

/* ---- Utilidades ---- */
function waLink(text) { return `https://wa.me/${SITE.whatsapp}?text=${encodeURIComponent(text)}`; }
function escapeHtml(s) { return String(s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c])); }
let toastTimer;
function toast(text) { const t = $('#toast'); t.textContent = text; t.hidden = false; clearTimeout(toastTimer); toastTimer = setTimeout(() => (t.hidden = true), 2800); }
