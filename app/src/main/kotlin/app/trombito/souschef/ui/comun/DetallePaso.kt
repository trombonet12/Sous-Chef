@file:Suppress("TooManyFunctions") // un solo componente: sus piezas privadas y la lógica pura que lo alimenta

package app.trombito.souschef.ui.comun

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.receta.Escalado
import app.trombito.souschef.receta.Holgura
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.TipoCarril
import app.trombito.souschef.ui.crono.minutosPaso
import app.trombito.souschef.ui.crono.recursoTapa
import app.trombito.souschef.ui.theme.CronoPalette
import app.trombito.souschef.ui.theme.FichaPalette
import app.trombito.souschef.ui.theme.colorCarrilTexto
import java.util.Locale

private const val MIN_DIA = 24 * 60
private const val SEG_MIN = 60
private val ANCHO_ETIQUETA = 76.dp
private val ANCHO_MIN_HORA_AVISO = 48.dp

/** Otro paso de la receta nombrado por su posición en el orden de reloj y su título («tras 2 (Pelar la patata)»). */
data class ReferenciaPaso(
    val numero: Int,
    val titulo: String,
)

/** Lo que el detalle necesita saber del paso dentro de su receta; se calcula con [contextoPaso]. */
data class ContextoPaso(
    /** Posición del paso en el orden de reloj (1 = el primero); 0 si no se conoce. */
    val numero: Int = 0,
    /** Posición dentro de su carril (1 = el primero); 0 si no se conoce. */
    val indiceCarril: Int = 0,
    val totalCarril: Int = 0,
    /** Potencia canónica del paso anterior del carril, para enseñar «4 → 5» solo si cambia. */
    val fuegoAnterior: Int? = null,
    val tras: List<ReferenciaPaso> = emptyList(),
    val juntoCon: ReferenciaPaso? = null,
    /** Minutos desde medianoche a los que arranca el plan (servir − total); null si no hay hora. */
    val inicioPlanMin: Int? = null,
)

/** Los pasos en orden de reloj (`off`); a igual `off`, el del array va antes. */
fun pasosPorReloj(receta: Receta): List<Paso> = receta.pasos.sortedBy { it.off }

/** El [ContextoPaso] de [paso] en [receta]; [inicioPlanMin] es la hora de arranque del plan si la hay. */
fun contextoPaso(
    receta: Receta,
    paso: Paso,
    inicioPlanMin: Int? = null,
): ContextoPaso {
    val porReloj = pasosPorReloj(receta)

    fun referencia(id: String): ReferenciaPaso? =
        porReloj.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { ReferenciaPaso(it + 1, porReloj[it].t) }
    val cola = receta.pasos.filter { it.carril == paso.carril }
    val i = cola.indexOfFirst { it.id == paso.id }
    return ContextoPaso(
        numero = porReloj.indexOfFirst { it.id == paso.id } + 1,
        indiceCarril = i + 1,
        totalCarril = if (i >= 0) cola.size else 0,
        fuegoAnterior = if (i > 0) cola[i - 1].f else null,
        tras = paso.dep.mapNotNull { referencia(it) },
        juntoCon = paso.junto?.let { referencia(it) },
        inicioPlanMin = inicioPlanMin,
    )
}

/** «HH:mm» del minuto [minutos] del plan, si se conoce la hora de arranque. */
fun horaDelPlan(
    inicioPlanMin: Int?,
    minutos: Double,
): String? {
    if (inicioPlanMin == null) return null
    val total = Math.floorMod(inicioPlanMin + Math.round(minutos).toInt(), MIN_DIA)
    return String.format(Locale.ROOT, "%02d:%02d", total / SEG_MIN, total % SEG_MIN)
}

/** «02:00» (o «1:02:00» desde la hora): la marca de un aviso dentro del paso. */
fun marcaAviso(segundos: Double): String {
    val total = Math.round(segundos).toInt()
    val h = total / (SEG_MIN * SEG_MIN)
    val m = total % (SEG_MIN * SEG_MIN) / SEG_MIN
    val s = total % SEG_MIN
    return if (h > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.ROOT, "%02d:%02d", m, s)
    }
}

/** Colores del detalle: la ficha y la cocina lo pintan sobre fondos distintos con el mismo componente. */
data class PaletaDetalle(
    val texto: Color,
    val apagado: Color,
    val acento: Color,
    val mete: Color,
    val saca: Color,
    val carril: Color,
)

/** Detalle sobre la carta del cronómetro; [color] es el del carril. */
@Composable
@ReadOnlyComposable
fun paletaDetalleCrono(color: Color): PaletaDetalle =
    PaletaDetalle(
        texto = CronoPalette.texto,
        apagado = CronoPalette.apagado,
        acento = CronoPalette.acento,
        mete = CronoPalette.meto,
        saca = CronoPalette.saco,
        carril = colorCarrilTexto(color),
    )

/** Detalle sobre el papel de la ficha. */
@Composable
@ReadOnlyComposable
fun paletaDetalleFicha(): PaletaDetalle =
    PaletaDetalle(
        texto = FichaPalette.tinta,
        apagado = FichaPalette.meta,
        acento = CronoPalette.acento,
        mete = CronoPalette.meto,
        saca = CronoPalette.saco,
        carril = FichaPalette.tinta,
    )

/**
 * El paso entero, el mismo en la ficha y en el cronómetro: cabecera, título, lo ejecutivo (METE, FUEGO, AVISOS,
 * SEÑAL, SACA y cómo se relaciona con otros pasos) y después el porqué. [conPorque] = false deja solo las órdenes.
 */
@Suppress("LongParameterList", "LongMethod")
@Composable
fun DetallePaso(
    paso: Paso,
    carrilNombre: String,
    tipoCarril: TipoCarril,
    contexto: ContextoPaso,
    paleta: PaletaDetalle,
    modifier: Modifier = Modifier,
    conPorque: Boolean = true,
    cuerpo: TextStyle = MaterialTheme.typography.bodyLarge,
    tecnicas: Map<String, String> = emptyMap(),
    onTecnica: (String) -> Unit = {},
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CabeceraDetalle(paso, carrilNombre, contexto, paleta)
        Text(
            buildAnnotatedString {
                if (contexto.numero > 0) {
                    withStyle(SpanStyle(color = paleta.apagado)) { append("${contexto.numero}  ") }
                }
                append(paso.t)
            },
            Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = paleta.texto,
        )
        paso.meto?.takeIf { it.isNotBlank() }?.let {
            LineaDetalle(R.string.detalle_mete, paleta.mete, cuerpo) { Text(textoRico(it), style = cuerpo) }
        }
        LineaDatos(paso, tipoCarril, contexto, paleta, cuerpo)
        Avisos(paso, paleta, cuerpo)
        paso.nota?.takeIf { it.isNotBlank() }?.let {
            LineaDetalle(R.string.detalle_senal, paleta.acento, cuerpo) { Text(textoRico(it), style = cuerpo) }
        }
        paso.saco?.takeIf { it.isNotBlank() }?.let {
            LineaDetalle(R.string.detalle_saca, paleta.saca, cuerpo) { Text(textoRico(it), style = cuerpo) }
        }
        Relacion(paso, contexto, paleta)
        if (conPorque) {
            paso.p?.takeIf { it.isNotBlank() }?.let { Porque(it, paleta, cuerpo) }
            tecnicas.forEach { (slug, titulo) ->
                TextButton(onClick = { onTecnica(slug) }) {
                    Text(stringResource(R.string.detalle_tecnica, titulo), color = paleta.acento)
                }
            }
        }
    }
}

@Composable
private fun CabeceraDetalle(
    paso: Paso,
    carrilNombre: String,
    contexto: ContextoPaso,
    paleta: PaletaDetalle,
) {
    val rango =
        stringResource(
            R.string.detalle_min_rango,
            Escalado.minutos(paso.off),
            Escalado.minutos(paso.off + paso.min),
        )
    val horas =
        horaDelPlan(contexto.inicioPlanMin, paso.off)?.let { desde ->
            horaDelPlan(contexto.inicioPlanMin, paso.off + paso.min)?.let { hasta ->
                stringResource(R.string.detalle_horas, desde, hasta)
            }
        }
    val resto =
        buildList {
            if (contexto.indiceCarril > 0) {
                add(stringResource(R.string.detalle_paso_de, contexto.indiceCarril, contexto.totalCarril))
            }
            add(listOfNotNull(rango, horas).joinToString(" "))
        }.joinToString(" · ")
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = paleta.carril, fontWeight = FontWeight.ExtraBold)) { append(carrilNombre) }
            append(" · $resto")
        },
        style = MaterialTheme.typography.labelLarge,
        color = paleta.apagado,
    )
}

/** «FUEGO 4 → 5 · sin tapa · 18 min»; sin fuego, «TIEMPO sin tapa · 18 min». */
@Composable
private fun LineaDatos(
    paso: Paso,
    tipo: TipoCarril,
    contexto: ContextoPaso,
    paleta: PaletaDetalle,
    cuerpo: TextStyle,
) {
    val cocina = LocalCocina.current
    val f = paso.f?.takeIf { tipo == TipoCarril.FUEGO }
    val anterior = contexto.fuegoAnterior?.takeIf { f != null && it != f }
    val fuego =
        f?.let {
            if (anterior == null) {
                cocina.nivel(it)
            } else {
                stringResource(R.string.detalle_fuego_cambia, cocina.nivel(anterior), cocina.nivel(it))
            }
        }
    val tapa = recursoTapa(paso.tapa)?.let { stringResource(it) }
    val datos = listOfNotNull(fuego, tapa, minutosPaso(paso)).joinToString(" · ")
    val etiqueta = if (fuego != null) R.string.detalle_fuego else R.string.detalle_tiempo
    LineaDetalle(etiqueta, paleta.texto, cuerpo) {
        Text(datos, style = cuerpo.merge(Tabular), color = paleta.texto)
    }
}

@Composable
private fun Avisos(
    paso: Paso,
    paleta: PaletaDetalle,
    cuerpo: TextStyle,
) {
    val hitos = paso.hitosValidos
    val sinAvisos = paso.sinHitos?.takeIf { hitos.isEmpty() && it.isNotBlank() }
    if (hitos.isEmpty() && sinAvisos == null) return
    LineaDetalle(R.string.detalle_avisos, paleta.acento, cuerpo) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            hitos.forEach {
                Row {
                    Text(
                        marcaAviso(it.en),
                        Modifier.widthIn(min = ANCHO_MIN_HORA_AVISO),
                        style = cuerpo.merge(Tabular),
                        color = paleta.apagado,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(it.t, Modifier.weight(1f), style = cuerpo, color = paleta.texto)
                }
            }
            sinAvisos?.let {
                Text(stringResource(R.string.detalle_sin_avisos, it), style = cuerpo, color = paleta.apagado)
            }
        }
    }
}

/** «tras 2 (Pelar la patata) · a la vez que 3 (…) · no espera», sangrada bajo las etiquetas. */
@Composable
private fun Relacion(
    paso: Paso,
    contexto: ContextoPaso,
    paleta: PaletaDetalle,
) {
    val tras = contexto.tras.map { stringResource(R.string.detalle_referencia, it.numero, it.titulo) }
    val junto = contexto.juntoCon?.let { stringResource(R.string.detalle_referencia, it.numero, it.titulo) }
    val partes =
        listOfNotNull(
            tras.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.detalle_tras, it.joinToString(", ")) },
            junto?.let { stringResource(R.string.detalle_a_la_vez, it) },
            when (paso.holguraTipo.takeIf { paso.holgura != null }) {
                Holgura.CRITICO -> stringResource(R.string.detalle_no_espera)
                Holgura.AJUSTADO -> stringResource(R.string.detalle_ajustado)
                else -> null
            },
        )
    if (partes.isEmpty()) return
    Text(
        partes.joinToString(" · "),
        Modifier.padding(start = ANCHO_ETIQUETA),
        style = MaterialTheme.typography.labelLarge,
        color = paleta.apagado,
    )
}

@Composable
private fun Porque(
    p: String,
    paleta: PaletaDetalle,
    cuerpo: TextStyle,
) {
    Text(
        stringResource(R.string.detalle_porque),
        Modifier.padding(top = 8.dp).semantics { heading() },
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.ExtraBold,
        color = paleta.apagado,
    )
    TextoBloques(p, style = cuerpo, color = paleta.texto)
}

/** Una fila «ETIQUETA  contenido»: la etiqueta ocupa siempre el mismo ancho para que el contenido se alinee. */
@Composable
private fun LineaDetalle(
    etiqueta: Int,
    colorEtiqueta: Color,
    cuerpo: TextStyle,
    contenido: @Composable () -> Unit,
) {
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        Text(
            stringResource(etiqueta),
            Modifier.width(ANCHO_ETIQUETA),
            style = cuerpo,
            fontWeight = FontWeight.ExtraBold,
            color = colorEtiqueta,
            maxLines = 1,
            softWrap = false,
        )
        Column(Modifier.weight(1f)) { contenido() }
    }
}
