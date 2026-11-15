@file:Suppress("TooManyFunctions") // cabecera, mandos, «i» y fila final de la tarjeta

package app.trombito.souschef.ui.crono

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
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
import app.trombito.souschef.receta.crono.ClaseEstado
import app.trombito.souschef.ui.comun.Tabular
import app.trombito.souschef.ui.comun.textoRico
import app.trombito.souschef.ui.crono.guia.GuiaIds
import app.trombito.souschef.ui.crono.guia.guia
import app.trombito.souschef.ui.shell.Icono
import app.trombito.souschef.ui.theme.CronoPalette
import app.trombito.souschef.ui.theme.LocalFonts
import app.trombito.souschef.ui.theme.colorCarrilTexto
import kotlin.math.roundToInt

/**
 * Tarjeta de un carril; enseña lo que toca en este momento (ADR-0017): antes de empezar, METE, fuego, tapa y duración
 * ([ContenidoPorEmpezar]); en marcha, el reloj, el próximo aviso y la SEÑAL; vencido, SACA y «Luego». Sin scroll.
 */
@Composable
fun TarjetaCarril(
    v: VistaCarril,
    acciones: AccionesCarril,
    carriles: Int,
    modifier: Modifier = Modifier,
) {
    val color = colorDeCarril(v.colorHex, v.indiceCarril)
    val forma = RoundedCornerShape(16.dp)
    // Borde grueso del color del carril cuando toca empezar ya: es la tarjeta a la que mirar.
    val tocaYa = v.accion == AccionCarril.EMPEZAR && v.clase == ClaseEstado.YA
    BoxWithConstraints(
        modifier
            .testTag("tarjeta-${v.id}")
            .guia(GuiaIds.CARRIL)
            .clip(forma)
            .avisoVencido(v)
            .background(if (v.vencido) CronoPalette.parpadeo else CronoPalette.carta)
            .border(if (tocaYa) 4.dp else 2.dp, if (v.vencido) CronoPalette.alerta else color, forma),
    ) {
        if (v.momento == MomentoPaso.POR_EMPEZAR) {
            ContenidoPorEmpezar(v, color, acciones)
            return@BoxWithConstraints
        }
        val escala = LocalDensity.current.fontScale
        // En marcha solo se enseña la SEÑAL; vencido, SACA y la SEÑAL.
        val hayMetoSaco = v.vencido && !v.pantalla.saca.isNullOrBlank()
        val hayNota = !v.pantalla.senal.isNullOrBlank()
        if (tarjetaCompacta(maxHeight.value, escala, hayNota = hayNota, hayMetoSaco = hayMetoSaco)) {
            ContenidoCompacto(v, color, acciones)
            return@BoxWithConstraints
        }
        val plan = planTarjeta(maxHeight.value, carriles, hayMetoSaco, hayNota)
        val paso = bloquesDelMomento(v, MaterialTheme.typography.bodyLarge)
        Column(
            Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FilaTitulo(v, color, acciones)
            FilaDatos(v, acciones)
            TituloPaso(v, plan.tituloLineas, acciones)
            BloqueReloj(v, color, plan.relojSp, Modifier.weight(1f))
            PilaPrioritaria(
                (if (v.vencido) listOfNotNull(paso.saca, paso.senal) else listOfNotNull(paso.senal)),
                Modifier.heightIn(max = (plan.textoDp * maxOf(1f, escala)).dp),
            )
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
            Row(
                Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // El nombre no se trocea: la letra baja antes de llegar a los puntos suspensivos.
                Text(
                    v.nombre,
                    Modifier.weight(1f, fill = false),
                    color = colorCarrilTexto(color),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    autoSize =
                        TextAutoSize.StepBased(
                            minFontSize = 10.sp,
                            maxFontSize = MaterialTheme.typography.titleSmall.fontSize,
                        ),
                )
                Text(
                    stringResource(R.string.crono_paso_de, v.paso, v.pasos),
                    color = CronoPalette.apagado,
                    style = MaterialTheme.typography.labelLarge.merge(Tabular),
                    maxLines = 1,
                )
            }
        }
        MandosSecundarios(v, acciones)
    }
}

/**
 * Segunda línea de la cabecera: la hora prevista a la izquierda (en una línea) y, a la derecha, fuego, tapa y «i».
 * No sale si el carril no tiene nada de eso (por ejemplo, un paso sin fuego en marcha).
 */
@Composable
private fun FilaDatos(
    v: VistaCarril,
    acciones: AccionesCarril,
) {
    val hayDatos = previstaDe(v) != null || v.fuego != null || recursoTapa(v.tapa) != null || v.tienePorque
    if (!hayDatos) return
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.weight(1f)) { HoraPrevista(v) }
        ChipFuego(v)
        ChipTapa(v)
        if (v.tienePorque) BotonPorque(v, acciones)
    }
}

/**
 * Pausa y +30 s (o el enlace «Empezar igual» si el carril espera a otro): secundarios, el principal está abajo. Pasado
 * de tiempo solo queda +30 s; en pausa, +30 s junto a «Seguir».
 */
@Composable
internal fun MandosSecundarios(
    v: VistaCarril,
    acciones: AccionesCarril,
    compacto: Boolean = false,
) {
    when (v.accion) {
        AccionCarril.PAUSAR -> {
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

        // Vencido no se pausa (el reloj ya pasó de cero): solo +30 s.
        AccionCarril.SEGUIR, AccionCarril.HECHO -> {
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

/** Título del paso, siempre en su propia línea (hasta [lineas]); al tocarlo se abre el porqué (si lo hay). */
@Composable
private fun TituloPaso(
    v: VistaCarril,
    lineas: Int,
    acciones: AccionesCarril,
) {
    Text(
        v.pantalla.titulo,
        Modifier.fillMaxWidth().abrePorque(v, acciones),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        maxLines = lineas,
        overflow = TextOverflow.Ellipsis,
        autoSize = TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = 22.sp),
    )
}

/**
 * La «i» que abre el porqué del paso: botón de 48 dp de área táctil con su descripción para TalkBack. Con [encabezado]
 * (tarjetas compactas y «por empezar», cuya cabecera ya es pulsable y ocupa más de 48 dp de alto) solo lleva 28 dp de
 * alto para no engordar la cabecera; el resto del toque lo recoge la propia cabecera, que abre lo mismo.
 */
@Composable
internal fun BotonPorque(
    v: VistaCarril,
    acciones: AccionesCarril,
    encabezado: Boolean = false,
) {
    val descripcion = stringResource(R.string.crono_ver_porque)
    val modifier = Modifier.testTag("porque-${v.id}").semantics { contentDescription = descripcion }
    if (encabezado) {
        Box(
            modifier
                .size(width = AREA_PORQUE, height = ALTO_PORQUE_ENCABEZADO)
                .clickable(role = Role.Button) { acciones.porque(v.id) },
            contentAlignment = Alignment.Center,
        ) { CirculoPorque() }
    } else {
        IconButton(onClick = { acciones.porque(v.id) }, modifier.size(AREA_PORQUE)) { CirculoPorque() }
    }
}

private val AREA_PORQUE = 48.dp
private val ALTO_PORQUE_ENCABEZADO = 28.dp

/** Círculo con una "i" (lo que se ve del botón; sin semántica propia, la lleva el botón). */
@Composable
private fun CirculoPorque() {
    Text(
        "i",
        Modifier
            .clearAndSetSemantics {}
            .clip(RoundedCornerShape(50))
            .background(CronoPalette.carta2)
            .width(24.dp)
            .padding(vertical = 2.dp),
        color = CronoPalette.acento,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
    )
}

/**
 * A la izquierda, «a la vez que ②» (si el paso tiene `junto`) y «Luego: …»; a la derecha, la acción principal: botón
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
                textoLuego(v),
                color = CronoPalette.apagado,
                style = MaterialTheme.typography.bodySmall,
                maxLines = if (junto != null) 1 else 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        BotonAccion(v, acciones, Modifier.widthIn(min = 120.dp, max = 220.dp))
    }
}
