package app.trombito.souschef.ui.crono

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.trombito.souschef.R
import app.trombito.souschef.ui.comun.Tabular
import app.trombito.souschef.ui.comun.textoRico
import app.trombito.souschef.ui.crono.guia.GuiaIds
import app.trombito.souschef.ui.crono.guia.guia
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
            .guia(GuiaIds.CARRIL)
            .clip(forma)
            .avisoVencido(v)
            .background(if (v.vencido) CronoPalette.parpadeo else CronoPalette.carta)
            .border(2.dp, if (v.vencido) CronoPalette.alerta else color, forma),
    ) {
        val escala = LocalDensity.current.fontScale
        val hayMetoSaco = !v.meto.isNullOrBlank() || !v.saco.isNullOrBlank()
        val hayNota = !v.nota.isNullOrBlank()
        if (tarjetaCompacta(maxHeight.value, escala, hayNota = hayNota, hayMetoSaco = hayMetoSaco)) {
            ContenidoCompacto(v, color, acciones, lineasExtraCompacta(maxHeight.value, escala))
            return@BoxWithConstraints
        }
        val plan = planTarjeta(maxHeight.value, carriles, hayMetoSaco, hayNota)
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
            FranjaCarril(color, 24.dp)
            InsigniaCarril(v, color)
            NombreYPrevista(
                Modifier.weight(1f, fill = false),
                nombre = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
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
                    }
                },
                prevista = { HoraPrevista(v) },
            )
        }
        MandosSecundarios(v, acciones)
    }
}

/**
 * Nombre del carril («OLLA · 2/5») con prioridad sobre la hora prevista: esta va a su derecha si cabe y, si no, debajo.
 * [prevista] puede no emitir nada (carril en marcha).
 */
@Composable
private fun NombreYPrevista(
    modifier: Modifier,
    nombre: @Composable () -> Unit,
    prevista: @Composable () -> Unit,
) {
    Layout(
        content = {
            nombre()
            prevista()
        },
        modifier = modifier,
    ) { medibles, constraints ->
        val separacion = SEPARACION_PREVISTA.roundToPx()
        val suelto = constraints.copy(minWidth = 0, minHeight = 0)
        val n = medibles[0].measure(suelto)
        val hueco = (constraints.maxWidth - n.width - separacion).coerceAtLeast(0)
        val p = medibles.getOrNull(1)?.measure(suelto)
        val debajo = p != null && p.width > hueco
        val ancho =
            when {
                p == null -> n.width
                debajo -> maxOf(n.width, p.width)
                else -> n.width + separacion + p.width
            }
        val alto =
            when {
                p == null -> n.height
                debajo -> n.height + p.height
                else -> maxOf(n.height, p.height)
            }
        layout(ancho.coerceAtMost(constraints.maxWidth), alto) {
            if (p != null && debajo) {
                n.placeRelative(0, 0)
                p.placeRelative(0, n.height)
            } else {
                n.placeRelative(0, (alto - n.height) / 2)
                p?.placeRelative(n.width + separacion, (alto - p.height) / 2)
            }
        }
    }
}

private val SEPARACION_PREVISTA = 8.dp

/** Pausa y +30 s (o el enlace «Empezar igual» si el carril espera a otro): secundarios, el principal está abajo. */
@Composable
internal fun MandosSecundarios(
    v: VistaCarril,
    acciones: AccionesCarril,
    compacto: Boolean = false,
) {
    when (v.accion) {
        AccionCarril.PAUSAR, AccionCarril.HECHO -> {
            BotonSecundario(
                stringResource(R.string.crono_pausa),
                stringResource(R.string.crono_a11y_pausa, v.nombre),
                compacto,
            ) { acciones.alternar(v.id) }
            BotonSecundario(
                stringResource(R.string.crono_mas_30),
                stringResource(R.string.crono_a11y_mas_30, v.nombre),
                compacto,
                Modifier.guia(GuiaIds.PRORROGA),
            ) { acciones.masTreinta(v.id) }
        }

        AccionCarril.SEGUIR -> {
            BotonSecundario(
                stringResource(R.string.crono_mas_30),
                stringResource(R.string.crono_a11y_mas_30, v.nombre),
                compacto,
                Modifier.guia(GuiaIds.PRORROGA),
            ) { acciones.masTreinta(v.id) }
        }

        AccionCarril.BLOQUEADO -> {
            EnlaceEmpezarIgual(v, acciones, compacto)
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
    IconButton(
        onClick = { acciones.deshacer(v.id) },
        Modifier.size(48.dp).testTag("deshacer-${v.id}").guia(GuiaIds.DESHACER),
    ) {
        Icon(
            Icono.Deshacer.vector,
            stringResource(R.string.crono_a11y_deshacer, v.nombre, v.deshacerA.orEmpty()),
            Modifier.size(20.dp),
            tint = CronoPalette.apagado,
        )
    }
}

/** «Empezar igual»: enlace de texto, no botón lleno; empieza aunque el paso del que depende no esté hecho. */
@Composable
private fun EnlaceEmpezarIgual(
    v: VistaCarril,
    acciones: AccionesCarril,
    compacto: Boolean,
) {
    val descripcion = stringResource(R.string.crono_a11y_empezar_igual, v.nombre)
    TextButton(
        onClick = { acciones.empezarIgual(v.id) },
        modifier =
            Modifier
                .heightIn(min = 48.dp)
                .padding(start = if (compacto) 4.dp else 6.dp)
                .semantics { contentDescription = descripcion },
        contentPadding = PaddingValues(horizontal = if (compacto) 8.dp else 10.dp, vertical = 2.dp),
    ) {
        Text(
            stringResource(R.string.crono_empezar_igual),
            color = CronoPalette.apagado,
            style = if (compacto) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
            textDecoration = TextDecoration.Underline,
            maxLines = 1,
        )
    }
}

@Composable
private fun BotonSecundario(
    texto: String,
    descripcion: String,
    compacto: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier =
            modifier
                .heightIn(min = 48.dp)
                .padding(start = if (compacto) 4.dp else 6.dp)
                .semantics { contentDescription = descripcion },
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
    Row(
        Modifier.fillMaxWidth().abrePorque(v, acciones),
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
            .clearAndSetSemantics {}
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

/**
 * A la izquierda, «a la vez que ②» (si el paso tiene `junto`) y «Después: …»; a la derecha, la acción principal: botón
 * lleno solo si toca ya o ha vencido, enlace «Empezar antes» si espera su hora y «Antes: …» si espera a otro carril.
 */
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
        // En BLOQ el «a la vez que» ya va en el texto «Antes: …» de la derecha.
        val junto = if (v.accion == AccionCarril.BLOQUEADO) null else textoJunto(v)
        val descripcionJunto = if (junto == null) null else descripcionJunto(v)
        Column(Modifier.weight(1f)) {
            if (junto != null) {
                Text(
                    junto,
                    Modifier.semantics { descripcionJunto?.let { contentDescription = it } },
                    color = CronoPalette.acento,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                textoDespuesLimpio(v),
                color = CronoPalette.apagado,
                style = MaterialTheme.typography.bodySmall,
                maxLines = if (junto != null) 1 else 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        BotonAccion(v, acciones, Modifier.widthIn(min = 120.dp, max = 220.dp))
    }
}
