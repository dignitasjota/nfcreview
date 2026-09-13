# Despliegue de ReviewTap

## Topología recomendada

| Dominio | Destino | Contenido |
|---|---|---|
| `app.midominio.com` | contenedor `frontend` (nginx, puerto 80) | SPA Angular; nginx proxifica `/api/*` al backend en la red interna |
| `r.midominio.com` | contenedor `backend` (puerto 8080) | Redirecciones públicas `/d/{code}` |

No hace falta `api.midominio.com`: el frontend y la API comparten origen a través del proxy de nginx, lo
que evita CORS y cookies cross-site. Si prefieres separarlos, apunta `api.midominio.com` al backend y define
`CORS_ALLOWED_ORIGINS=https://app.midominio.com` (mismo *site*, la cookie `SameSite=Strict` sigue funcionando).

`r.midominio.com` también responde a `/api/**` (es el mismo proceso). Si quieres que sólo exponga
redirecciones, limita en el proxy la ruta `/d/` para ese host.

## Requisitos

- Docker Engine 24+ y Docker Compose v2.
- Un reverse proxy con TLS: Nginx Proxy Manager (NPM), Caddy, Traefik o nginx.
- DNS: registros A/AAAA para `app.` y `r.` apuntando al VPS.

## Pasos

```bash
git clone <repo> reviewtap && cd reviewtap
cp .env.example .env
openssl rand -base64 48        # → JWT_SECRET
openssl rand -base64 24        # → POSTGRES_PASSWORD
$EDITOR .env                   # PUBLIC_BASE_URL=https://r.midominio.com, APP_BOOTSTRAP_ADMIN_*
docker compose up -d --build
docker compose logs -f backend # espera "Started ReviewTapApplication"
```

El backend aplica las migraciones Flyway al arrancar y crea el primer ADMIN si no existe ninguno
(`APP_BOOTSTRAP_ADMIN_EMAIL` / `APP_BOOTSTRAP_ADMIN_PASSWORD`). Una vez creado puedes vaciar esas
variables. Cambia la contraseña desde *Cuenta* tras el primer acceso.

Puertos publicados sólo en `127.0.0.1` (`BACKEND_PORT`, `FRONTEND_PORT`): el proxy es quien expone TLS.

## Nginx Proxy Manager

Si NPM corre en Docker en el mismo host, conéctalo a la red `reviewtap-proxy` (variable
`PROXY_NETWORK`) y usa los nombres de contenedor como destino:

| Proxy host | Scheme | Forward host | Puerto | Opciones |
|---|---|---|---|---|
| `app.midominio.com` | http | `reviewtap-frontend-1` (o `127.0.0.1` si NPM está fuera de Docker) | 80 (u `8081`) | Websockets no necesarios; *Block common exploits* OK |
| `r.midominio.com` | http | `reviewtap-backend-1` (o `127.0.0.1`) | 8080 | — |

Activa **Force SSL** y **HTTP/2** en ambos y solicita certificados Let's Encrypt.

NPM envía `X-Forwarded-For`, `X-Forwarded-Proto` y `X-Real-IP` por defecto. **Importante**: el proxy debe
reenviar la cabecera `Host` original (NPM lo hace; en nginx, `proxy_set_header Host $http_host`). Spring
compara `Origin` con `Host`/`X-Forwarded-*` para distinguir peticiones same-origin de cross-origin: si el
host llega alterado, el login devuelve 403 desde el navegador aunque funcione con `curl`. El backend tiene
`server.forward-headers-strategy=framework`, con lo que Spring reconoce HTTPS (cookie `Secure`) y el host
público; `ClientKeyResolver` usa `X-Forwarded-For` para la deduplicación anónima.

## nginx clásico

```nginx
server {
    listen 443 ssl http2;
    server_name app.midominio.com;
    # ssl_certificate …
    location / {
        proxy_pass http://127.0.0.1:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }
}

server {
    listen 443 ssl http2;
    server_name r.midominio.com;
    # ssl_certificate …
    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }
}
```

## Comprobación tras el despliegue

```bash
curl -I https://r.midominio.com/d/NoExiste99          # 404 con página HTML
curl -I https://app.midominio.com/                    # 200 (SPA)
curl -s https://app.midominio.com/api/auth/me         # {"code":"UNAUTHORIZED",…}
docker compose ps                                     # los tres servicios "healthy"
```

Crea un negocio y un dispositivo desde `/admin/businesses/new`, abre su URL NFC en el móvil y comprueba
que redirige a Google y que la interacción aparece en el dashboard.

## Operación

- **Logs**: `docker compose logs -f backend`. Se registran errores, logins fallidos (email enmascarado),
  operaciones administrativas y redirecciones rechazadas. Las redirecciones correctas no se loguean.
- **Backup**: `docker compose exec postgres pg_dump -U reviewtap reviewtap | gzip > backup-$(date +%F).sql.gz`.
- **Actualizar**: `git pull && docker compose up -d --build`. Las migraciones nuevas se aplican solas.
- **Salud**: `GET /actuator/health` (backend) y `GET /healthz` (frontend) para tu monitorización.
- **Escalado**: una instancia del backend. Antes de replicar, mover deduplicación y bloqueo de login a Redis.

## Variables de entorno

Ver `.env.example`. Las críticas: `POSTGRES_PASSWORD`, `JWT_SECRET` (≥32 bytes), `PUBLIC_BASE_URL`
(sin barra final; no cambiarla una vez impresas tarjetas), `COOKIE_SECURE=true`, `APP_BOOTSTRAP_ADMIN_*`.
