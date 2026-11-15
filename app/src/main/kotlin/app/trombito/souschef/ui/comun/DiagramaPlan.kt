@file:Suppress("TooManyFunctions") // un componente: sus piezas privadas y la lógica pura que lo alimenta

package app.trombito.souschef.ui.comun

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.Escalado
import app.trombito.souschef.receta.Gantt
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.TipoCarril
import app.trombito.souschef.receta.crono.ConsultasCrono
import app.trombito.souschef.ui.crono.recursoTapa
import app.trombito.souschef.ui.theme.CronoPalette
import app.trombito.souschef.ui.theme.FichaPalette
import app.trombito.souschef.ui.theme.contraste

/** Cuánto dibujo se da a cada carril: sin texto (lista), con nombre y eje (resumen) o con los pasos numerados. */
enum class DensidadPlan { HUELLA, RESUMEN, DETALLE }

/** Dónde está un paso en una cocinada: hecho (apagado), en marcha (lleno) o pendiente (de contorno). */
enum class SituacionPaso { HECHO, EN_MARCHA, PENDIENTE }

/** Estado vivo de la cocina para el diagrama: situación de cada paso (por id) y minuto del plan en que va la cocina. */
@Immutable
data class VivoPlan(
    val situacion: Map<String, SituacionPaso>,
    val ahoraMin: Double?,
)

/** Colores del diagrama; la ficha lo pinta en papel y la cocina en el tema del cronómetro. */
@Immutable
data class PaletaPlan(
    val tinta: Color,
    val meta: Color,
    val pista: Color,
    val rejilla: Color,
    val borde: Color,
    val ahora: Color,
)

/** Sobre la ficha (papel claro u oscuro). */
@Composable
@ReadOnlyComposable
fun paletaPlanFicha(): PaletaPlan {
    val f = FichaPalette
    return PaletaPlan(f.tinta, f.meta, f.papel2, f.linea, f.tinta2, f.tinta)
}

/** Sobre el cronómetro. */
@Composable
@ReadOnlyComposable
fun paletaPlanCrono(): PaletaPlan {
    val c = CronoPalette
    return PaletaPlan(c.texto, c.tenue, c.carta2, c.linea, c.fondo, c.texto)
}

/** Sobre una tarjeta de la lista (tema de la app). */
@Composable
@ReadOnlyComposable
fun paletaPlanLista(): PaletaPlan {
    val e = MaterialTheme.colorScheme
    return PaletaPlan(e.onSurface, e.onSurfaceVariant, e.outlineVariant, e.outlineVariant, e.outline, e.onSurface)
}

private const val CONTRASTE_BARRA = 3f
private const val PASOS_ACLARADO = 20
private const val LUMINANCIA_CLARA = 0.5f
private val CLARO_BARRA = Color(0xFFF4F1E8)

/**
 * Color de la barra de un carril sobre [fondo]: en fondo claro es el del carril; en fondo oscuro se aclara hasta 3:1
 * (elemento gráfico) para que los carriles oscuros (violeta, marrón) no se pierdan.
 */
fun colorBarraSobre(
    color: Color,
    fondo: Color,
): Color =
    if (fondo.luminance() >= LUMINANCIA_CLARA) {
        color
    } else {
        (0..PASOS_ACLARADO)
            .map { lerp(color, CLARO_BARRA, it / PASOS_ACLARADO.toFloat()) }
            .firstOrNull { contraste(it, fondo) >= CONTRASTE_BARRA } ?: CLARO_BARRA
    }

/** Color del carril en la ficha: el del diagrama, para chips y franjas que lo acompañan. */
@Composable
@ReadOnlyComposable
fun colorPlanFicha(
    carriles: List<Carril>,
    id: String,
): Color = colorBarraSobre(colorPlan(carriles, id), FichaPalette.papel2)

// ---------- Lógica pura ----------

/** Una fila del diagrama: un carril con sus pasos por orden de `off`. */
@Immutable
class FilaPlan(
    val carril: Carril,
    val rgb: Int,
    val pasos: List<Paso>,
) {
    val fuego: Boolean get() = carril.tipoCarril == TipoCarril.FUEGO
    val primerOff: Double get() = pasos.minOf { it.off }
    val color: Color get() = Color(0xFF000000.toInt() or rgb)
}

/**
 * Filas del diagrama: un carril por fila, ordenados por el `off` de su primer paso (a igual `off`, el de la receta).
 * El color sale de la posición del carril entre los normalizados del motor, como en la cocina.
 */
fun filasPlan(receta: Receta): List<FilaPlan> {
    val carriles = ConsultasCrono.carriles(receta)
    return carriles
        .map { c ->
            FilaPlan(c, rgbCarrilPlan(carriles, c.id), receta.pasos.filter { it.carril == c.id }.sortedBy { it.off })
        }.filter { it.pasos.isNotEmpty() }
        .sortedBy { it.primerOff }
}

/** Número de cada paso (por id): su posición por orden de `off`, el mismo que en la ficha y en la impresa. */
fun numerosPlan(receta: Receta): Map<String, Int> = pasosPorReloj(receta).mapIndexed { i, p -> p.id to i + 1 }.toMap()

private const val FUEGO_MAX = 9f

/** Altura relativa (0..1) del escalón de fuego de potencia canónica [f]. */
fun alturaFuego(f: Int): Float = f.coerceIn(0, FUEGO_MAX.toInt()) / FUEGO_MAX

/** Minutos hasta los que el diagrama cabe a lo ancho; más largo se desplaza. */
const val MAX_MIN_SIN_DESPLAZAR = 90

/** Medidas de una densidad. [perfil] es el alto del perfil de fuego bajo los carriles de fuego. */
private class Medidas(
    val barra: Dp,
    val arriba: Dp,
    val abajo: Dp,
    val perfil: Dp,
    val hueco: Dp,
    val minimo: Dp,
) {
    fun alto(fila: FilaPlan): Dp = arriba + barra + if (fila.fuego && perfil > 0.dp) perfil else abajo
}

private fun medidas(densidad: DensidadPlan): Medidas =
    when (densidad) {
        DensidadPlan.HUELLA -> Medidas(6.dp, 0.dp, 0.dp, 0.dp, 3.dp, 3.dp)
        DensidadPlan.RESUMEN -> Medidas(18.dp, 2.dp, 2.dp, 0.dp, 2.dp, 3.dp)
        DensidadPlan.DETALLE -> Medidas(28.dp, 4.dp, 4.dp, 12.dp, 0.dp, 18.dp)
    }

private fun tintaSobre(fondo: Color): Color =
    if (fondo.luminance() >
        LUMINANCIA_TINTA
    ) {
        Color(0xFF191A14)
    } else {
        Color.White
    }

private const val LUMINANCIA_TINTA = 0.4f
private const val ALFA_HECHO = 0.35f
private const val ALFA_PENDIENTE = 0.14f
private const val ALFA_PERFIL = 0.8f
private const val CADA_REJILLA_MIN = 10

@Immutable
private class ModeloPlan(
    val receta: Receta,
) {
    val filas: List<FilaPlan> = filasPlan(receta)
    val numeros: Map<String, Int> = numerosPlan(receta)
    val orden: List<Paso> = pasosPorReloj(receta)
}

@Immutable
private class Contexto(
    val densidad: DensidadPlan,
    val total: Double,
    val paleta: PaletaPlan,
    val vivo: VivoPlan?,
    val numeros: Map<String, Int>,
    val estiloNumero: TextStyle,
) {
    val medidas = medidas(densidad)
}

// ---------- Componente ----------

/**
 * El diagrama de tiempos: un carril por fila, con la misma forma en la lista ([DensidadPlan.HUELLA]), el Resumen de la
 * ficha ([DensidadPlan.RESUMEN]) y el detalle de la ficha y de la cocina ([DensidadPlan.DETALLE], con cada barra
 * numerada, el perfil de fuego y la lista de pasos debajo). Cabe a lo ancho hasta 90 minutos; solo entonces
 * se desplaza.
 * Con [inicioPlanMin] (hora de empezar, minutos desde medianoche) el eje lleva la hora real; con [vivo] pinta el avance
 * de la cocina (hecho apagado, en marcha lleno, pendiente de contorno y la línea «ahora»).
 */
@Suppress("LongParameterList") // la receta, la densidad y las cuatro opciones de pintado
@Composable
fun DiagramaPlan(
    receta: Receta,
    densidad: DensidadPlan,
    modifier: Modifier = Modifier,
    paleta: PaletaPlan = paletaPlanFicha(),
    inicioPlanMin: Int? = null,
    vivo: VivoPlan? = null,
) {
    val total = receta.totalEfectivo
    if (total <= 0 || receta.pasos.isEmpty()) return
    val modelo = remember(receta) { ModeloPlan(receta) }
    val estiloNumero =
        MaterialTheme.typography.labelMedium
            .merge(Tabular)
            .copy(fontWeight = FontWeight.Bold)
    val ctx = Contexto(densidad, total, paleta, vivo, modelo.numeros, estiloNumero)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        CuerpoPlan(modelo, ctx, inicioPlanMin)
        if (densidad != DensidadPlan.HUELLA) {
            Text(stringResource(R.string.ficha_eje), style = MaterialTheme.typography.labelSmall, color = paleta.meta)
        }
        if (densidad == DensidadPlan.DETALLE) ListaPasosPlan(modelo, ctx)
    }
}

@Composable
private fun CuerpoPlan(
    modelo: ModeloPlan,
    ctx: Contexto,
    inicioPlanMin: Int?,
) {
    val rotulos = ctx.densidad != DensidadPlan.HUELLA
    val tipografia = MaterialTheme.typography
    val estilo = if (ctx.densidad == DensidadPlan.DETALLE) tipografia.labelMedium else tipografia.labelSmall
    val medidor = rememberTextMeasurer()
    val pantalla = LocalDensity.current
    val estiloEje = tipografia.labelSmall.merge(Tabular)
    val alturaLinea = medidor.measure("0", estiloEje).size.height
    val lineaEje = with(pantalla) { alturaLinea.toDp() }
    val altoEje = if (rotulos) lineaEje * (if (inicioPlanMin != null) 2 else 1) + 2.dp else 0.dp
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val separacion = if (rotulos) SEPARACION_ROTULO else 0.dp
        val anchoRotulo = if (rotulos) anchoRotulos(modelo, medidor, estilo, maxWidth) else 0.dp
        val disponible = maxWidth - anchoRotulo - separacion
        val desplaza = ctx.total > MAX_MIN_SIN_DESPLAZAR
        val anchoPistas = if (desplaza) disponible * (ctx.total / MAX_MIN_SIN_DESPLAZAR).toFloat() else disponible
        Row(Modifier.fillMaxWidth()) {
            if (rotulos) {
                Column(Modifier.width(anchoRotulo), verticalArrangement = Arrangement.spacedBy(ctx.medidas.hueco)) {
                    Spacer(Modifier.height(altoEje))
                    modelo.filas.forEach { Rotulo(it, ctx, estilo) }
                }
                Spacer(Modifier.width(separacion))
            }
            val scroll = if (desplaza) Modifier.horizontalScroll(rememberScrollState()) else Modifier
            Box(Modifier.weight(1f).then(scroll)) {
                Column(
                    if (desplaza) Modifier.width(anchoPistas) else Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(ctx.medidas.hueco),
                ) {
                    if (rotulos) EjePlan(ctx, inicioPlanMin, altoEje)
                    modelo.filas.forEach { PistaPlan(it, ctx) }
                }
            }
        }
    }
}

/** Ancho de la columna de nombres: el del más largo entero, con un tope para que quede sitio al dibujo. */
@Composable
private fun anchoRotulos(
    modelo: ModeloPlan,
    medidor: TextMeasurer,
    estilo: TextStyle,
    maximo: Dp,
): Dp {
    val texto = modelo.filas.maxOf { medidor.measure(it.carril.nombre, estilo).size.width }
    return minOf(with(LocalDensity.current) { texto.toDp() } + MARGEN_ROTULO, maximo * TOPE_ROTULO)
}

private val SEPARACION_ROTULO = 8.dp
private val MARGEN_ROTULO = 4.dp
private const val TOPE_ROTULO = 0.42f

@Composable
private fun Rotulo(
    fila: FilaPlan,
    ctx: Contexto,
    estilo: TextStyle,
) {
    Box(Modifier.fillMaxWidth().height(ctx.medidas.alto(fila))) {
        Box(
            Modifier.padding(top = ctx.medidas.arriba).height(ctx.medidas.barra).fillMaxWidth(),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                fila.carril.nombre,
                Modifier.clearAndSetSemantics {},
                style = estilo,
                color = ctx.paleta.tinta,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Marcas del eje en minutos y, si se conoce la hora de empezar, la hora real debajo de cada una. */
@Composable
private fun EjePlan(
    ctx: Contexto,
    inicioPlanMin: Int?,
    alto: Dp,
) {
    val medidor = rememberTextMeasurer()
    val marcas = remember(ctx.total) { Gantt.marcas(ctx.total) }
    val minutos =
        MaterialTheme.typography.labelSmall
            .merge(Tabular)
            .copy(color = ctx.paleta.meta)
    val horas = minutos
    Canvas(Modifier.fillMaxWidth().height(alto).clearAndSetSemantics {}) {
        // De derecha a izquierda: la última marca (servir) siempre sale; las anteriores que la pisarían se omiten.
        var limite = size.width + HUECO_MARCAS.toPx()
        marcas.indices.reversed().forEach { i ->
            val minuto = medidor.measure(Escalado.minutos(marcas[i]), minutos)
            val hora = horaDelPlan(inicioPlanMin, marcas[i])?.let { medidor.measure(it, horas) }
            val ancho = maxOf(minuto.size.width, hora?.size?.width ?: 0)
            val x = (marcas[i] / ctx.total * size.width).toFloat()
            val izq = centrar(x, ancho, size.width)
            if (i == marcas.lastIndex || izq + ancho + HUECO_MARCAS.toPx() <= limite) {
                drawText(minuto, topLeft = Offset(centrar(x, minuto.size.width, size.width), 0f))
                hora?.let {
                    drawText(it, topLeft = Offset(centrar(x, it.size.width, size.width), minuto.size.height.toFloat()))
                }
                limite = izq
            }
        }
    }
}

private val HUECO_MARCAS = 2.dp

private fun centrar(
    x: Float,
    ancho: Int,
    total: Float,
): Float = (x - ancho / 2f).coerceIn(0f, (total - ancho).coerceAtLeast(0f))

@Composable
private fun PistaPlan(
    fila: FilaPlan,
    ctx: Contexto,
) {
    val medidor = rememberTextMeasurer()
    val fondo = ctx.paleta.pista
    val color = remember(fila, fondo) { colorBarraSobre(fila.color, fondo) }
    val descripcion =
        if (ctx.densidad == DensidadPlan.HUELLA) {
            null
        } else {
            stringResource(
                R.string.ficha_carril_resumen,
                fila.carril.nombre,
                Escalado.minutos(fila.primerOff),
                Escalado.minutos(fila.pasos.maxOf { it.fin }),
            )
        }
    val semantica =
        if (descripcion ==
            null
        ) {
            Modifier.clearAndSetSemantics {}
        } else {
            Modifier.semantics { contentDescription = descripcion }
        }
    Canvas(Modifier.fillMaxWidth().height(ctx.medidas.alto(fila)).then(semantica)) {
        pintarPista(fila, color, ctx, medidor)
    }
}

private fun DrawScope.pintarPista(
    fila: FilaPlan,
    color: Color,
    ctx: Contexto,
    medidor: TextMeasurer,
) {
    val total = ctx.total
    val m = ctx.medidas
    val radio = CornerRadius(minOf(m.barra.toPx() / 2, 3.dp.toPx()))
    val huella = ctx.densidad == DensidadPlan.HUELLA
    drawRoundRect(
        ctx.paleta.pista,
        Offset(0f, m.arriba.toPx()),
        Size(size.width, m.barra.toPx()),
        radio,
        alpha = if (huella) 0.5f else 1f,
    )
    if (!huella) {
        generateSequence(CADA_REJILLA_MIN.toDouble()) { it + CADA_REJILLA_MIN }.takeWhile { it < total }.forEach {
            val x = (it / total * size.width).toFloat()
            drawLine(
                ctx.paleta.rejilla,
                Offset(x, m.arriba.toPx()),
                Offset(x, (m.arriba + m.barra).toPx()),
                1.dp.toPx(),
            )
        }
    }
    posicionesBarras(fila, ctx).forEach { pintarBarra(it, color, ctx, radio, medidor) }
    if (ctx.densidad == DensidadPlan.DETALLE && fila.fuego) pintarPerfil(fila, color, ctx)
    ctx.vivo?.ahoraMin?.takeIf { it in 0.0..total }?.let {
        val x = (it / total * size.width).toFloat()
        drawLine(ctx.paleta.ahora, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2.dp.toPx())
    }
}

private fun DrawScope.pintarBarra(
    barra: BarraPlan,
    color: Color,
    ctx: Contexto,
    radio: CornerRadius,
    medidor: TextMeasurer,
) {
    val paso = barra.paso
    val m = ctx.medidas
    val origen = Offset(barra.x, m.arriba.toPx())
    val tamano = Size(barra.ancho, m.barra.toPx())
    val situacion = ctx.vivo?.situacion?.get(paso.id) ?: SituacionPaso.EN_MARCHA
    val detallada = ctx.densidad != DensidadPlan.HUELLA
    when (situacion) {
        SituacionPaso.HECHO -> {
            drawRoundRect(color, origen, tamano, radio, alpha = ALFA_HECHO)
        }

        SituacionPaso.EN_MARCHA -> {
            drawRoundRect(color, origen, tamano, radio)
            if (detallada) {
                drawRoundRect(
                    ctx.paleta.borde,
                    origen,
                    tamano,
                    radio,
                    style = Stroke(1.dp.toPx()),
                    alpha = 0.6f,
                )
            }
            if (ctx.vivo != null) drawRoundRect(ctx.paleta.tinta, origen, tamano, radio, style = Stroke(2.dp.toPx()))
        }

        SituacionPaso.PENDIENTE -> {
            drawRoundRect(color, origen, tamano, radio, alpha = ALFA_PENDIENTE)
            drawRoundRect(color, origen, tamano, radio, style = Stroke(1.5.dp.toPx()))
        }
    }
    if (ctx.densidad == DensidadPlan.DETALLE) {
        val texto = medidor.measure((ctx.numeros[paso.id] ?: 0).toString(), ctx.estiloNumero)
        val tinta =
            when (situacion) {
                SituacionPaso.HECHO -> ctx.paleta.meta
                SituacionPaso.PENDIENTE -> ctx.paleta.tinta
                SituacionPaso.EN_MARCHA -> tintaSobre(color)
            }
        val x = origen.x + (tamano.width - texto.size.width) / 2
        val y = origen.y + (tamano.height - texto.size.height) / 2
        drawText(texto, color = tinta, topLeft = Offset(x, y))
    }
}

/** Escalones de potencia bajo la barra: la altura de cada uno es `f / 9`. */
private fun DrawScope.pintarPerfil(
    fila: FilaPlan,
    color: Color,
    ctx: Contexto,
) {
    val total = ctx.total
    val m = ctx.medidas
    val base = (m.arriba + m.barra + m.perfil).toPx()
    drawLine(ctx.paleta.rejilla, Offset(0f, base), Offset(size.width, base), 1.dp.toPx())
    fila.pasos.forEach { paso ->
        val f = paso.f ?: return@forEach
        val (izq, ancho) = Gantt.barra(paso, total)
        val alto = (m.perfil.toPx() - 1.dp.toPx()) * alturaFuego(f).coerceAtLeast(MIN_ESCALON)
        val hecho = ctx.vivo?.situacion?.get(paso.id) == SituacionPaso.HECHO
        drawRect(
            color,
            Offset(izq * size.width, base - 1.dp.toPx() - alto),
            Size(ancho * size.width, alto),
            alpha = if (hecho) ALFA_HECHO else ALFA_PERFIL,
        )
    }
}

private const val MIN_ESCALON = 0.12f

// ---------- Lista numerada (detalle) ----------

@Composable
private fun ListaPasosPlan(
    modelo: ModeloPlan,
    ctx: Contexto,
) {
    val cocina = LocalCocina.current
    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        modelo.orden.forEach { paso ->
            val fila = modelo.filas.first { it.carril.id == paso.carril }
            val situacion = ctx.vivo?.situacion?.get(paso.id)
            val estado =
                when (situacion) {
                    SituacionPaso.HECHO -> stringResource(R.string.plan_paso_hecho)
                    SituacionPaso.EN_MARCHA -> stringResource(R.string.plan_paso_en_marcha)
                    else -> null
                }
            val apagado = if (situacion == SituacionPaso.HECHO) ALFA_ITEM_HECHO else 1f
            Row(
                Modifier
                    .fillMaxWidth()
                    .alpha(apagado)
                    .semantics(mergeDescendants = true) { estado?.let { stateDescription = it } },
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                InsigniaPlan(modelo.numeros.getValue(paso.id), colorBarraSobre(fila.color, ctx.paleta.pista))
                Column(Modifier.weight(1f)) {
                    Text(paso.t, style = MaterialTheme.typography.bodyMedium, color = ctx.paleta.tinta)
                    Text(
                        sublineaPlan(paso, fila.carril, modelo, cocina),
                        style = MaterialTheme.typography.bodySmall,
                        color = ctx.paleta.meta,
                    )
                }
            }
        }
    }
}

private const val ALFA_ITEM_HECHO = 0.6f
private const val NBSP = ' '

@Composable
private fun InsigniaPlan(
    numero: Int,
    color: Color,
) {
    Box(Modifier.size(26.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
        Text(
            numero.toString(),
            style =
                MaterialTheme.typography.labelMedium
                    .merge(Tabular)
                    .copy(fontWeight = FontWeight.Bold),
            color = tintaSobre(color),
        )
    }
}

/** «SARTÉN GRANDE · min 6-26 · fuego 4 · sin tapa · tras 2 · a la vez que 3». */
@Composable
private fun sublineaPlan(
    paso: Paso,
    carril: Carril,
    modelo: ModeloPlan,
    cocina: Cocina,
): String {
    val rango = stringResource(R.string.ficha_min_rango, Escalado.minutos(paso.off), Escalado.minutos(paso.fin))
    val fuego =
        paso.f?.let { stringResource(R.string.ficha_fuego_bajo, cocina.nivel(it)) }
            ?: stringResource(R.string.ficha_sin_fuego_bajo)
    val tapa = recursoTapa(paso.tapa)?.let { stringResource(it) } ?: paso.tapa?.takeIf { it.isNotBlank() }?.lowercase()
    val tras = paso.dep.mapNotNull { modelo.numeros[it] }.takeIf { it.isNotEmpty() }
    val junto = paso.junto?.let { modelo.numeros[it] }
    return listOfNotNull(
        carril.nombre,
        rango,
        fuego,
        tapa,
        tras?.let { stringResource(R.string.detalle_tras, it.joinToString(", ")) },
        junto?.let { stringResource(R.string.detalle_a_la_vez, it.toString()) },
    ).joinToString(" · ") { it.replace(' ', NBSP) }
}

/** Un paso con la posición y el ancho (en píxeles) de su barra. */
private data class BarraPlan(
    val paso: Paso,
    val x: Float,
    val ancho: Float,
)

/**
 * Posición de cada barra de [fila]. Todas miden al menos lo que cabe su número; en el detalle, si una barra
 * mínima pisaría a la anterior se corre a su derecha y, si así se saliera por el final, se recorta primero el final de
 * las barras anchas que la preceden y solo después se corren a la izquierda, para que cada número se lea.
 */
private fun DrawScope.posicionesBarras(
    fila: FilaPlan,
    ctx: Contexto,
): List<BarraPlan> {
    val minPx = ctx.medidas.minimo.toPx()
    val barras =
        fila.pasos.map { paso ->
            val (izq, ancho) = Gantt.barra(paso, ctx.total, anchoMinimo = (minPx / size.width).toDouble())
            BarraPlan(paso, izq * size.width, ancho * size.width)
        }
    if (ctx.densidad != DensidadPlan.DETALLE) return barras
    val separacion = 1.dp.toPx()
    val xs = barras.map { it.x }.toFloatArray()
    val ws = barras.map { it.ancho }.toFloatArray()
    for (i in barras.indices) {
        if (i > 0) xs[i] = maxOf(xs[i], xs[i - 1] + ws[i - 1] + separacion)
        if (xs[i] + ws[i] > size.width) xs[i] = (size.width - ws[i]).coerceAtLeast(0f)
    }
    for (i in barras.lastIndex - 1 downTo 0) {
        val sobra = xs[i] + ws[i] + separacion - xs[i + 1]
        if (sobra > 0) {
            val recorte = minOf(sobra, (ws[i] - minPx).coerceAtLeast(0f))
            ws[i] -= recorte
            xs[i] = (xs[i] - (sobra - recorte)).coerceAtLeast(0f)
        }
    }
    return barras.mapIndexed { i, b -> b.copy(x = xs[i], ancho = ws[i]) }
}
