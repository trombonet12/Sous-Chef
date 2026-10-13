# ADR-0006: Ficha nativa en Compose e impresión mediante HTML

- Estado: aceptado
- Fecha: 2026-10-13
- Relacionado: `docs/decisiones.md` § UI, [ADR-0001](0001-stack-android-y-modulos.md), [spec del legado](../spec/app-legado.md), `docs/legado/ficha-plantilla.html`

## Contexto
- La ficha de receta es la pantalla principal de lectura y debe poder imprimirse o exportarse a PDF en A4 con el aspecto de la app vieja.
- Los cortes de verdura se describen con SVG; las líneas de `mep` admiten marcado mínimo.
- Se quieren las mismas fuentes (Bricolage Grotesque y Newsreader) en pantalla e impresión.

## Decisión
- La ficha en pantalla es **nativa en Compose**, siempre sobre fondo papel.
- Imprimir/PDF: Kotlin genera un HTML con las cantidades ya escaladas, que reutiliza el CSS A4 de `docs/legado/ficha-plantilla.html`, y se imprime con un `WebView` y `PrintDocumentAdapter`.
- Las fuentes viven en `assets/fonts/` y las comparten Compose y el WebView.
- Los SVG de cortes se pintan con coil-svg, siempre sobre fondo papel (independiente del tema oscuro).
- El campo `mep` solo admite `<b>` (lo exige el validador) y se convierte con `AnnotatedString.fromHtml`.

## Alternativas consideradas
| Alternativa | Pros | Contras | Motivo de descarte |
|---|---|---|---|
| Ficha entera en WebView | Una sola implementación para pantalla e impresión | Interacción, escalado y navegación menos nativos | La ficha es interactiva (raciones, enlaces a técnicas) |
| Imprimir desde Compose con `PdfDocument` | Sin WebView | Paginado A4 y tipografía manuales; replicar el CSS | Mucho trabajo para el mismo resultado |
| Librería de generación de PDF | Control total | Dependencia nueva y maquetación propia | El CSS A4 ya existe y funciona |
| Convertir los SVG a vectores Android en build | Sin librería en runtime | Cada corte exige conversión previa; no siempre convertible | coil-svg los pinta directamente |
| Compose en pantalla + HTML con WebView solo para imprimir | Reutiliza el CSS A4 probado; fuentes compartidas | Dos renderizados de la misma ficha | Elegido |

## Consecuencias
- Positivas: aspecto de impresión fiel al legado; ficha rápida y accesible en pantalla.
- Negativas / costes: dos vías de render que mantener alineadas (tests sobre el HTML generado); coil-svg es una dependencia nueva (skill dependency-add).
- Seguimiento: capturas del emulador y una impresión de prueba a PDF en QA.
