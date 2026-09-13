# Arquitectura de ReviewTap

Monolito modular: **Angular (SPA) → REST API Spring Boot → PostgreSQL**, más un endpoint público de
redirección (`GET /d/{code}`) que es la pieza crítica del producto.

```
 NFC / QR ──▶ https://r.midominio.com/d/F8k3Lm2Pq7?src=nfc
                     │  RedirectController (backend)
                     │  1 consulta: device ⋈ business por public_code (índice único)
                     │  registra Interaction (asíncrono, deduplicado)
                     ▼
              302 → business.google_review_url   (Cache-Control: no-store)

 app.midominio.com (nginx) ── sirve la SPA y proxifica /api/* ──▶ backend:8080
```

## Entidades y relaciones

| Entidad | Claves | Notas |
|---|---|---|
| `app_user` | `id` UUID, `email` (único, case-insensitive) | `role` = `ADMIN` \| `BUSINESS_USER`, `enabled`, `password_hash` BCrypt(12) |
| `business` | `id`, `slug` único | `google_review_url` (nullable hasta configurarla), `timezone` (IANA, por defecto `Europe/Madrid`), `active` |
| `business_user` | `(user_id, business_id)` único | Rol dentro del negocio: `OWNER` \| `MANAGER`. Un usuario puede pertenecer a varios negocios y un negocio tener varios usuarios |
| `device` | `id`, `public_code` único | `business_id`, `name`, `location_description`, `type` (`NFC` \| `QR` \| `NFC_QR`), `active` |
| `interaction` | `id` | `device_id`, `created_at` (UTC), `interaction_type` (`NFC` \| `QR` \| `UNKNOWN`), `user_agent_category`, `referer` (sólo origen) |

Índices (`V1__init.sql`): `device(public_code)` único, `device(business_id)`,
`interaction(device_id, created_at)`, `interaction(created_at)`, `business_user(business_id)`,
`app_user(lower(email))`. Todas las fechas son `timestamptz` y se escriben en UTC; la conversión a la
zona del negocio ocurre en las consultas de agregación (`at time zone`) y en el frontend (`Intl`).

`Interaction.device_id` es una columna plana (sin `@ManyToOne`) para que el insert no cargue nada.

## Flujo de redirección

`GET /d/{code}?src=nfc|qr` (`RedirectController`):

1. Comprueba el formato del código (`[A-Za-z0-9]{6,32}`); si no cumple, 404 sin tocar la BD.
2. Una única consulta JPQL con proyección (`DeviceRedirectTarget`): id del dispositivo, `device.active`,
   `business.active` y `google_review_url`.
3. Dispositivo inexistente → 404; dispositivo o negocio inactivo → 410; sin URL → 404. En los tres casos
   se devuelve la misma página HTML mínima (`RedirectErrorPage`), sin revelar el motivo.
4. Si procede, `InteractionGuard.shouldRecord()` decide si contar; el registro va a un pool acotado
   (`@Async("interactionExecutor")`, 2–4 hilos, cola 2000, `CallerRunsPolicy`: si se satura, el hilo de la
   petición inserta él mismo en lugar de descartar).
5. `302 Found` con `Location` = URL de la BD, `Cache-Control: no-store` y `Referrer-Policy: no-referrer`.

La URL de destino **nunca** procede de la petición: se valida al guardar el negocio (`@HttpsUrl`: absoluta,
esquema `https`, host presente, sin credenciales, sin espacios) y se lee de la BD en cada redirect.

Con `app.interactions.async=false` (perfil de tests) el registro es síncrono.

### NFC vs QR: una URL por canal, un solo código

Se ha optado por **un único `public_code` por dispositivo físico** y distinguir el canal con el parámetro
`src` (`?src=nfc` / `?src=qr`), en lugar de dos identificadores independientes:

- Una placa física = un registro = una fila en las tablas del dashboard. Con dos códigos habría que
  modelar "grupo de dispositivos" para que el comercio viera su placa como una sola cosa.
- Activar/desactivar, renombrar o reubicar la placa es una única operación.
- El QR y el chip NFC se generan a partir del mismo código; no hay riesgo de emparejar mal dos códigos.

El parámetro **sólo se acepta si es exactamente `nfc` o `qr`** (`InteractionType.fromSource`, sin distinguir
mayúsculas). Cualquier otro valor o su ausencia se registra como `UNKNOWN` y se muestra como "Otros". Esto
significa que alguien podría inflar artificialmente NFC frente a QR cambiando el parámetro, pero no puede
inyectar valores arbitrarios ni afectar al total más allá de la deduplicación descrita abajo.

## Deduplicación y protección básica anti-abuso

`InteractionGuard` (memoria de proceso, Caffeine) aplica tres reglas antes de registrar; en todos los
casos el visitante **es redirigido igualmente**, sólo se omite el registro:

1. **Deduplicación**: clave `deviceId + HMAC(ip + user-agent)` con expiración de 60 s
   (`app.interactions.dedupe-window`). Tres toques seguidos = 1 interacción. Dos móviles distintos
   detrás de la misma IP (NAT del local) cuentan por separado.
2. **Techo por IP**: como máximo 40 registros (`client-rate-limit`) por `HMAC(ip)` en 10 minutos
   sumando todos los dispositivos. No incluye el User-Agent porque el cliente puede rotarlo.
3. **Techo por dispositivo**: como máximo 60 registros por minuto (`device-rate-limit`) por
   dispositivo, sea quien sea el cliente. Es la red de seguridad final: aunque alguien consiguiera
   simular clientes distintos, la cifra queda acotada (un mostrador real no se acerca a ese ritmo).

Además, **no se registran** (pero sí se redirigen) las peticiones `HEAD` ni los User-Agents de bots y
previsualizadores de enlaces (WhatsApp, Telegram, Facebook, Slack, Discord, Googlebot, monitores…),
que no son personas delante del dispositivo.

### De dónde sale la IP

`server.forward-headers-strategy=native`: el `RemoteIpValve` de Tomcat recorre `X-Forwarded-For`
desde la derecha saltando proxies internos (rangos privados + `TRUSTED_PROXIES`) y deja en
`getRemoteAddr()` el primer salto no confiable. Es la única fuente que usa la aplicación; **nunca se
lee la cabecera directamente**: el cliente puede fabricar sus primeros valores (y el valve reescribe
la cabecera dejando justamente esos). Si la cadena sólo contiene direcciones privadas —algo que en
producción sólo ocurre con una cabecera fabricada— todas se agrupan en un único cubo `private`.
Se descartó la estrategia `framework` (`ForwardedHeaderFilter`) porque toma el **primer** valor.

Las claves son `HMAC-SHA256(salt, …)` truncados. El salt se genera con `SecureRandom` al arrancar y
se rota cada día UTC: la clave no es reversible, no es estable entre días ni reinicios y **nunca se
persiste**. La IP sólo existe en memoria durante el cálculo. Al ser estado en memoria, la protección
es por instancia (suficiente para single-node; con réplicas habría que moverla a Redis).

El bloqueo de login (`LoginAttemptService`) usa el mismo enfoque con dos claves: `email + IP`
(8 fallos → 15 min; conocer el email de un cliente no permite dejarle sin acceso desde otra red) y
`IP` (5×8 fallos → frena la enumeración de cuentas).

## Seguridad

- **Sesión**: JWT HS256 (Nimbus, incluido en Spring Security) con `sub`, `email`, `role`, 8 h de vida, en
  una cookie `rt_session` **HttpOnly, SameSite=Strict, Secure** (configurable para `http://localhost`).
  Angular nunca ve el token. No hay refresh tokens en el MVP.
  - **Revocación**: `app_user.token_version` viaja como claim `ver`. El filtro recarga el usuario en
    cada petición y rechaza el token si la versión no coincide. Cerrar sesión, cambiar la
    contraseña, que un ADMIN la restablezca o deshabilitar al usuario incrementan la versión:
    todas las cookies anteriores (en cualquier dispositivo) dejan de valer al instante.
  - Elección frente a `Authorization: Bearer` + localStorage: elimina el robo de token por XSS y el
    manejo manual en el cliente. La API se sirve en el mismo origen que la SPA (nginx proxifica `/api`),
    por lo que no hay CORS ni cookies cross-site; si se separan dominios (`app.` / `api.`), ambos son
    *same-site* y basta con `CORS_ALLOWED_ORIGINS`.
  - CSRF: la cookie es `SameSite=Strict`, la API sólo acepta JSON y el cliente añade
    `X-Requested-With`; los formularios cross-site no pueden reproducir eso. Por eso se desactiva el
    token CSRF de Spring.
- **Contraseña olvidada** (`PasswordResetService`): token de 32 bytes aleatorios en el enlace; en BD
  sólo su SHA-256 (`password_reset_token`), 1 h de vida, un solo uso, purga diaria. La solicitud
  responde siempre 204 y está limitada a 3/h por email y 15/h por IP. Sin SMTP configurado no se envía
  nada y se deja traza en el log. El envío de correo (`MailService`) nunca lanza excepciones.
- **Filtro** `JwtCookieAuthFilter`: valida la firma y **recarga el usuario de BD** en cada petición
  autenticada, de modo que deshabilitar un usuario o cambiarle el rol surte efecto inmediato.
- **Autorización de rutas** (`SecurityConfig`): `/d/**`, `/actuator/health`, `/api/docs/**` y
  `POST /api/auth/login` públicos; `/api/admin/**` exige `ROLE_ADMIN`; el resto de `/api/**` requiere
  sesión; cualquier otra ruta se deniega.
- **Autorización de recurso (multi-tenant)**: cada endpoint que recibe un `businessId`/`deviceId` lleva
  `@PreAuthorize("@access.canRead(#id)")` (`BusinessAccess`): ADMIN pasa siempre; un `BUSINESS_USER`
  sólo si existe la fila `business_user`. `canManage` exige además rol `OWNER`. Los dispositivos se
  resuelven a su negocio antes de comprobar. Fallo → 403 `FORBIDDEN` (cubierto por `TenantAuthorizationIT`).
- **Mutaciones**: crear negocios/dispositivos/usuarios, cambiar la Google Review URL, activar/desactivar y
  gestionar miembros son exclusivas de ADMIN. El `OWNER` sólo edita nombre/dirección/teléfono/zona horaria.
- Contraseñas BCrypt (coste 12); el login compara siempre contra un hash aunque el email no exista
  (tiempo constante aproximado) y devuelve el mismo error en ambos casos.
- Errores: `GlobalExceptionHandler` + `ApiErrorController` devuelven siempre
  `{code, message, fields?}` sin trazas. Cabeceras `X-Frame-Options: DENY`, `nosniff` (Spring Security).

## Analítica

Todas las cifras se agregan en SQL (`AnalyticsRepository`, consultas nativas con
`count(*) filter (where …)`); nunca se cargan interacciones en memoria para contarlas. La exportación CSV
recorre el resultado en *streaming* (`Stream<ExportRow>`, `fetchSize=500`).

Periodos (`PeriodResolver`): `today`, `7d`, `30d`, `this_month`, `last_month`, `custom` (máx. 366 días).
Se resuelven en el backend en la zona del negocio y se devuelven como fechas locales inclusivas. El periodo
de comparación es el inmediatamente anterior con la misma duración (o el mes natural anterior en los
presets mensuales). `changePercent` es `null` cuando el periodo anterior no tiene datos.

Endpoints por negocio: `summary` (totales, NFC/QR, comparación, dispositivo top), `timeline` (buckets
diarios en la zona del negocio, con días a cero rellenados en Java), `devices` (todos los dispositivos,
incluso sin uso, ordenados por total), `recent` (últimas N, anónimas), `export.csv`
(`from`, `to`, `deviceId?`). Global (ADMIN): `/api/admin/analytics/summary`.

## Privacidad (minimización de datos)

Por interacción se guarda: dispositivo, instante, canal, categoría gruesa del User-Agent
(móvil/tablet/escritorio/bot) y el **origen** del Referer (esquema + host, sin ruta ni query). No se guarda
IP, ni fingerprint, ni UA completo, ni cookies en `/d/**`. La única cookie del sistema es la de sesión del
panel. Sin analítica de terceros.

## Decisiones técnicas relevantes

- **Spring Boot 4.1 / Spring Security 7 / Hibernate 7 / Testcontainers 2** (últimas estables). El pom usa
  los nuevos starters (`spring-boot-starter-webmvc`, `*-test` por módulo).
- **Flyway** con `ddl-auto=validate`: Hibernate nunca modifica el esquema.
- **Sin MapStruct**: los DTOs son `record`s y los mapeos son métodos estáticos de 5 líneas.
- **Lombok** sólo para `@Getter/@Setter/@RequiredArgsConstructor/@Slf4j`.
- **Códigos públicos**: 10 caracteres de un alfabeto de 57 símbolos sin ambigüedades (≈58 bits,
  `SecureRandom`). No se expone el UUID interno.
- **QR**: ZXing genera PNG; el SVG se construye a partir de la `BitMatrix` (vectorial, imprimible a
  cualquier tamaño).
- **Angular 22** standalone + signals + zoneless, `@if/@for`, formularios reactivos, Chart.js directo (sin
  `ng2-charts`, que arrastra `@angular/cdk`). Cada vista maneja estados `loading / empty / error / success`
  y los errores HTTP se normalizan en `errorInterceptor` (nunca se muestra un error crudo).
- **Datos demo** sólo con `app.seed.enabled=true` (perfil `dev`); en `prod` el primer ADMIN se crea con
  `APP_BOOTSTRAP_ADMIN_*` y es idempotente.

## Preparado para crecer (sin implementar)

- Planes/suscripciones y límite de dispositivos: campos en `business` + comprobación en `DeviceService.create`.
- Múltiples establecimientos: entidad `location` entre `business` y `device`.
- Google Business Profile API: tabla `review_snapshot` por negocio; el redirect no cambia.
- Escalado horizontal: sustituir las cachés Caffeine (`InteractionGuard`, `LoginAttemptService`) por Redis.
