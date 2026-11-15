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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
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

private val NOMBRE_MAX = 128.dp

/**
 * Contenido de la tarjeta cuando falta alto (4 carriles o letra grande): arriba número, nombre, paso, fuego y tapa;
 * en medio el reloj con su barra, los mandos secundarios y el botón principal; debajo, si caben, el próximo hito, qué
 * va después y la nota. Tocar la fila de arriba abre el porqué. Nada se mueve: el texto que no cabe se corta con «…».
 */
@Composable
internal fun ContenidoCompacto(
    v: VistaCarril,
    color: Color,
    acciones: AccionesCarril,
    lineasExtra: Int,
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
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
                    autoSize =
                        TextAutoSize.StepBased(
                            minFontSize = 20.sp,
                            maxFontSize =
                                if (lineasExtra >
                                    0
                                ) {
                                    34.sp
                                } else {
                                    44.sp
                                },
                        ),
                )
                BarraProgreso(v, if (v.quedanS < 0) CronoPalette.alerta else color)
            }
            MandosSecundarios(v, acciones, compacto = true)
            if (v.accion != AccionCarril.BLOQUEADO) BotonAccion(v, acciones, compacto = true)
        }
        LineasExtra(v, lineasExtra)
    }
}

@Composable
private fun CabeceraCompacta(
    v: VistaCarril,
    color: Color,
    acciones: AccionesCarril,
) {
    Row(
        Modifier.fillMaxWidth().abrePorque(v, acciones),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        InsigniaCarril(v, color, tamano = 20.dp)
        Text(
            v.nombre,
            Modifier.widthIn(max = NOMBRE_MAX),
            color = colorCarrilTexto(color),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            autoSize =
                TextAutoSize.StepBased(
                    minFontSize = 10.sp,
                    maxFontSize = MaterialTheme.typography.labelLarge.fontSize,
                ),
        )
        Text(
            v.titulo.orEmpty(),
            Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        HoraPrevista(v, estilo = MaterialTheme.typography.labelMedium)
        ChipFuego(v)
        // «destapada» es lo normal: en compacto solo se avisa de la tapa puesta o a medias.
        if (recursoTapa(v.tapa) != R.string.crono_destapada) ChipTapa(v)
        if (v.tienePorque) IconoPorque()
    }
}

/**
 * Hasta [cuantas] líneas de texto, por prioridad: la nota (señal), «a la vez que ②» (y «Antes: …» si el carril espera a
 * otro: en compacto no hay botón que lo diga), METO, SACO, el próximo aviso y «Después». Cada texto tiene hasta 2
 * líneas con «…» y ninguno se mueve.
 */
@Composable
private fun LineasExtra(
    v: VistaCarril,
    cuantas: Int,
) {
    if (cuantas <= 0) return
    val hito =
        v.proximoHito?.let { h ->
            val segundos = (h.fraccion * v.duracionS).roundToInt()
            stringResource(R.string.crono_proximo_hito, formatoReloj(segundos), h.texto)
        }
    val nota = v.nota?.takeIf { it.isNotBlank() }
    val meto = v.meto?.takeIf { it.isNotBlank() }
    val saco = v.saco?.takeIf { it.isNotBlank() }
    val antes =
        if (v.accion == AccionCarril.BLOQUEADO) {
            stringResource(R.string.crono_antes_bloqueado, v.bloqueadoPor.orEmpty())
        } else {
            null
        }
    val junto = listOfNotNull(antes, textoJunto(v)).joinToString(" · ").takeIf { it.isNotEmpty() }
    val juntoA11y = listOfNotNull(antes, descripcionJunto(v)).joinToString(" · ")
    val reparto =
        repartoLineasCompacta(
            presupuesto = cuantas,
            longNota = nota?.length ?: 0,
            longMeto = meto?.length ?: 0,
            longSaco = saco?.length ?: 0,
            hayHito = hito != null,
            hayJunto = junto != null,
        )
    val textos =
        listOf(
            Triple(
                etiquetada(stringResource(R.string.crono_senal), CronoPalette.acento, nota),
                CronoPalette.texto,
                reparto.nota,
            ),
            Triple(AnnotatedString(junto.orEmpty()), CronoPalette.acento, reparto.junto),
            Triple(
                etiquetada(stringResource(R.string.crono_meto), CronoPalette.meto, meto),
                CronoPalette.texto,
                reparto.meto,
            ),
            Triple(
                etiquetada(stringResource(R.string.crono_saco), CronoPalette.saco, saco),
                CronoPalette.texto,
                reparto.saco,
            ),
            Triple(AnnotatedString(hito.orEmpty()), CronoPalette.acento, reparto.hito),
            Triple(AnnotatedString(textoDespuesLimpio(v)), CronoPalette.apagado, reparto.despues),
        )
    textos.forEachIndexed { i, (texto, color, lineas) ->
        // La línea 1 es la de «a la vez que»: TalkBack lee «el carril 2» en vez del círculo.
        if (lineas > 0) LineaExtra(texto, color, lineas, if (i == INDICE_JUNTO) juntoA11y else null)
    }
}

private const val INDICE_JUNTO = 1

private fun etiquetada(
    etiqueta: String,
    color: Color,
    texto: String?,
): AnnotatedString =
    buildAnnotatedString {
        if (texto == null) return@buildAnnotatedString
        withStyle(SpanStyle(color = color, fontWeight = FontWeight.ExtraBold)) { append("$etiqueta  ") }
        append(textoRico(texto))
    }

@Composable
private fun LineaExtra(
    texto: AnnotatedString,
    color: Color,
    lineas: Int,
    descripcion: String? = null,
) {
    Text(
        texto,
        Modifier.fillMaxWidth().semantics { if (descripcion != null) contentDescription = descripcion },
        color = color,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = lineas,
        overflow = TextOverflow.Ellipsis,
    )
}
