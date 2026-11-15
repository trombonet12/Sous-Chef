package app.trombito.souschef.ui.crono

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import app.trombito.souschef.ui.comun.textoRico
import app.trombito.souschef.ui.theme.CronoPalette
import app.trombito.souschef.ui.theme.colorCarrilTexto
import kotlin.math.roundToInt

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
                Text(
                    formatoReloj(v.quedanS),
                    color = v.colorReloj(),
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.displaySmall.merge(Tabular),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 20.sp, maxFontSize = RELOJ_COMPACTO_MAX),
                )
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
            if (v.tienePorque) IconoPorque()
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

/** Título del paso en su propia línea (hasta [lineas]), con «…» si no cabe. */
@Composable
internal fun TituloEnCabecera(
    titulo: String,
    lineas: Int,
) {
    Text(
        titulo,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        maxLines = lineas,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * Bloques de la tarjeta compacta, por prioridad: la nota (señal), «a la vez que ②» (y «Antes: …» si el carril espera a
 * otro: en compacto no hay botón que lo diga), METO, SACO, el próximo aviso y «Después».
 */
@Composable
private fun bloquesCompacta(v: VistaCarril): List<BloquePila> {
    val hito =
        v.proximoHito?.let { h ->
            val segundos = (h.fraccion * v.duracionS).roundToInt()
            stringResource(R.string.crono_proximo_hito, formatoReloj(segundos), h.texto)
        }
    val antes =
        if (v.accion == AccionCarril.BLOQUEADO) {
            stringResource(R.string.crono_antes_bloqueado, v.bloqueadoPor.orEmpty())
        } else {
            null
        }
    val junto = listOfNotNull(antes, textoJunto(v)).joinToString(" · ").takeIf { it.isNotEmpty() }
    val juntoA11y = listOfNotNull(antes, descripcionJunto(v)).joinToString(" · ")
    val paso = bloquesDelPaso(v)
    return listOfNotNull(
        paso.nota,
        junto?.let { BloquePila(AnnotatedString(it), CronoPalette.acento, descripcion = juntoA11y) },
        paso.meto,
        paso.saco,
        hito?.let { BloquePila(AnnotatedString(it), CronoPalette.acento) },
        BloquePila(AnnotatedString(textoDespuesLimpio(v)), CronoPalette.apagado),
    )
}

/** SEÑAL (la nota), METO y SACO del paso como bloques de texto; null si el paso no los tiene. */
internal class BloquesPaso(
    val nota: BloquePila?,
    val meto: BloquePila?,
    val saco: BloquePila?,
) {
    /** En orden de prioridad: SEÑAL, METO, SACO. */
    fun lista(): List<BloquePila> = listOfNotNull(nota, meto, saco)
}

@Composable
internal fun bloquesDelPaso(
    v: VistaCarril,
    estiloNota: TextStyle? = null,
): BloquesPaso {
    val nota = v.nota?.takeIf { it.isNotBlank() }
    val meto = v.meto?.takeIf { it.isNotBlank() }
    val saco = v.saco?.takeIf { it.isNotBlank() }
    val senal = stringResource(R.string.crono_senal)
    val etiquetaMeto = stringResource(R.string.crono_meto)
    val etiquetaSaco = stringResource(R.string.crono_saco)
    return BloquesPaso(
        nota = nota?.let { BloquePila(etiquetada(senal, CronoPalette.acento, it), CronoPalette.texto, estiloNota) },
        meto = meto?.let { BloquePila(etiquetada(etiquetaMeto, CronoPalette.meto, it), CronoPalette.texto) },
        saco = saco?.let { BloquePila(etiquetada(etiquetaSaco, CronoPalette.saco, it), CronoPalette.texto) },
    )
}

private fun etiquetada(
    etiqueta: String,
    color: Color,
    texto: String,
): AnnotatedString =
    buildAnnotatedString {
        withStyle(SpanStyle(color = color, fontWeight = FontWeight.ExtraBold)) { append("$etiqueta  ") }
        append(textoRico(texto))
    }
