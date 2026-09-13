# Landing pública (midominio.com)

Sitio estático sin build: `index.html` + `styles.css` + `app.js` + `img/`.

- **Todo lo editable** (precios, textos de envío/garantía, WhatsApp, email, enlaces al panel, cifras,
  testimonios) está en el bloque `SITE` al principio de `app.js`.
- SEO: `siteUrl` en `SITE` alimenta el `canonical` y los datos estructurados (Organization, Product,
  FAQPage); cambia también el dominio en `sitemap.xml` y `robots.txt`.
- Los pedidos se envían por WhatsApp o email (sin pasarela de pago en el MVP). Para añadir Stripe u otra
  pasarela, sustituye el `submit` de `#checkout-form` en `app.js`.
- Sustituye las ilustraciones `img/device-*.svg` por fotos reales del producto cuando las tengas
  (mismo nombre de archivo o cambia `img` en `SITE.products`).
- Probar en local: `python3 -m http.server 8090 -d landing` → http://localhost:8090
- En Docker forma parte de `docker-compose.yml` como servicio `landing` (puerto `LANDING_PORT`).
