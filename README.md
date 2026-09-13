# ReviewTap

SaaS B2B para gestionar tarjetas, placas y pegatinas **NFC/QR** que facilitan que los clientes de un
comercio dejen una reseña en Google. Cada dispositivo tiene una URL propia
(`https://r.midominio.com/d/F8k3Lm2Pq7?src=nfc`); al acercar el móvil o escanear el QR, el backend
identifica el dispositivo, registra una **interacción anónima** y redirige de inmediato a la página oficial
de Google para escribir una reseña. El comercio ve sus estadísticas en un dashboard privado.

> Una interacción es una apertura del enlace. **Nunca** se presenta como una reseña conseguida: ReviewTap no
> publica, lee, incentiva ni filtra reseñas.

## Arquitectura

```
Angular 22 (SPA) ──/api──▶ Spring Boot 4.1 (Java 21) ──▶ PostgreSQL 16
                                   ▲
   NFC / QR ──▶ GET /d/{code}?src=nfc|qr ──▶ 302 → Google Reviews
```

- `backend/` — API REST (Spring Web, Security, Data JPA, Validation, Flyway, OpenAPI, ZXing).
- `frontend/` — Angular standalone + signals, formularios reactivos, Chart.js.
- `landing/` — web pública de venta (HTML/CSS/JS estático, sin build; precios y textos en `landing/app.js`).
- `docker-compose.yml` — `postgres` + `backend` + `frontend` (nginx que sirve la SPA y proxifica `/api`) + `landing`.
- `docs/ARCHITECTURE.md` — entidades, seguridad, redirect, analítica, deduplicación, privacidad.
- `docs/DEPLOYMENT.md` — despliegue con Nginx Proxy Manager / nginx.

## Requisitos

| Uso | Necesitas |
|---|---|
| Producción | Docker 24+ y Docker Compose v2 |
| Desarrollo backend | JDK 21, Maven 3.9, Docker (para PostgreSQL y Testcontainers) |
| Desarrollo frontend | Node 22.22+ (o 24.15+), npm |

## Arranque rápido con Docker

```bash
cp .env.example .env
# Edita .env: POSTGRES_PASSWORD, JWT_SECRET (openssl rand -base64 48), PUBLIC_BASE_URL,
#             APP_BOOTSTRAP_ADMIN_EMAIL / APP_BOOTSTRAP_ADMIN_PASSWORD
docker compose up -d --build
```

- Web pública: http://localhost:8082 (`LANDING_PORT`).
- Panel: http://localhost:8081 (o el `FRONTEND_PORT` configurado).
- API/redirect: http://localhost:8080 — `GET /actuator/health`, `GET /d/{code}`.
- Swagger UI (si `SWAGGER_UI_ENABLED=true`): http://localhost:8080/api/docs/ui

Para probar en local con `http://` pon `COOKIE_SECURE=false` y `PUBLIC_BASE_URL=http://localhost:8080`.
Con `SPRING_PROFILES_ACTIVE=dev` se cargan los datos demo (ver abajo).

## Desarrollo local

```bash
# PostgreSQL
docker run -d --name reviewtap-pg -e POSTGRES_DB=reviewtap -e POSTGRES_USER=reviewtap \
  -e POSTGRES_PASSWORD=reviewtap -p 5432:5432 postgres:16-alpine

# Backend (perfil dev: seed demo, cookie sin Secure, JWT_SECRET de desarrollo)
cd backend && SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run

# Frontend (proxy /api → http://localhost:8080, ver proxy.conf.json)
cd frontend && npm install --legacy-peer-deps && npm start   # http://localhost:4200
```

Variables útiles del backend (todas con valor por defecto para dev): `DATABASE_URL`, `DATABASE_USER`,
`DATABASE_PASSWORD`, `JWT_SECRET`, `PUBLIC_BASE_URL`, `CORS_ALLOWED_ORIGINS`, `COOKIE_SECURE`,
`INTERACTION_DEDUPE_WINDOW` (`PT60S`), `INTERACTION_CLIENT_RATE_LIMIT` (`40`), `DEV_SEED_PASSWORD`.

### Datos demo (perfil `dev`)

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin@example.local` | `Demo1234!` (o `DEV_SEED_PASSWORD`) | ADMIN |
| `pepe@example.local` | `Demo1234!` | Propietario de **Barbería Demo** (3 dispositivos, ~600 interacciones en 60 días) |
| `ana@example.local` | `Demo1234!` | Propietaria de **Cafetería Aroma** (sin interacciones; útil para probar el aislamiento) |

El seed es idempotente (no se repite si ya existe `barberia-demo`) y **no se ejecuta** en `prod`.

## Migraciones

Flyway aplica `backend/src/main/resources/db/migration/V*__*.sql` al arrancar; Hibernate sólo valida
(`ddl-auto=validate`). Para un cambio de esquema, añade `V2__descripcion.sql` — nunca edites una migración
ya aplicada.

## Tests

```bash
cd backend && mvn test          # 58 tests: unitarios + integración con PostgreSQL (Testcontainers, requiere Docker)
cd frontend && npm test         # Vitest: utilidades de periodo, interceptor de errores, store de sesión
```

Cubren: redirect válido/inexistente/inactivo/sin URL, deduplicación y techo por cliente, login/lockout/
cookie/logout, autorización entre negocios (403), métricas y comparación de periodos, validación de URL,
generación y decodificación de QR.

## Crear el primer ADMIN

En producción, define `APP_BOOTSTRAP_ADMIN_EMAIL` y `APP_BOOTSTRAP_ADMIN_PASSWORD` en `.env` antes del
primer arranque. El backend crea el administrador sólo si no existe ninguno; después puedes borrar las
variables. Desde el panel (*Usuarios*) se crean más administradores o usuarios de negocio.

## Despliegue

Ver [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md): topología `app.midominio.com` (frontend) +
`r.midominio.com` (redirect), configuración de Nginx Proxy Manager y nginx, cabeceras `X-Forwarded-*`,
comprobaciones y backups.

## Cómo obtener la Google Review URL

1. Entra en [Google Business Profile](https://business.google.com) con la cuenta del negocio.
2. En el perfil, pulsa **Pedir reseñas** (*Ask for reviews* / *Get more reviews*): Google muestra un
   enlace corto del tipo `https://g.page/r/XXXXXXXX/review`.
3. Alternativa: `https://search.google.com/local/writereview?placeid=<PLACE_ID>` usando el *Place ID*
   del negocio.
4. Pégala en **Admin → Negocios → Google Review URL**. Debe empezar por `https://`; se valida al guardar.

## Cómo programar una tarjeta NFC

1. Abre **Dispositivos → (dispositivo)**; en *Programación NFC* pulsa **Copiar URL**
   (`https://r.midominio.com/d/XXXXXXXX?src=nfc`).
2. Con una app de escritura NFC (NFC Tools en Android/iOS, por ejemplo) añade un registro **URL/URI**,
   pega la URL y pulsa *Escribir* acercando la tarjeta (chips NTAG213/215/216 funcionan bien).
3. Opcional: bloquea el chip contra escritura tras verificar que redirige.
4. Prueba acercando un móvil: debe abrir directamente la pantalla de reseña de Google.

## Cómo generar y descargar el QR

En la ficha del dispositivo (o al final del asistente de alta), *Programación QR* muestra el código con la
URL `?src=qr`. **Descargar PNG** (1024 px) sirve para pegatinas; **Descargar SVG** es vectorial para
imprenta. El QR también está disponible por API: `GET /api/devices/{id}/qr.png?size=512` y
`GET /api/devices/{id}/qr.svg` (requieren sesión con acceso al negocio).

## Endpoints principales

| Método | Ruta | Quién |
|---|---|---|
| GET | `/d/{code}?src=nfc\|qr` | Público (redirect) |
| POST / POST / GET / POST | `/api/auth/login`, `/logout`, `/me`, `/change-password` | Cualquiera / sesión |
| GET / POST | `/api/businesses` | Sesión (lista propia) / ADMIN (crear, con propietario opcional) |
| GET / PUT / PATCH | `/api/businesses/{id}`, `…/profile`, `…/status` | Miembro / ADMIN / OWNER / ADMIN |
| GET / POST / DELETE | `/api/businesses/{id}/members[/{userId}]` | Miembro / ADMIN |
| GET / POST | `/api/businesses/{id}/devices` | Miembro / ADMIN |
| GET / PUT / PATCH | `/api/devices/{id}`, `…/status` | Miembro / ADMIN |
| GET | `/api/devices/{id}/qr.png`, `qr.svg` | Miembro |
| GET | `/api/businesses/{id}/analytics/summary\|timeline\|devices\|recent\|export.csv` | Miembro |
| GET / POST / PATCH / POST | `/api/admin/users`, `…/{id}/status`, `…/{id}/reset-password` | ADMIN |
| GET | `/api/admin/devices`, `/api/admin/analytics/summary` | ADMIN |

Parámetros de analítica: `period=today|7d|30d|this_month|last_month|custom` y `from`/`to` (ISO) para
`custom`. Documentación OpenAPI en `/api/docs` (JSON) y `/api/docs/ui`.

Errores: siempre `{ "code": "VALIDATION_ERROR", "message": "…", "fields": { "googleReviewUrl": "…" } }`.

## Decisiones técnicas

Resumen (detalle en `docs/ARCHITECTURE.md`):

- **Sesión en cookie HttpOnly + SameSite=Strict** con JWT HS256; sin refresh tokens; el frontend nunca toca
  el token. La API se sirve en el mismo origen que la SPA (nginx), sin CORS.
- **Un código por dispositivo físico y `?src=nfc|qr`** para distinguir el canal; valores no reconocidos →
  `UNKNOWN`.
- **Deduplicación anónima**: HMAC(salt diario, IP + UA) en memoria, ventana de 60 s por dispositivo y techo
  de 40 registros / 10 min por cliente. La IP no se persiste. Se redirige siempre.
- **Registro asíncrono** con pool acotado y `CallerRunsPolicy` (nunca se descarta silenciosamente).
- **Agregaciones en SQL** con índices `(device_id, created_at)`; CSV en streaming.
- **Sin ORM-mapper ni librería de UI**: records + mapeos manuales; CSS propio con variables.
- **Spring Boot 4.1 / Angular 22** (versiones estables actuales en septiembre de 2026).

## Fuera del MVP (a propósito)

Pagos y planes, Google Business Profile API, lectura/respuesta de reseñas, IA, WhatsApp, emails,
multiidioma, white label, app móvil/PWA, marketing site, gamificación, multi-establecimiento por negocio.
La arquitectura los admite sin rehacer nada (ver el último apartado de `docs/ARCHITECTURE.md`).

## Privacidad

No se almacenan datos personales de quien usa un dispositivo (ni IP, ni fingerprint, ni ubicación). La
única cookie es la técnica de sesión del panel. Páginas `/privacy` y `/terms` incluidas en la app.
