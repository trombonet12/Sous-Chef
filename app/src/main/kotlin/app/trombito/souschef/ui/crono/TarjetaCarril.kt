package app.trombito.souschef.ui.crono

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trombito.souschef.R
import app.trombito.souschef.ui.comun.Tabular
import app.trombito.souschef.ui.comun.textoRico
import app.trombito.souschef.ui.shell.Icono
import app.trombito.souschef.ui.theme.CronoPalette
import app.trombito.souschef.ui.theme.LocalFonts
import app.trombito.souschef.ui.theme.colorCarrilTexto
import kotlin.math.roundToInt

/** Tarjeta completa de un carril: título, reloj con hitos, METO/SACO, nota y acción principal. Sin scroll. */
@Composable
fun TarjetaCarril(
    v: VistaCarril,
    acciones: AccionesCarril,
    carriles: Int,
    modifier: Modifier = Modifier,
) {
    val color = colorDeCarril(v.colorHex, v.indiceCarril)
    val forma = RoundedCornerShape(16.dp)
    BoxWithConstraints(
        modifier
            .testTag("tarjeta-${v.id}")
            .clip(forma)
            .background(if (v.vencido) CronoPalette.parpadeo else CronoPalette.carta)
            .border(2.dp, if (v.vencido) CronoPalette.alerta else color, forma),
    ) {
        val escala = LocalDensity.current.fontScale
        if (tarjetaCompacta(maxHeight.value, escala)) {
            ContenidoCompacto(v, color, acciones, lineasExtraCompacta(maxHeight.value, escala))
            return@BoxWithConstraints
        }
        val hayMetoSaco = !v.meto.isNullOrBlank() || !v.saco.isNullOrBlank()
        val plan = planTarjeta(maxHeight.value, carriles, hayMetoSaco, !v.nota.isNullOrBlank())
        Column(
            Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FilaTitulo(v, color, acciones)
            TituloPaso(v, plan.tituloLineas, acciones)
            BloqueReloj(v, color, plan.relojSp, Modifier.weight(1f))
            if (plan.metoSaco) LineasMetoSaco(v)
            if (plan.nota) NotaDelPaso(v)
            FilaFinal(v, acciones)
        }
    }
}

@Composable
private fun FilaTitulo(
    v: VistaCarril,
    color: Color,
    acciones: AccionesCarril,
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(
                Modifier
                    .width(4.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color),
            )
            InsigniaCarril(v, color)
            Text(
                v.nombre,
                Modifier.weight(1f, fill = false),
                color = colorCarrilTexto(color),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(R.string.crono_paso_de, v.paso, v.pasos),
                color = CronoPalette.apagado,
                style = MaterialTheme.typography.labelLarge.merge(Tabular),
                maxLines = 1,
            )
            HoraPrevista(v)
        }
        MandosSecundarios(v, acciones)
    }
}

/** Pausa y +30 s (o "Empezar igual" si el carril espera a otro): secundarios, el principal está abajo. */
@Composable
internal fun MandosSecundarios(
    v: VistaCarril,
    acciones: AccionesCarril,
    compacto: Boolean = false,
) {
    when (v.accion) {
        AccionCarril.PAUSAR, AccionCarril.HECHO -> {
            BotonSecundario(stringResource(R.string.crono_pausa), compacto) { acciones.alternar(v.id) }
            BotonSecundario(stringResource(R.string.crono_mas_30), compacto) { acciones.masTreinta(v.id) }
        }

        AccionCarril.SEGUIR -> {
            BotonSecundario(stringResource(R.string.crono_mas_30), compacto) { acciones.masTreinta(v.id) }
        }

        AccionCarril.BLOQUEADO -> {
            BotonSecundario(stringResource(R.string.crono_empezar_igual), compacto) { acciones.empezarIgual(v.id) }
        }

        AccionCarril.EMPEZAR, AccionCarril.TERMINADO -> {
            Unit
        }
    }
    BotonDeshacer(v, acciones)
}

/** Minibotón ↶: vuelve al paso anterior del carril si el último Hecho fue sin querer. Solo si hay algo que deshacer. */
@Composable
internal fun BotonDeshacer(
    v: VistaCarril,
    acciones: AccionesCarril,
) {
    if (!v.puedeDeshacer) return
    IconButton(onClick = { acciones.deshacer(v.id) }, Modifier.size(40.dp).testTag("deshacer-${v.id}")) {
        Icon(
            Icono.Deshacer.vector,
            stringResource(R.string.crono_deshacer_paso, v.deshacerA.orEmpty()),
            Modifier.size(20.dp),
            tint = CronoPalette.apagado,
        )
    }
}

@Composable
private fun BotonSecundario(
    texto: String,
    compacto: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.heightIn(min = 48.dp).padding(start = if (compacto) 4.dp else 6.dp),
        contentPadding = PaddingValues(horizontal = if (compacto) 8.dp else 10.dp, vertical = 2.dp),
        border = BorderStroke(1.dp, CronoPalette.linea),
    ) {
        Text(
            texto,
            color = CronoPalette.texto,
            style = if (compacto) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
            maxLines = 1,
        )
    }
}

/** Título del paso; al tocarlo se abre el porqué (si lo hay). */
@Composable
private fun TituloPaso(
    v: VistaCarril,
    lineas: Int,
    acciones: AccionesCarril,
) {
    val etiquetaPorque = stringResource(R.string.crono_ver_porque)
    val toque =
        if (v.tienePorque) {
            Modifier.clickable(onClickLabel = etiquetaPorque) { acciones.porque(v.id) }
        } else {
            Modifier
        }
    Row(
        Modifier.fillMaxWidth().then(toque),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            v.titulo.orEmpty(),
            Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = lineas,
            overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = 22.sp),
        )
        ChipFuego(v)
        ChipTapa(v)
        if (v.tienePorque) IconoPorque()
    }
}

/** Círculo con una "i": avisa de que el título abre el porqué del paso. */
@Composable
internal fun IconoPorque() {
    Text(
        "i",
        Modifier
            .clip(RoundedCornerShape(50))
            .background(CronoPalette.carta2)
            .width(22.dp)
            .padding(vertical = 1.dp),
        color = CronoPalette.acento,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
    )
}

/** «Después: …» a la izquierda y el botón grande de la acción principal a la derecha. */
@Composable
private fun FilaFinal(
    v: VistaCarril,
    acciones: AccionesCarril,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            textoDespues(v),
            Modifier.weight(1f),
            color = CronoPalette.apagado,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        BotonAccion(v, acciones, Modifier.widthIn(min = 120.dp, max = 220.dp))
    }
}

/** «Después: Reposo · fuego 8 · 10:00», o «Último paso». */
@Composable
internal fun textoDespues(v: VistaCarril): String {
    val despues = v.despues ?: return stringResource(R.string.crono_ultimo)
    return v.despuesS?.let { stringResource(R.string.crono_despues_tiempo, despues, formatoReloj(it)) }
        ?: stringResource(R.string.crono_despues_corto, despues)
}
