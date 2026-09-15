# ReviewTap — Contexto del proyecto

SaaS B2B de dispositivos **NFC/QR** que llevan a los clientes de un comercio a la pantalla de reseñas de
Google. Estado a **2026-09-15**: MVP funcional de extremo a extremo, auditado y endurecido, con landing de
venta, CI en verde y desplegable con `docker compose`. Repo: https://github.com/dignitasjota/nfcreview

> Vocabulario: las cifras son **interacciones** (aperturas del enlace), nunca "reseñas conseguidas".

## Stack (elegido por requisito explícito del cliente, no por defecto del autor)

| Capa | Tecnología |
|---|---|
| Backend | Java 21 · Spring Boot **4.1.1** (Spring Framework 7, Security 7, Hibernate 7, Jackson 3) · Maven |
| BD | PostgreSQL 16 · Flyway (`ddl-auto=validate`) · Testcontainers 2 |
| Frontend | Angular **22** standalone + signals + zoneless · Vitest · Chart.js directo |
| Landing | HTML/CSS/JS estático sin build (`landing/`) |
| Infra | Docker Compose (postgres, backend, frontend nginx-unprivileged, landing, backup opcional) · GitHub Actions |

## Estructura

```
backend/src/main/java/com/reviewtap/
  auth/        JWT en cookie, filtro, BusinessAccess (@access), lockout, reset de contraseña, MailService
  business/    Business, BusinessUser (N:M con rol OWNER/MANAGER), servicio y controlador
  device/      Device (publicCode), URLs NFC/QR, QR PNG/SVG (ZXing), PublicCodeGenerator
  interaction/ RedirectController (/d/{code}), InteractionGuard (dedupe), ClientKeyResolver, recorder @Async
  analytics/   AnalyticsRepository (SQL nativo), PeriodResolver, export CSV
  user/        User, AdminUserController
  seed/        AdminBootstrap (prod) y DevDataSeeder (perfil dev)
  common/      ApiError, GlobalExceptionHandler, ApiErrorController, @HttpsUrl
  config/      AppProperties (app.*), SecurityConfig, AsyncConfig, OpenApiConfig
backend/src/main/resources/db/migration/   V1 init · V2 token_version · V3 password_reset_token
frontend/src/app/  core/ (api, guards, interceptores, session.store) · shared/ui · layout/shell · features/{auth,business,admin,shared,public}
landing/           index.html · styles.css · app.js (bloque SITE editable) · fonts/ · img/ · nginx.conf
docs/              ARCHITECTURE.md · DEPLOYMENT.md
```

## Comandos

```bash
# Backend (JAVA_HOME=/opt/homebrew/opt/openjdk@21 en el Mac de desarrollo; maven arrastra un JDK 26)
cd backend && mvn test                       # 70 tests, Testcontainers → necesita Docker
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run   # seed demo: admin@example.local / pepe@example.local · Demo1234!
# Frontend (Node 22.22+; npm 10.9 tiene un bug de arborist → --legacy-peer-deps)
cd frontend && npm install --legacy-peer-deps && npm start        # proxy /api → :8080
npx ng test --watch=false && npx ng build --configuration production
# Todo
cp .env.example .env && docker compose up -d --build              # + --profile backup
```

## Invariantes que no se negocian

1. **La URL de destino sale siempre de la BD** y se valida `https://` al guardar (`@HttpsUrl`). Nunca del request.
2. **Autorización a nivel de recurso**: todo endpoint con `businessId`/`deviceId` lleva `@PreAuthorize("@access.…")`. ADMIN ve todo; BUSINESS_USER sólo lo suyo (`TenantAuthorizationIT` lo prueba).
3. **Minimización de datos**: `interaction` no guarda IP, UA completo ni nada personal. Las claves de dedupe son HMAC con salt diario en memoria.
4. **La IP del cliente es `request.getRemoteAddr()`** saneada por Tomcat (`forward-headers-strategy=native`). No leer `X-Forwarded-For` a mano (ver lección 3).
5. **Sesión = cookie HttpOnly SameSite=Strict con JWT + `token_version`**. Cualquier cambio de contraseña, logout o deshabilitación incrementa la versión y revoca todas las cookies.
6. **Agregaciones en SQL**, nunca cargar interacciones en Java para contarlas.
7. **Un `publicCode` por dispositivo físico** y `?src=nfc|qr` validado contra whitelist (otro valor → `UNKNOWN`).
8. **Sin datos inventados en la landing**: cifras y testimonios viven en `SITE` y se ocultan si están vacíos.

## Lecciones aprendidas (para no repetir)

1. **Spring Boot 4 cambia artefactos y paquetes**: `spring-boot-starter-webmvc`, starters de test por módulo (`spring-boot-starter-webmvc-test`), `AutoConfigureMockMvc` en `org.springframework.boot.webmvc.test.autoconfigure`, `ErrorController` en `org.springframework.boot.webmvc.error`, Testcontainers 2 en `org.testcontainers.postgresql`, Jackson 3 en `tools.jackson`. Generar el pom desde start.spring.io y buscar las clases en `~/.m2` con `unzip -l` antes de adivinar imports.
2. **Surefire no ejecuta `*IT` por defecto**: añadir `**/*IT.java` a `includes` o los tests de integración parecen pasar porque no corren.
3. **Cabeceras `X-Forwarded-For`**: con `forward-headers-strategy=framework` Spring toma el **primer** valor (el que fabrica el cliente). Con `native`, el `RemoteIpValve` calcula bien `getRemoteAddr()` pero **reescribe la cabecera** dejando los saltos no confiables, y si toda la cadena es privada deja la primera privada. Por eso: sólo `getRemoteAddr()`, IPs privadas → cubo único, techo por IP sin UA y techo por dispositivo. **MockMvc no pasa por Tomcat**: el fix "correcto" pasaba los tests y seguía siendo vulnerable; hay que probar contra el servidor real.
4. **Bots y previsualizadores** (WhatsApp, iMessage, Googlebot…) y peticiones `HEAD` (Spring mapea HEAD a `@GetMapping`) cuentan como interacciones si no se filtran.
5. **Una anotación Bean Validation no admite métodos `static`** en la interfaz; y un error en una anotación aborta el procesado de Lombok → cascada de "cannot find symbol" que despista. Arreglar primero el error de la anotación.
6. **Angular: `inject()` después de un `await` en un guard lanza NG0203**. Resolver todas las dependencias antes del primer `await`.
7. **Angular producción + CSP `script-src 'self'`**: la optimización `inlineCritical` emite `<link onload="…">` inline. Desactivar `optimization.styles.inlineCritical` en `angular.json`.
8. **nginx**: una `location` regex (`~* \.(png|js)$`) gana a un prefijo `/api/` → usar `^~ /api/`. `add_header` dentro de una `location` **anula** los del `server` → cabeceras de seguridad en un `include` repetido en cada location. `proxy_set_header Host $http_host` (con puerto) y `X-Forwarded-Port`: Spring compara `Origin` con `Host` y devuelve **403 sólo desde el navegador** (curl no envía `Origin`).
9. **Bloqueo de login sólo por email = DoS a un cliente concreto**. Clave `email+IP` y techo por IP.
10. **Google Fonts en la UE** transfiere la IP a Google: descargar los `woff2` (curl con UA de Chrome para obtener el CSS con woff2) y `@font-face` local.
11. **Playwright**: `fullPage` screenshots desplazan elementos `fixed`; un toast fijo puede interceptar clicks; `input[name=x]` casa con el primer formulario del DOM; un click `force` sobre un backdrop cierra el drawer. Escribir los selectores acotados al contenedor y no usar `networkidle` con `ng serve` (el websocket de HMR lo impide).
12. **Ecosistema local**: Homebrew `maven` instala `openjdk` 26 → fijar `JAVA_HOME` a 21. Los puertos 5432/5433/8080 suelen estar ocupados por otros proyectos → usar puertos altos para pruebas.

## Fuera del MVP (deliberado)

Pagos/planes, Google Business Profile API, lectura/respuesta de reseñas, IA, WhatsApp API, multiidioma,
white label, PWA, multi-establecimiento. Ver `docs/ARCHITECTURE.md` § "Preparado para crecer".
