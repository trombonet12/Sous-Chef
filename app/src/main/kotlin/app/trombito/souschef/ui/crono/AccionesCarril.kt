package app.trombito.souschef.ui.crono

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trombito.souschef.R
import app.trombito.souschef.ui.comun.LocalCocina
import app.trombito.souschef.ui.comun.nivel
import app.trombito.souschef.ui.comun.textoRico
import app.trombito.souschef.ui.crono.guia.GuiaIds
import app.trombito.souschef.ui.crono.guia.guia
import app.trombito.souschef.ui.theme.CronoPalette

/**
 * Lo que la UI puede pedir sobre un carril; todos los mandos reciben el id del carril. Es un saco de callbacks con
 * nombre (uno por mando), no una función con demasiados argumentos.
 */
@Suppress("LongParameterList")
class AccionesCarril(
    val hecho: (String) -> Unit,
    val alternar: (String) -> Unit,
    val masTreinta: (String) -> Unit,
    val empezarIgual: (String) -> Unit,
    val enfocar: (String) -> Unit,
    val porque: (String) -> Unit,
    /** Deshace el último Hecho del carril (toque sin querer). */
    val deshacer: (String) -> Unit = {},
) {
    companion object {
        /** Mandos sin efecto: la cocina de muestra de la intro no se toca. */
        val INERTES =
            AccionesCarril(
                hecho = {},
                alternar = {},
                masTreinta = {},
                empezarIgual = {},
                enfocar = {},
                porque = {},
            )
    }
}

/** Franja de color del carril (4 dp de ancho) que abre cada tarjeta o fila. */
@Composable
internal fun FranjaCarril(
    color: Color,
    alto: Dp,
    modifier: Modifier = Modifier,
) {
    Spacer(
        modifier
            .width(4.dp)
            .height(alto)
            .clip(RoundedCornerShape(2.dp))
            .background(color),
    )
}

/** Toque que abre «Ver el porqué» del carril [v]; no hace nada si el paso no tiene porqué. */
@Composable
internal fun Modifier.abrePorque(
    v: VistaCarril,
    acciones: AccionesCarril,
): Modifier {
    if (!v.tienePorque) return this
    val etiquetaPorque = stringResource(R.string.crono_ver_porque)
    return clickable(onClickLabel = etiquetaPorque) { acciones.porque(v.id) }
}

/** Ejecuta la acción principal del carril: empezar/seguir alternan; en marcha o vencido, "Hecho y siguiente". */
fun AccionesCarril.principal(v: VistaCarril) {
    when (v.accion) {
        AccionCarril.EMPEZAR, AccionCarril.SEGUIR -> alternar(v.id)
        AccionCarril.PAUSAR, AccionCarril.HECHO -> hecho(v.id)
        AccionCarril.BLOQUEADO, AccionCarril.TERMINADO -> Unit
    }
}

@Composable
internal fun etiquetaAccion(v: VistaCarril): String =
    when (v.accion) {
        AccionCarril.EMPEZAR -> stringResource(R.string.crono_btn_empezar)
        AccionCarril.SEGUIR -> stringResource(R.string.crono_btn_seguir)
        AccionCarril.PAUSAR, AccionCarril.HECHO -> stringResource(R.string.crono_btn_hecho)
        AccionCarril.BLOQUEADO -> stringResource(R.string.crono_antes_bloqueado, v.bloqueadoPor.orEmpty())
        AccionCarril.TERMINADO -> stringResource(R.string.crono_terminado)
    }

/** Botón de la acción principal; ≥ 48 dp. Deshabilitado si el carril espera a otro. */
@Composable
internal fun BotonAccion(
    v: VistaCarril,
    acciones: AccionesCarril,
    modifier: Modifier = Modifier,
    compacto: Boolean = false,
) {
    val bloqueado = v.accion == AccionCarril.BLOQUEADO
    val descripcion = descripcionAccion(v)
    val tipografia = MaterialTheme.typography
    // Solo el botón que dice «Hecho» es el punto de la guía.
    val diceHecho = v.accion == AccionCarril.PAUSAR || v.accion == AccionCarril.HECHO
    val puntoGuia = if (diceHecho) Modifier.guia(GuiaIds.HECHO) else Modifier
    Button(
        onClick = { acciones.principal(v) },
        enabled = !bloqueado,
        modifier =
            modifier
                .heightIn(min = 48.dp)
                .testTag("accion-${v.id}")
                .semantics { contentDescription = descripcion }
                .then(puntoGuia),
        contentPadding = PaddingValues(horizontal = if (compacto) 8.dp else 20.dp, vertical = 4.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = if (v.vencido) CronoPalette.urgente else CronoPalette.principal,
                contentColor = if (v.vencido) CronoPalette.onUrgente else CronoPalette.onPrincipal,
                disabledContainerColor = CronoPalette.carta2,
                disabledContentColor = CronoPalette.apagado,
            ),
    ) {
        Text(
            etiquetaAccion(v),
            style =
                when {
                    compacto -> tipografia.labelMedium
                    bloqueado -> tipografia.labelLarge
                    else -> tipografia.titleMedium
                },
            fontWeight = FontWeight.Bold,
            maxLines = if (bloqueado) 2 else 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun Chip(
    texto: String,
    color: Color,
    descripcion: String? = null,
) {
    Text(
        texto,
        Modifier
            .clip(RoundedCornerShape(50))
            .background(CronoPalette.carta2)
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .semantics { if (descripcion != null) contentDescription = descripcion },
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
    )
}

/** "🔥 5" (o "🔥 medio-alto") si el carril es de fuego y el paso fija potencia; en la cocina del usuario. */
@Composable
internal fun ChipFuego(v: VistaCarril) {
    val potencia = LocalCocina.current.nivel(v.fuego ?: return)
    Chip(
        stringResource(R.string.crono_fuego_chip, potencia),
        CronoPalette.acento,
        stringResource(R.string.ficha_fuego_bajo, potencia),
    )
}

/** "tapada / destapada / tapa a medias"; resaltada si la tapa está puesta o a medias. */
@Composable
internal fun ChipTapa(v: VistaCarril) {
    val recurso = recursoTapa(v.tapa) ?: return
    val resaltada = recurso != R.string.crono_destapada
    Chip(stringResource(recurso), if (resaltada) CronoPalette.tapa else CronoPalette.apagado)
}

@Composable
@ReadOnlyComposable
internal fun VistaCarril.colorReloj(): Color = if (quedanS < 0) CronoPalette.alerta else CronoPalette.texto

/** Línea "METO  …" o "SACO  …": etiqueta de color en negrita y texto con negritas ligeras. */
@Composable
internal fun LineaEtiquetada(
    rotulo: Int,
    color: Color,
    texto: String,
    modifier: Modifier = Modifier,
    maxLineas: Int = Int.MAX_VALUE,
) {
    val etiqueta = stringResource(rotulo)
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = color, fontWeight = FontWeight.ExtraBold)) { append("$etiqueta  ") }
            append(textoRico(texto))
        },
        modifier,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = maxLineas,
        overflow = TextOverflow.Ellipsis,
    )
}
