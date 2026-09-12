package com.reviewtap.interaction;

/** Página mínima cuando un enlace no está disponible. Mismo texto para todos los casos: no filtra causas. */
final class RedirectErrorPage {

    private RedirectErrorPage() {}

    static final String HTML = """
            <!doctype html>
            <html lang="es">
            <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <meta name="robots" content="noindex">
            <title>Enlace no disponible</title>
            <style>
              body{margin:0;min-height:100vh;display:flex;align-items:center;justify-content:center;
                   font-family:system-ui,-apple-system,Segoe UI,Roboto,sans-serif;background:#f6f7f9;color:#1f2937}
              main{max-width:420px;padding:2.5rem 2rem;text-align:center}
              .icon{width:56px;height:56px;border-radius:50%;background:#e5e7eb;margin:0 auto 1.25rem;
                    display:flex;align-items:center;justify-content:center;font-size:1.5rem}
              h1{font-size:1.25rem;margin:0 0 .5rem}
              p{margin:0;color:#6b7280;line-height:1.5}
            </style>
            </head>
            <body>
            <main>
              <div class="icon">&#128279;</div>
              <h1>Este enlace no está disponible</h1>
              <p>Puede que el dispositivo se haya desactivado o que la dirección no sea correcta.
                 Si crees que se trata de un error, consulta con el establecimiento.</p>
            </main>
            </body>
            </html>
            """;
}
