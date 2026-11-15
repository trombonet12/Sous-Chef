@file:Suppress("TooManyFunctions") // una función por sección de la ficha

package app.trombito.souschef.ui.ficha

import app.trombito.souschef.receta.Bloque
import app.trombito.souschef.receta.Bloques
import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Escalado
import app.trombito.souschef.receta.Gantt
import app.trombito.souschef.receta.Ingrediente
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.crono.OrdenPaso
import app.trombito.souschef.ui.comun.conNegritas
import java.util.Locale

private const val COLOR_CARRIL_DEFECTO = "#B0A899"
private val COLOR_HEX = Regex("#[0-9A-Fa-f]{6}")
private const val ANCHO_CORTE_PX = 120
private const val CADA_REJILLA_MIN = 10
private const val ANCHO_MINIMO_BARRA = 0.03
private const val LUMINANCIA_CLARA = 150
private const val TINTA = "#191A14"

internal fun esc(texto: String): String =
    buildString(texto.length + 8) {
        texto.forEach {
            when (it) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(it)
            }
        }
    }

/** Texto escapado donde solo se respeta `<b>...</b>`. */
internal fun rico(texto: String): String =
    conNegritas(texto).joinToString("") { if (it.negrita) "<b>${esc(it.texto)}</b>" else esc(it.texto) }

/**
 * Prosa estructurada (ADR-0017) en `<div class="prosa [clase]">` con `<p>` y `<ul>`; nada si el texto no tiene bloques.
 * Un texto de una sola línea sale como un único `<p>`.
 */
private fun prosa(
    b: StringBuilder,
    texto: String?,
    clase: String = "",
) {
    val bloques = Bloques.analizar(texto.orEmpty())
    if (bloques.isEmpty()) return
    b.append("<div class=\"prosa").append(if (clase.isEmpty()) "" else " $clase").append("\">")
    bloques.forEach { bloque ->
        when (bloque) {
            is Bloque.Parrafo -> {
                b.append("<p>").append(rico(bloque.texto)).append("</p>")
            }

            is Bloque.Lista -> {
                b.append("<ul>")
                bloque.elementos.forEach { b.append("<li>").append(rico(it)).append("</li>") }
                b.append("</ul>")
            }
        }
    }
    b.append("</div>")
}

private fun colorCss(carril: Carril?): String = carril?.color?.takeIf { COLOR_HEX.matches(it) } ?: COLOR_CARRIL_DEFECTO

/** Tinta que se lee sobre [fondo] (un `#RRGGBB`): oscura sobre colores claros, blanca sobre los oscuros. */
private fun tintaSobre(fondo: String): String {
    val n = fondo.removePrefix("#").toInt(16)
    val luminancia = 0.299 * (n shr 16 and 0xFF) + 0.587 * (n shr 8 and 0xFF) + 0.114 * (n and 0xFF)
    return if (luminancia > LUMINANCIA_CLARA) TINTA else "#FFFFFF"
}

private fun pct(v: Number): String = String.format(Locale.ROOT, "%.2f%%", v.toDouble() * 100)

private val CSS =
    """
    @page{size:A4;margin:13mm}
    *{box-sizing:border-box}
    html,body{margin:0;background:#fff;color:$TINTA}
    body{font-family:Georgia,'Times New Roman',serif;font-size:10pt;line-height:1.4;padding:0}
    h1,h2,h3,.ui{font-family:system-ui,'Helvetica Neue',Arial,sans-serif}
    h1{font-size:30pt;line-height:1.05;margin:0 0 6pt;font-weight:600}
    .hoja h1{font-size:24pt;margin-bottom:2pt}
    .kick{margin:0 0 2pt;font-family:system-ui,Arial,sans-serif;font-size:8.5pt;font-weight:600;
      text-transform:uppercase;letter-spacing:.12em;color:#6B6C5F}
    .desc{color:#46473D;margin:0 0 10pt;max-width:70ch}
    .datos{display:flex;flex-wrap:wrap;gap:4pt 22pt;margin:8pt 0 0}
    .hoja .datos{margin-top:4pt}
    .dato b{display:block;font-family:system-ui,Arial,sans-serif;font-size:15pt;font-weight:600}
    .dato span{font-family:system-ui,Arial,sans-serif;font-size:8pt;color:#6B6C5F;text-transform:uppercase;letter-spacing:.06em}
    section{margin-top:16pt}
    .hoja section{margin-top:11pt}
    h2{font-size:9.5pt;text-transform:uppercase;letter-spacing:.12em;color:#6B6C5F;margin:0 0 8pt;
      padding-bottom:3pt;border-bottom:1px solid #D6D5C6;font-weight:600}
    .hoja h2{margin-bottom:6pt}
    h3{font-size:10.5pt;margin:8pt 0 3pt;font-weight:600}
    .zonas{display:grid;grid-template-columns:repeat(auto-fit,minmax(190pt,1fr));gap:4pt 18pt}
    .zona h3{margin:0 0 2pt;font-size:9.5pt;text-transform:uppercase;letter-spacing:.05em}
    .ing{display:grid;grid-template-columns:56pt 1fr;gap:1pt 8pt}
    .ing b{text-align:right;font-weight:600;font-variant-numeric:tabular-nums}
    .nota{font-size:9pt;color:#46473D;margin:6pt 0 0}
    .diagrama{break-inside:avoid}
    .carril{display:grid;grid-template-columns:96pt 1fr;gap:8pt;align-items:center;padding:2pt 0}
    .carril .et{font-family:system-ui,Arial,sans-serif;font-size:8pt;font-weight:600;color:#46473D;
      text-transform:uppercase;letter-spacing:.04em;line-height:1.15}
    .pista{position:relative;height:15pt;background:#EFEDE3;border-radius:3px;overflow:hidden}
    .barra{position:absolute;top:1px;height:calc(100% - 2px);border-radius:3px;border:1px solid rgba(25,26,20,.35);
      display:flex;align-items:center;justify-content:center;overflow:hidden;
      font-family:system-ui,Arial,sans-serif;font-size:7.5pt;font-weight:700;line-height:1}
    .regla{padding-bottom:2pt}
    .ticks{position:relative;height:11pt;font-family:system-ui,Arial,sans-serif;font-size:7.5pt;color:#6B6C5F}
    .ticks span{position:absolute;transform:translateX(-50%);font-variant-numeric:tabular-nums}
    .meta{font-family:system-ui,Arial,sans-serif;font-size:8pt;color:#6B6C5F;text-transform:uppercase;letter-spacing:.04em}
    .diagrama .meta{margin:4pt 0 0}
    .lectura{margin:6pt 0 0}
    .mep{display:grid;grid-template-columns:repeat(auto-fit,minmax(200pt,1fr));gap:10pt 18pt}
    .mep h3{font-size:9.5pt;letter-spacing:.05em;margin:0 0 2pt}
    .mep .porq{font-size:9pt;color:#46473D;margin:0 0 4pt}
    .mep ul,.mep ol{margin:0;padding-left:16pt}
    .mep li{padding:1pt 0}
    .cron{margin:0;padding:0;list-style:none}
    .ord{margin:0 0 7pt;padding-left:25pt;position:relative}
    .hoja .ord{margin-bottom:5pt}
    .ord>i{position:absolute;left:0;top:0;width:18pt;height:18pt;border-radius:50%;background:$TINTA;color:#fff;
      font:600 8.5pt/18pt system-ui,Arial,sans-serif;text-align:center}
    .cab{font-family:system-ui,Arial,sans-serif;font-size:10.5pt;line-height:1.3}
    .cab .cm{font-size:8.5pt;font-weight:600;color:#46473D;text-transform:uppercase;letter-spacing:.04em;
      font-variant-numeric:tabular-nums}
    .cab b{font-weight:600}
    .l{margin:1pt 0 0;font-size:9.5pt;line-height:1.3;padding-left:38pt;text-indent:-38pt}
    .k{display:inline-block;min-width:38pt;text-indent:0;font-family:system-ui,Arial,sans-serif;font-size:8pt;
      font-weight:700;letter-spacing:.06em;text-transform:uppercase;color:#46473D}
    .dat{font-family:system-ui,Arial,sans-serif;font-size:9pt;padding-left:0;text-indent:0;color:#46473D;
      font-variant-numeric:tabular-nums}
    .porqp{margin-top:3pt;color:#46473D}
    .porqp .k{display:block;margin-bottom:1pt}
    .corte{display:grid;grid-template-columns:${ANCHO_CORTE_PX}px 1fr;gap:12pt;align-items:start;margin-bottom:10pt}
    .corte svg{max-width:100%;height:auto}
    .corte p{margin:2pt 0 0}
    .porque div{margin-top:8pt}
    .porque dt{font-family:system-ui,Arial,sans-serif;font-weight:600}
    .porque dd{margin:2pt 0 0;color:#46473D}
    div.prosa p{margin:0 0 4pt}
    div.prosa ul{margin:0 0 4pt;padding-left:14pt}
    div.prosa li{margin:0 0 2pt}
    div.prosa>:last-child{margin-bottom:0}
    .pie{margin-top:16pt;font-size:9pt;color:#46473D}
    @media print{
      .zona,.diagrama,.ord,.mep>div,.corte,.porque div,div.prosa p,div.prosa ul{break-inside:avoid}
      h2{break-after:avoid}
      .pista,.barra,.ord>i{-webkit-print-color-adjust:exact;print-color-adjust:exact}
    }
    """.trimIndent()

/** Un paso con el número que lleva en el diagrama y en la cronología: el de su orden por `off` (M6). */
private class PasoOrdenado(
    val n: Int,
    val paso: Paso,
)

/** Todo lo que necesita una salida del HTML: la receta, los textos y cuál de las dos salidas es. */
private class Salida(
    val receta: Receta,
    val raciones: Int,
    val textos: TextosFicha,
    val imp: TextosImpresion,
    val tipo: TipoFicha,
    servirMin: Int?,
) {
    val completa: Boolean get() = tipo == TipoFicha.COMPLETA

    /** Pasos por orden de reloj (`off`); a igual `off` manda el orden del array. */
    val pasos: List<PasoOrdenado> = receta.pasos.sortedBy { it.off }.mapIndexed { i, p -> PasoOrdenado(i + 1, p) }

    private val empiezaMin: Int? = servirMin?.let { Servir.inicio(it, receta) }
    val servir: String? = servirMin?.let { Servir.formato(it) }
    val empieza: String? = empiezaMin?.let { Servir.formato(it) }

    /** Hora de reloj a la que arranca un paso que empieza en el minuto [off], si se conoce la hora de servir. */
    fun hora(off: Double): String? = empiezaMin?.let { Servir.formato(Servir.ajustar(it, Math.round(off).toInt())) }
}

/**
 * HTML autónomo de la ficha, con las cantidades escaladas a [raciones], listo para imprimir o guardar en PDF.
 * [tipo] elige la salida: la hoja de cocina (1 o 2 páginas) o la ficha completa. Con [servirMin] (minutos desde
 * medianoche) salen la hora de empezar, la de servir y la de cada paso.
 */
@Suppress("LongParameterList") // la receta, sus textos y las tres opciones de la salida
fun fichaHtml(
    receta: Receta,
    raciones: Int,
    textos: TextosFicha,
    tipo: TipoFicha = TipoFicha.COMPLETA,
    servirMin: Int? = null,
    impresion: TextosImpresion = TextosImpresion.VACIO,
): String {
    val s = Salida(receta, raciones, textos, impresion, tipo, servirMin)
    val b = StringBuilder(16_000)
    b.append("<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"utf-8\">")
    b.append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
    b
        .append("<title>")
        .append(esc(receta.titulo))
        .append("</title><style>")
        .append(CSS)
        .append("</style></head><body class=\"")
        .append(if (s.completa) "completa" else "hoja")
        .append("\">")
    s.portada(b)
    s.ingredientes(b)
    s.tiempo(b)
    if (s.completa) s.mep(b)
    s.cronologia(b)
    if (s.completa) {
        s.tecnica(b)
        receta.pie?.takeIf { it.isNotBlank() }?.let { b.append("<p class=\"pie\">").append(rico(it)).append("</p>") }
    }
    b.append("</body></html>")
    return b.toString()
}

private fun Salida.portada(b: StringBuilder) {
    val etiqueta = if (completa) imp.completa else imp.hoja
    b.append("<header>")
    if (etiqueta.isNotBlank()) b.append("<p class=\"kick\">").append(esc(etiqueta)).append("</p>")
    b.append("<h1>").append(esc(receta.titulo)).append("</h1>")
    if (completa) prosa(b, receta.descripcion, "desc")
    b.append("<div class=\"datos\">")
    dato(b, raciones.toString(), textos.raciones(raciones).removePrefix("$raciones "))
    empieza?.let { dato(b, it, imp.empieza) }
    servir?.let { dato(b, it, imp.sirve) }
    receta.datos.filter { it.isNotEmpty() }.forEach { dato(b, it[0], it.getOrElse(1) { "" }) }
    b.append("</div></header>")
}

private fun dato(
    b: StringBuilder,
    valor: String,
    etiqueta: String,
) {
    b.append("<div class=\"dato\"><b>").append(esc(valor)).append("</b><span>")
    b.append(esc(etiqueta)).append("</span></div>")
}

/** Los ingredientes una sola vez, agrupados por recipiente o zona (la lista «todo junto» ya no se repite). */
private fun Salida.ingredientes(b: StringBuilder) {
    if (receta.grupos.isEmpty()) return
    val f = Escalado.factor(raciones, receta.raciones)
    b.append("<section><h2>").append(esc(textos.ingredientes)).append("</h2><div class=\"zonas\">")
    receta.grupos.forEach { g ->
        b.append("<div class=\"zona\"><h3>").append(esc(g.n)).append("</h3>")
        filasIngredientes(b, g.items, f)
        b.append("</div>")
    }
    b.append("</div>")
    prosa(b, receta.notaEscala, "nota")
    b.append("</section>")
}

private fun filasIngredientes(
    b: StringBuilder,
    items: List<Ingrediente>,
    f: Double,
) {
    b.append("<div class=\"ing\">")
    items.forEach {
        b
            .append("<b>")
            .append(esc(Escalado.cantidad(it, f)))
            .append("</b><span>")
            .append(esc(it.n))
            .append("</span>")
    }
    b.append("</div>")
}

/** El diagrama por carriles (una fila por carril, los pasos numerados como en la cronología); entero en una página. */
private fun Salida.tiempo(b: StringBuilder) {
    val total = receta.totalEfectivo
    if (total <= 0 || pasos.isEmpty()) return
    b.append("<section class=\"diagrama\"><h2>").append(esc(textos.tiempo)).append("</h2>")
    b.append("<div class=\"carril regla\"><div></div><div class=\"ticks\">")
    Gantt.marcas(total).forEach {
        b.append("<span style=\"left:").append(pct(it / total)).append("\">")
        b.append(esc(Escalado.minutos(it))).append("</span>")
    }
    b.append("</div></div>")
    val ids = (receta.carriles.map { it.id } + pasos.map { it.paso.carril }).distinct()
    ids.forEach { id ->
        val delCarril = pasos.filter { it.paso.carril == id }
        if (delCarril.isNotEmpty()) filaCarril(b, receta.carril(id), id, delCarril, total)
    }
    b.append("<p class=\"meta\">").append(esc(textos.eje)).append("</p>")
    if (completa) prosa(b, receta.lectura, "lectura")
    b.append("</section>")
}

private fun filaCarril(
    b: StringBuilder,
    carril: Carril?,
    id: String,
    pasos: List<PasoOrdenado>,
    total: Double,
) {
    b.append("<div class=\"carril\"><div class=\"et\">").append(esc(carril?.nombre ?: id)).append("</div>")
    b.append("<div class=\"pista\" style=\"background-image:").append(rejilla(total)).append("\">")
    val color = colorCss(carril)
    pasos.forEach {
        val (izq, ancho) = Gantt.barra(it.paso, total, anchoMinimo = ANCHO_MINIMO_BARRA)
        b
            .append("<div class=\"barra\" style=\"left:")
            .append(pct(izq))
            .append(";width:")
            .append(pct(ancho))
        b.append(";background:$color;color:${tintaSobre(color)}\">")
        b.append(it.n).append("</div>")
    }
    b.append("</div></div>")
}

/** Rejilla cada 10 min como degradado repetido; si no cabe ninguna línea, sin rejilla. */
private fun rejilla(total: Double): String {
    if (total <= CADA_REJILLA_MIN) return "none"
    val paso = CADA_REJILLA_MIN / total * 100
    val pasoTxt = String.format(Locale.ROOT, "%.3f%%", paso)
    return "linear-gradient(to right,transparent calc(100% - 1px),#D6D5C6 0);background-size:$pasoTxt 100%"
}

private fun Salida.mep(b: StringBuilder) {
    if (receta.mep.isEmpty()) return
    b.append("<section><h2>").append(esc(textos.mep)).append("</h2><div class=\"mep\">")
    receta.mep.forEach { bl ->
        b.append("<div><h3>").append(esc(bl.t)).append("</h3>")
        prosa(b, bl.porq, "porq")
        val tag = if (bl.orden) "ol" else "ul"
        b.append("<$tag>")
        bl.l.forEach { b.append("<li>").append(rico(it)).append("</li>") }
        b.append("</$tag></div>")
    }
    b.append("</div></section>")
}

/** La cronología por orden de `off`: cada paso es una orden completa; en la ficha completa lleva también su porqué. */
private fun Salida.cronologia(b: StringBuilder) {
    if (pasos.isEmpty()) return
    b.append("<section><h2>").append(esc(imp.cronologia.ifBlank { textos.pasos })).append("</h2><ol class=\"cron\">")
    pasos.forEach { entrada(b, it) }
    b.append("</ol></section>")
}

private fun Salida.entrada(
    b: StringBuilder,
    o: PasoOrdenado,
) {
    val p = o.paso
    val carril = receta.carril(p.carril)?.nombre ?: p.carril
    val cabecera =
        listOfNotNull(imp.minuto(Escalado.minutos(p.off)), hora(p.off), carril.uppercase()).joinToString(" · ")
    b.append("<li class=\"ord\"><i>").append(o.n).append("</i>")
    b.append("<div class=\"cab\"><span class=\"cm\">").append(esc(cabecera)).append(" ·</span> <b>")
    b.append(esc(p.t)).append("</b></div>")
    p.meto?.takeIf { it.isNotBlank() }?.let { linea(b, imp.mete, rico(it)) }
    b.append("<p class=\"l dat\">").append(esc(datosPaso(p))).append("</p>")
    p.hitosValidos.takeIf { it.isNotEmpty() }?.let { hitos ->
        linea(b, imp.avisos, hitos.joinToString(" · ") { esc("${formatoHito(it.en)} ${it.t}") })
    }
    p.nota?.takeIf { it.isNotBlank() }?.let { linea(b, imp.senal, rico(it)) }
    p.saco?.takeIf { it.isNotBlank() }?.let { linea(b, imp.saca, rico(it)) }
    if (completa && !p.p.isNullOrBlank()) {
        b.append("<div class=\"porqp\"><span class=\"k\">").append(esc(imp.porque)).append("</span>")
        prosa(b, p.p)
        b.append("</div>")
    }
    b.append("</li>")
}

/** «fuego 4 · sin tapa · 5′» (o «sin fuego · 5′» si el paso no usa fuego). */
private fun Salida.datosPaso(p: Paso): String =
    listOfNotNull(
        p.f?.let { textos.fuego(it) } ?: textos.sinFuego,
        OrdenPaso.textoTapa(p.tapa) ?: p.tapa?.takeIf { it.isNotBlank() }?.lowercase(),
        textos.duracion(Escalado.minutos(p.min)),
    ).joinToString(" · ")

/** Una línea de la orden con su etiqueta (METE, AVISOS, SEÑAL, SACA); [html] ya viene escapado. */
private fun linea(
    b: StringBuilder,
    etiqueta: String,
    html: String,
) {
    b.append("<p class=\"l\"><span class=\"k\">${esc(etiqueta)}</span> $html</p>")
}

private fun Salida.tecnica(b: StringBuilder) {
    if (receta.cortes.isEmpty() && receta.porque.isEmpty()) return
    b.append("<section><h2>").append(esc(textos.tecnica)).append("</h2>")
    receta.cortes.forEach { c ->
        b.append("<div class=\"corte\"><div>")
        c.svg?.takeIf { it.isNotBlank() }?.let { b.append(it) }
        b.append("</div><div><h3>").append(esc(c.t)).append("</h3>")
        prosa(b, c.p)
        b.append("</div></div>")
    }
    val porque = receta.porque.filter { it.isNotEmpty() }
    if (porque.isNotEmpty()) {
        b.append("<dl class=\"porque\">")
        porque.forEach {
            b.append("<div><dt>").append(esc(it[0])).append("</dt>")
            it.getOrNull(1)?.takeIf { d -> d.isNotBlank() }?.let { d ->
                b.append("<dd>")
                prosa(b, d)
                b.append("</dd>")
            }
            b.append("</div>")
        }
        b.append("</dl>")
    }
    b.append("</section>")
}
