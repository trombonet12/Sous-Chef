package app.trombito.souschef.ui.crono

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trombito.souschef.R
import app.trombito.souschef.ui.comun.Tabular
import app.trombito.souschef.ui.theme.CronoPalette
import app.trombito.souschef.ui.theme.colorCarrilTexto

/**
 * Contenido de la tarjeta cuando falta alto (4 carriles o letra grande): arriba la cabecera (insignia, nombre, hora
 * prevista, fuego, tapa y «i») y el título del paso en su propia línea; en medio el reloj con su barra, los mandos
 * secundarios y el botón principal; debajo, lo que quepa ENTERO de la pila de textos por prioridad. Tocar la cabecera
 * abre el porqué. Nada se mueve: lo que no cabe en 2 líneas se corta con «…» y lo que no cabe en 1 se omite.
 */
@Composable
internal fun ContenidoCompacto(
    v: VistaCarril,
    color: Color,
    acciones: AccionesCarril,
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CabeceraCompacta(v, color, acciones)
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        formatoReloj(v.quedanS),
                        Modifier.weight(1f, fill = false),
                        color = v.colorReloj(),
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.displaySmall.merge(Tabular),
                        maxLines = 1,
                        autoSize = TextAutoSize.StepBased(minFontSize = 20.sp, maxFontSize = RELOJ_COMPACTO_MAX),
                    )
                    if (v.enPausa) {
                        RotuloPausa(Modifier.padding(bottom = 4.dp), MaterialTheme.typography.labelMedium)
                    }
                }
                BarraProgreso(v, if (v.quedanS < 0) CronoPalette.alerta else color)
            }
            MandosSecundarios(v, acciones, compacto = true)
            if (v.accion != AccionCarril.BLOQUEADO) BotonAccion(v, acciones, compacto = true)
        }
        PilaPrioritaria(bloquesCompacta(v), Modifier.weight(1f, fill = false))
    }
}

private val RELOJ_COMPACTO_MAX = 40.sp

@Composable
private fun CabeceraCompacta(
    v: VistaCarril,
    color: Color,
    acciones: AccionesCarril,
) {
    CabeceraPaso(
        insignia = { InsigniaCarril(v, color, tamano = 20.dp) },
        nombre = { NombreCarril(v, color, MaterialTheme.typography.labelLarge) },
        prevista = { HoraPrevista(v, estilo = MaterialTheme.typography.labelMedium) },
        extras = {
            ChipFuego(v)
            ChipTapa(v)
            if (v.tienePorque) BotonPorque(v, acciones, encabezado = true)
        },
        titulo = { TituloEnCabecera(v.titulo.orEmpty(), lineas = 2) },
        modifier = Modifier.fillMaxWidth().abrePorque(v, acciones),
    )
}

/** Nombre del carril en una sola línea: la letra baja antes de cortarse, porque el nombre no se trocea. */
@Composable
internal fun NombreCarril(
    v: VistaCarril,
    color: Color,
    estilo: TextStyle,
) {
    Text(
        v.nombre,
        color = colorCarrilTexto(color),
        style = estilo,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = estilo.fontSize),
    )
}

/** Título del paso en su propia línea (hasta [lineas]), con «…» si no cabe; [grande] lo pone a 16-22 sp. */
@Composable
internal fun TituloEnCabecera(
    titulo: String,
    lineas: Int,
    grande: Boolean = false,
) {
    Text(
        titulo,
        if (grande) Modifier.fillMaxWidth() else Modifier,
        style = if (grande) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        maxLines = lineas,
        overflow = TextOverflow.Ellipsis,
        autoSize = if (grande) TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = 22.sp) else null,
    )
}

/**
 * Bloques de pila de la tarjeta compacta (carril en marcha o vencido), por prioridad según el momento: en marcha,
 * el próximo aviso, SEÑAL, «a la vez que ②» y «Luego»; vencido, SACA, «Luego», «a la vez que ②» y SEÑAL.
 */
@Composable
private fun bloquesCompacta(v: VistaCarril): List<BloquePila> {
    val hito =
        v.proximoHito?.let { h -> textoProximoAviso(v, h) }
    val junto = textoJunto(v)
    val paso = bloquesDelMomento(v)
    val bloqueHito = hito?.let { BloquePila(AnnotatedString(it), CronoPalette.acento) }
    val bloqueJunto =
        junto?.let { BloquePila(AnnotatedString(it), CronoPalette.acento, descripcion = descripcionJunto(v)) }
    val luego = BloquePila(AnnotatedString(textoLuego(v)), CronoPalette.apagado)
    return if (v.vencido) {
        listOfNotNull(paso.saca, luego, bloqueJunto, paso.senal)
    } else {
        listOfNotNull(bloqueHito, paso.senal, bloqueJunto, luego)
    }
}

/** SEÑAL, METE, SACA y el fuego del paso como bloques de texto; null si el paso no los tiene. */
internal class BloquesPaso(
    val senal: BloquePila?,
    val mete: BloquePila?,
    val saca: BloquePila?,
    /** «fuego 4 → 5 · sin tapa · 18 min»: lo que va junto a METE antes de empezar. */
    val detalle: BloquePila?,
)

/** Las piezas de la orden del paso ([VistaCarril.pantalla]) como bloques; [estilo] es la letra (null = la base). */
@Composable
internal fun bloquesDelMomento(
    v: VistaCarril,
    estilo: TextStyle? = null,
): BloquesPaso {
    val p = v.pantalla
    val etiquetaSenal = stringResource(R.string.crono_senal)
    val etiquetaMete = stringResource(R.string.crono_mete)
    val etiquetaSaca = stringResource(R.string.crono_saca)
    val negrita = (estilo ?: MaterialTheme.typography.bodyMedium).copy(fontWeight = FontWeight.Bold)
    return BloquesPaso(
        senal =
            p.senal?.let {
                BloquePila(
                    etiquetada(etiquetaSenal, CronoPalette.acento, it),
                    CronoPalette.texto,
                    estilo,
                )
            },
        mete =
            p.mete?.let {
                BloquePila(
                    etiquetada(etiquetaMete, CronoPalette.meto, it),
                    CronoPalette.texto,
                    estilo,
                )
            },
        saca =
            p.saca?.let {
                BloquePila(
                    etiquetada(etiquetaSaca, CronoPalette.saco, it),
                    CronoPalette.texto,
                    estilo,
                )
            },
        detalle =
            p.detalle.takeIf { it.isNotEmpty() }?.let { BloquePila(AnnotatedString(it), CronoPalette.texto, negrita) },
    )
}

private fun etiquetada(
    etiqueta: String,
    color: Color,
    texto: String,
): AnnotatedString =
    buildAnnotatedString {
        withStyle(SpanStyle(color = color, fontWeight = FontWeight.ExtraBold)) { append("$etiqueta  ") }
        append(texto)
    }

/**
 * Tarjeta de un carril que aún no ha empezado (toca ya, espera su hora o espera a otro carril): no hay reloj que
 * enseñar, así que todo el sitio es para la orden: cabecera, METE, «fuego 4 → 5 · sin tapa · 18 min» y, si cabe, la
 * SEÑAL, en letra de encimera (18 sp); debajo, «Luego» y la acción. Nada se mueve: lo que no cabe se corta o se omite.
 */
@Composable
internal fun ContenidoPorEmpezar(
    v: VistaCarril,
    color: Color,
    acciones: AccionesCarril,
) {
    val estilo = MaterialTheme.typography.bodyLarge
    Column(
        Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CabeceraPaso(
            insignia = { InsigniaCarril(v, color, tamano = 24.dp) },
            nombre = { NombreCarril(v, color, MaterialTheme.typography.titleSmall) },
            prevista = { HoraPrevista(v) },
            extras = {
                Text(
                    stringResource(R.string.crono_paso_de, v.paso, v.pasos),
                    color = CronoPalette.apagado,
                    style = MaterialTheme.typography.labelLarge.merge(Tabular),
                    maxLines = 1,
                )
                if (v.tienePorque) BotonPorque(v, acciones, encabezado = true)
            },
            titulo = { TituloEnCabecera(v.pantalla.titulo, lineas = 2, grande = true) },
            modifier = Modifier.fillMaxWidth().abrePorque(v, acciones),
        )
        PilaPrioritaria(
            bloquesPorEmpezar(v, estilo),
            Modifier.weight(1f).fillMaxWidth(),
            estiloBase = estilo,
            separacion = 4.dp,
        )
        FilaPorEmpezar(v, acciones)
    }
}

/**
 * Los bloques de la tarjeta por empezar, por prioridad: METE, fuego/tapa/duración y SEÑAL; un carril que espera a otro
 * dice primero a cuál, y si toca ya «a la vez que ②» va tras la orden.
 */
@Composable
private fun bloquesPorEmpezar(
    v: VistaCarril,
    estilo: TextStyle,
): List<BloquePila> {
    val paso = bloquesDelMomento(v, estilo)
    val bloqueado = v.accion == AccionCarril.BLOQUEADO
    val antes = if (bloqueado) stringResource(R.string.crono_antes_bloqueado, v.bloqueadoPor.orEmpty()) else null
    val texto = listOfNotNull(antes, textoJunto(v)).joinToString(" · ").takeIf { it.isNotEmpty() }
    val junto =
        texto?.let {
            val descripcion = listOfNotNull(antes, descripcionJunto(v)).joinToString(" · ")
            BloquePila(
                AnnotatedString(it),
                CronoPalette.acento,
                estilo,
                descripcion,
                "accion-${v.id}".takeIf { bloqueado },
            )
        }
    return if (bloqueado) {
        listOfNotNull(junto, paso.mete, paso.detalle, paso.senal)
    } else {
        listOfNotNull(paso.mete, paso.detalle, paso.senal, junto)
    }
}

/** «Luego: …» a la izquierda (letra de mano) y la acción a la derecha: botón lleno solo si toca ya. */
@Composable
private fun FilaPorEmpezar(
    v: VistaCarril,
    acciones: AccionesCarril,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            textoLuego(v),
            Modifier.weight(1f),
            color = CronoPalette.apagado,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        MandosSecundarios(v, acciones, compacto = true)
        if (v.accion != AccionCarril.BLOQUEADO) BotonAccion(v, acciones, Modifier.widthIn(min = 120.dp, max = 200.dp))
    }
}
