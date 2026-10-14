@file:Suppress("TooManyFunctions") // una función por sección de la ficha

package app.souschef.ui.ficha

import app.souschef.receta.Carril
import app.souschef.receta.Escalado
import app.souschef.receta.Gantt
import app.souschef.receta.Paso
import app.souschef.receta.Receta
import app.souschef.ui.comun.conNegritas
import java.util.Locale

private const val COLOR_CARRIL_DEFECTO = "#B0A899"
private val COLOR_HEX = Regex("#[0-9A-Fa-f]{6}")
private const val ANCHO_CORTE_PX = 120
private const val CADA_REJILLA_MIN = 10

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

private fun colorCss(carril: Carril?): String = carril?.color?.takeIf { COLOR_HEX.matches(it) } ?: COLOR_CARRIL_DEFECTO

private fun pct(v: Number): String = String.format(Locale.ROOT, "%.2f%%", v.toDouble() * 100)

private val CSS =
    """
    @page{size:A4;margin:13mm}
    *{box-sizing:border-box}
    html,body{margin:0;background:#fff;color:#191A14}
    body{font-family:Georgia,'Times New Roman',serif;font-size:10pt;line-height:1.4;padding:0}
    h1,h2,h3,.ui{font-family:system-ui,'Helvetica Neue',Arial,sans-serif}
    h1{font-size:30pt;line-height:1.05;margin:0 0 6pt;font-weight:600}
    .desc{font-style:italic;color:#46473D;margin:0 0 10pt;max-width:70ch}
    .datos{display:flex;flex-wrap:wrap;gap:4pt 22pt;margin:8pt 0 0}
    .dato b{display:block;font-family:system-ui,Arial,sans-serif;font-size:15pt;font-weight:600}
    .dato span{font-family:system-ui,Arial,sans-serif;font-size:8pt;color:#6B6C5F;text-transform:uppercase;letter-spacing:.06em}
    section{margin-top:16pt}
    h2{font-size:9.5pt;text-transform:uppercase;letter-spacing:.12em;color:#6B6C5F;margin:0 0 8pt;
      padding-bottom:3pt;border-bottom:1px solid #D6D5C6;font-weight:600}
    h3{font-size:10.5pt;margin:8pt 0 3pt;font-weight:600}
    .ing{display:grid;grid-template-columns:90pt 1fr;gap:2pt 10pt}
    .ing b{text-align:right;font-weight:600;font-variant-numeric:tabular-nums}
    .nota{font-size:9pt;font-style:italic;color:#6B6C5F;margin:6pt 0 0}
    .fila{display:grid;grid-template-columns:150pt 1fr 40pt;gap:8pt;align-items:center;padding:3pt 0}
    .fila .et b{font-family:system-ui,Arial,sans-serif;font-weight:500;font-size:10pt;display:block;line-height:1.2}
    .fila .et span{font-family:system-ui,Arial,sans-serif;font-size:7.5pt;color:#6B6C5F;text-transform:uppercase;
      letter-spacing:.04em}
    .fila .dur{text-align:right;font-family:system-ui,Arial,sans-serif;font-size:9pt;font-variant-numeric:tabular-nums}
    .pista{position:relative;height:11pt;background:#EFEDE3;border-radius:3px;overflow:hidden}
    .barra{position:absolute;top:1px;height:calc(100% - 2px);border-radius:3px;opacity:.9;border:1px solid rgba(25,26,20,.35)}
    .regla{padding-bottom:2pt}
    .ticks{position:relative;height:11pt;font-family:system-ui,Arial,sans-serif;font-size:7.5pt;color:#6B6C5F}
    .ticks span{position:absolute;transform:translateX(-50%);font-variant-numeric:tabular-nums}
    .leyenda{display:flex;flex-wrap:wrap;gap:3pt 12pt;margin-top:6pt;font-family:system-ui,Arial,sans-serif;
      font-size:8pt;color:#46473D}
    .leyenda i{display:inline-block;width:8pt;height:8pt;border-radius:50%;margin-right:4pt}
    .lectura{font-style:italic;margin:6pt 0 0}
    .mep{display:grid;grid-template-columns:repeat(auto-fit,minmax(200pt,1fr));gap:10pt 18pt}
    .mep h3{font-size:9.5pt;letter-spacing:.05em;margin:0 0 2pt}
    .mep .porq{font-size:9pt;color:#6B6C5F;margin:0 0 4pt}
    .mep ul,.mep ol{margin:0;padding-left:16pt}
    .mep li{padding:1pt 0}
    .pasos{margin:0;padding:0;list-style:none}
    .pasos li{margin:0 0 8pt;padding-left:22pt;position:relative}
    .pasos li>i{position:absolute;left:0;top:0;font-style:normal;font-family:system-ui,Arial,sans-serif;font-weight:600}
    .pasos b.t{font-family:system-ui,Arial,sans-serif;font-weight:600}
    .meta{font-family:system-ui,Arial,sans-serif;font-size:8pt;color:#6B6C5F;text-transform:uppercase;letter-spacing:.04em}
    .pasos p{margin:2pt 0 0}
    .hito{font-family:system-ui,Arial,sans-serif;font-size:8.5pt;color:#46473D;font-variant-numeric:tabular-nums}
    .corte{display:grid;grid-template-columns:${ANCHO_CORTE_PX}px 1fr;gap:12pt;align-items:start;margin-bottom:10pt}
    .corte svg{max-width:100%;height:auto}
    .corte p{margin:2pt 0 0}
    .porque div{margin-top:8pt}
    .porque dt{font-family:system-ui,Arial,sans-serif;font-weight:600}
    .porque dd{margin:2pt 0 0;color:#46473D}
    .compra{margin:0;padding-left:16pt}
    .pie{margin-top:16pt;font-size:8.5pt;color:#6B6C5F}
    @media print{
      .fila,.pasos li,.mep>div,.corte,.porque div{break-inside:avoid}
      .pista,.barra,.leyenda i{-webkit-print-color-adjust:exact;print-color-adjust:exact}
    }
    """.trimIndent()

/** HTML autónomo de la ficha, con las cantidades escaladas a [raciones], listo para imprimir o guardar en PDF. */
fun fichaHtml(
    receta: Receta,
    raciones: Int,
    textos: TextosFicha,
): String {
    val b = StringBuilder(16_000)
    b.append("<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"utf-8\">")
    b.append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
    b
        .append("<title>")
        .append(esc(receta.titulo))
        .append("</title><style>")
        .append(CSS)
        .append("</style></head><body>")
    portada(b, receta, raciones, textos)
    ingredientes(b, receta, raciones, textos)
    tiempo(b, receta, textos)
    mep(b, receta, textos)
    pasos(b, receta, textos)
    tecnica(b, receta, textos)
    compra(b, receta, raciones, textos)
    receta.pie?.takeIf { it.isNotBlank() }?.let { b.append("<p class=\"pie\">").append(rico(it)).append("</p>") }
    b.append("</body></html>")
    return b.toString()
}

private fun portada(
    b: StringBuilder,
    receta: Receta,
    raciones: Int,
    textos: TextosFicha,
) {
    b.append("<header><h1>").append(esc(receta.titulo)).append("</h1>")
    if (receta.descripcion.isNotBlank()) b.append("<p class=\"desc\">").append(esc(receta.descripcion)).append("</p>")
    b.append("<div class=\"datos\">")
    b.append("<div class=\"dato\"><b>").append(raciones).append("</b><span>")
    b.append(esc(textos.raciones(raciones).removePrefix("$raciones "))).append("</span></div>")
    receta.datos.filter { it.isNotEmpty() }.forEach {
        b.append("<div class=\"dato\"><b>").append(esc(it[0])).append("</b><span>")
        b.append(esc(it.getOrElse(1) { "" })).append("</span></div>")
    }
    b.append("</div></header>")
}

private fun ingredientes(
    b: StringBuilder,
    receta: Receta,
    raciones: Int,
    textos: TextosFicha,
) {
    if (receta.grupos.isEmpty()) return
    val f = Escalado.factor(raciones, receta.raciones)
    b.append("<section><h2>").append(esc(textos.ingredientes)).append("</h2>")
    receta.grupos.forEach { g ->
        b.append("<h3>").append(esc(g.n)).append("</h3><div class=\"ing\">")
        g.items.forEach {
            b
                .append(
                    "<b>",
                ).append(esc(Escalado.cantidad(it, f)))
                .append("</b><span>")
                .append(esc(it.n))
                .append("</span>")
        }
        b.append("</div>")
    }
    receta.notaEscala?.takeIf { it.isNotBlank() }?.let { b.append("<p class=\"nota\">").append(esc(it)).append("</p>") }
    b.append("</section>")
}

private fun tiempo(
    b: StringBuilder,
    receta: Receta,
    textos: TextosFicha,
) {
    val total = receta.totalEfectivo
    if (total <= 0 || receta.pasos.isEmpty()) return
    b.append("<section><h2>").append(esc(textos.tiempo)).append("</h2>")
    b.append("<div class=\"fila regla\"><div></div><div class=\"ticks\">")
    Gantt.marcas(total).forEach {
        b.append("<span style=\"left:").append(pct(it / total)).append("\">")
        b.append(esc(Escalado.minutos(it))).append("</span>")
    }
    b.append("</div><div></div></div>")
    receta.pasos.forEach { filaGantt(b, it, receta, total, textos) }
    b.append("<div class=\"leyenda\">")
    receta.carriles.forEach {
        b.append("<span><i style=\"background:").append(colorCss(it)).append("\"></i>")
        b.append(esc(it.nombre)).append("</span>")
    }
    b.append("</div><p class=\"meta\">").append(esc(textos.eje)).append("</p>")
    receta.lectura?.takeIf { it.isNotBlank() }?.let { b.append("<p class=\"lectura\">").append(esc(it)).append("</p>") }
    b.append("</section>")
}

private fun filaGantt(
    b: StringBuilder,
    paso: Paso,
    receta: Receta,
    total: Double,
    textos: TextosFicha,
) {
    val carril = receta.carril(paso.carril)
    val sub =
        listOfNotNull(
            carril?.nombre?.uppercase(),
            textos.rango(Escalado.minutos(paso.off), Escalado.minutos(paso.fin)),
            paso.f?.let { textos.fuego(it) } ?: textos.sinFuego,
            paso.tapa?.uppercase(),
        ).joinToString(" · ")
    val (izq, ancho) = Gantt.barra(paso, total, anchoMinimo = 0.01)
    b.append("<div class=\"fila\"><div class=\"et\"><b>").append(esc(paso.t)).append("</b><span>")
    b.append(esc(sub)).append("</span></div><div class=\"pista\"")
    b.append(" style=\"background-image:").append(rejilla(total)).append("\">")
    b
        .append("<div class=\"barra\" style=\"left:")
        .append(pct(izq))
        .append(";width:")
        .append(pct(ancho))
    b.append(";background:").append(colorCss(carril)).append("\"></div></div>")
    b.append("<div class=\"dur\">").append(esc(textos.duracion(Escalado.minutos(paso.min)))).append("</div></div>")
}

/** Rejilla cada 10 min como degradado repetido; si no cabe ninguna línea, sin rejilla. */
private fun rejilla(total: Double): String {
    if (total <= CADA_REJILLA_MIN) return "none"
    val paso = CADA_REJILLA_MIN / total * 100
    val pasoTxt = String.format(Locale.ROOT, "%.3f%%", paso)
    return "linear-gradient(to right,transparent calc(100% - 1px),#D6D5C6 0);background-size:$pasoTxt 100%"
}

private fun mep(
    b: StringBuilder,
    receta: Receta,
    textos: TextosFicha,
) {
    if (receta.mep.isEmpty()) return
    b.append("<section><h2>").append(esc(textos.mep)).append("</h2><div class=\"mep\">")
    receta.mep.forEach { bl ->
        b.append("<div><h3>").append(esc(bl.t)).append("</h3>")
        bl.porq?.takeIf { it.isNotBlank() }?.let { b.append("<p class=\"porq\">").append(esc(it)).append("</p>") }
        val tag = if (bl.orden) "ol" else "ul"
        b.append("<$tag>")
        bl.l.forEach { b.append("<li>").append(rico(it)).append("</li>") }
        b.append("</$tag></div>")
    }
    b.append("</div></section>")
}

private fun pasos(
    b: StringBuilder,
    receta: Receta,
    textos: TextosFicha,
) {
    if (receta.pasos.isEmpty()) return
    b.append("<section><h2>").append(esc(textos.pasos)).append("</h2><ol class=\"pasos\">")
    receta.pasos.forEachIndexed { i, p ->
        val carril = receta.carril(p.carril)?.nombre ?: p.carril
        val meta =
            listOfNotNull(
                carril.uppercase(),
                textos.duracion(Escalado.minutos(p.min)),
                p.f?.let { textos.fuego(it) } ?: textos.sinFuego,
                p.tapa?.uppercase(),
            ).joinToString(" · ")
        b
            .append("<li><i>")
            .append(i + 1)
            .append(".</i><b class=\"t\">")
            .append(esc(p.t))
            .append("</b>")
        b.append("<div class=\"meta\">").append(esc(meta)).append("</div>")
        (p.p ?: p.nota)?.takeIf { it.isNotBlank() }?.let { b.append("<p>").append(rico(it)).append("</p>") }
        p.meto?.takeIf { it.isNotBlank() }?.let {
            b
                .append("<p><b>")
                .append(esc(textos.meto))
                .append(":</b> ")
                .append(rico(it))
                .append("</p>")
        }
        p.saco?.takeIf { it.isNotBlank() }?.let {
            b
                .append("<p><b>")
                .append(esc(textos.saco))
                .append(":</b> ")
                .append(rico(it))
                .append("</p>")
        }
        p.hitosValidos.forEach {
            b
                .append(
                    "<div class=\"hito\">",
                ).append(esc(formatoHito(it.en)))
                .append(' ')
                .append(esc(it.t))
                .append("</div>")
        }
        b.append("</li>")
    }
    b.append("</ol></section>")
}

private fun tecnica(
    b: StringBuilder,
    receta: Receta,
    textos: TextosFicha,
) {
    if (receta.cortes.isEmpty() && receta.porque.isEmpty()) return
    b.append("<section><h2>").append(esc(textos.tecnica)).append("</h2>")
    receta.cortes.forEach { c ->
        b.append("<div class=\"corte\"><div>")
        c.svg?.takeIf { it.isNotBlank() }?.let { b.append(it) }
        b.append("</div><div><h3>").append(esc(c.t)).append("</h3>")
        c.p?.takeIf { it.isNotBlank() }?.let { b.append("<p>").append(rico(it)).append("</p>") }
        b.append("</div></div>")
    }
    val porque = receta.porque.filter { it.isNotEmpty() }
    if (porque.isNotEmpty()) {
        b.append("<dl class=\"porque\">")
        porque.forEach {
            b.append("<div><dt>").append(esc(it[0])).append("</dt>")
            it.getOrNull(1)?.let { d -> b.append("<dd>").append(rico(d)).append("</dd>") }
            b.append("</div>")
        }
        b.append("</dl>")
    }
    b.append("</section>")
}

private fun compra(
    b: StringBuilder,
    receta: Receta,
    raciones: Int,
    textos: TextosFicha,
) {
    b.append("<section><h2>").append(esc(textos.compra)).append("</h2>")
    if (receta.compra.isEmpty()) {
        b.append("<p>").append(esc(textos.compraVacia)).append("</p>")
    } else {
        b.append("<ul class=\"compra\">")
        lineasCompra(receta, raciones).forEach { b.append("<li>").append(esc(it)).append("</li>") }
        b.append("</ul>")
    }
    b.append("</section>")
}
