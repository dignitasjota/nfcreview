import { Component } from '@angular/core';

@Component({
  selector: 'app-privacy',
  template: `
    <h1>Política de privacidad</h1>
    <p class="muted small">Última actualización: septiembre de 2026</p>

    <h2>Qué hace este servicio</h2>
    <p>ReviewTap permite a establecimientos físicos colocar tarjetas o placas con chip NFC y/o código QR que, al acercar
      el móvil o escanearlas, abren la página oficial de Google para dejar una opinión sobre el negocio. Nosotros no
      publicamos, generamos ni filtramos reseñas: sólo facilitamos el acceso al enlace oficial.</p>

    <h2>Datos de las personas que usan un dispositivo NFC/QR</h2>
    <p>Cuando alguien abre uno de nuestros enlaces (<code>/d/…</code>) registramos una <strong>interacción anónima</strong>
      que contiene únicamente: el identificador del dispositivo, la fecha y hora, el canal (NFC o QR), una categoría
      genérica del tipo de aparato (móvil, tablet, ordenador) y, si el navegador lo envía, el dominio de origen.</p>
    <p><strong>No almacenamos</strong> direcciones IP, identificadores de publicidad, huellas del navegador, ubicación GPS,
      cookies de seguimiento ni ningún dato que permita identificar a la persona. La dirección IP se usa de forma transitoria,
      en memoria y mediante un resumen criptográfico con una clave que cambia a diario, con el único fin de no contar varias
      veces un mismo toque accidental. Ese resumen nunca se guarda en disco.</p>
    <p>No utilizamos Google Analytics ni servicios de terceros de analítica o publicidad. Tras la redirección, la
      navegación continúa en los servicios de Google, sujetos a su propia política de privacidad.</p>

    <h2>Datos de los usuarios del panel</h2>
    <p>Los administradores y usuarios de los negocios cliente acceden con email y contraseña. Almacenamos el email,
      nombre, la contraseña cifrada de forma irreversible (BCrypt) y la relación con los negocios que gestionan. Estos
      datos se tratan para prestar el servicio contratado y se conservan mientras dure la relación.</p>

    <h2>Cookies</h2>
    <p>El panel utiliza una única cookie técnica de sesión (<code>rt_session</code>), estrictamente necesaria para
      mantener la autenticación. Es <em>HttpOnly</em>, se limita al sitio y caduca a las pocas horas. Al no tratarse de una
      cookie de seguimiento ni de terceros, no requiere consentimiento previo. Las páginas de redirección
      (<code>/d/…</code>) no utilizan cookies.</p>

    <h2>Derechos</h2>
    <p>Puedes ejercer los derechos de acceso, rectificación, supresión, oposición, limitación y portabilidad escribiendo
      al responsable del servicio a través de los datos de contacto facilitados en tu contrato. Si consideras que el
      tratamiento no es adecuado, puedes reclamar ante la autoridad de control competente (en España, la AEPD).</p>
  `,
})
export class PrivacyComponent {}

@Component({
  selector: 'app-terms',
  template: `
    <h1>Términos del servicio</h1>
    <p class="muted small">Última actualización: septiembre de 2026</p>

    <h2>Objeto</h2>
    <p>ReviewTap ofrece a negocios físicos dispositivos NFC/QR y un panel de estadísticas asociado. Cada dispositivo redirige
      a la URL de reseñas de Google que el propio negocio configura.</p>

    <h2>Qué miden las estadísticas</h2>
    <p>Las cifras del panel son <strong>interacciones</strong>: aperturas del enlace desde un dispositivo. Una interacción
      no implica que la persona haya publicado finalmente una reseña, ni ReviewTap tiene acceso al contenido o número de
      reseñas en Google. Las cifras son orientativas y pueden diferir de las de Google.</p>

    <h2>Uso permitido</h2>
    <ul>
      <li>El negocio es responsable de la URL de destino que configura y de cumplir las políticas de Google sobre reseñas
        (en particular, no incentivar, comprar ni filtrar opiniones).</li>
      <li>No está permitido usar los dispositivos para redirigir a contenidos ilícitos o engañosos.</li>
      <li>Las credenciales de acceso son personales; el titular debe custodiarlas y notificar cualquier uso no autorizado.</li>
    </ul>

    <h2>Disponibilidad y responsabilidad</h2>
    <p>Procuramos un servicio continuo, pero no garantizamos disponibilidad ininterrumpida. ReviewTap no se responsabiliza
      de cambios en los servicios de Google que afecten a las URLs de reseñas ni de las decisiones de negocio tomadas a
      partir de las estadísticas.</p>

    <h2>Baja</h2>
    <p>El negocio puede solicitar la desactivación de sus dispositivos y la eliminación de su cuenta en cualquier momento.
      Los datos agregados y anónimos de interacciones pueden conservarse con fines estadísticos.</p>
  `,
})
export class TermsComponent {}
