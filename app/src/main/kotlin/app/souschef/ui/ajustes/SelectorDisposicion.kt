package app.souschef.ui.ajustes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.souschef.R
import app.souschef.domain.DisposicionCrono
import app.souschef.ui.theme.ColoresCrono
import app.souschef.ui.theme.CronoPalette

/** Ancho mínimo por tarjeta para ponerlas en fila; si no caben, van una debajo de otra con la miniatura al lado. */
private val ANCHO_MIN_TARJETA = 104.dp
private val MINIATURA_EN_LISTA = 72.dp
internal const val PROPORCION = 9f / 16f

private val COLORES_MUESTRA = listOf(Color(0xFFF0AE4A), Color(0xFFE8836B), Color(0xFF7FB7D6))

private class OpcionDisposicion(
    val disposicion: DisposicionCrono,
    val titulo: Int,
    val detalle: Int,
)

private val OPCIONES =
    listOf(
        OpcionDisposicion(DisposicionCrono.APILADA, R.string.ajustes_apilada, R.string.ajustes_apilada_detalle),
        OpcionDisposicion(DisposicionCrono.FOCO, R.string.ajustes_foco, R.string.ajustes_foco_detalle),
        OpcionDisposicion(DisposicionCrono.CLASICA, R.string.ajustes_clasica, R.string.ajustes_clasica_detalle),
    )

/**
 * Las tres disposiciones del cronómetro como tarjetas con una miniatura (un esquema, no una captura: se adapta a la
 * letra y no envejece). Lo usan Ajustes y el menú ⋮ de la cocina.
 */
@Composable
fun SelectorDisposicion(
    elegida: DisposicionCrono,
    onElegir: (DisposicionCrono) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tarjetas =
        OPCIONES.map {
            OpcionTarjeta(it.titulo, it.detalle, "disposicion-${it.disposicion.name}", it.disposicion == elegida)
        }
    SelectorTarjetas(tarjetas, { onElegir(OPCIONES[it].disposicion) }, modifier) { i, mod ->
        Miniatura(OPCIONES[i].disposicion, mod)
    }
}

/** Datos de una tarjeta-opción de los selectores con miniatura (textos como recursos). */
internal class OpcionTarjeta(
    val titulo: Int,
    val detalle: Int,
    val testTag: String,
    val elegida: Boolean,
)

/**
 * Contenedor común de los selectores de tarjetas con miniatura: en fila si caben (con el detalle de la elegida
 * debajo) o una debajo de otra con la miniatura al lado. [onElegir] recibe la posición de la tarjeta pulsada.
 */
@Composable
internal fun SelectorTarjetas(
    opciones: List<OpcionTarjeta>,
    onElegir: (Int) -> Unit,
    modifier: Modifier = Modifier,
    miniatura: @Composable (Int, Modifier) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val enFila = maxWidth >= ANCHO_MIN_TARJETA * opciones.size + 16.dp
        if (enFila) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Min).selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    opciones.forEachIndexed { i, opcion ->
                        TarjetaOpcion(opcion, enFila = true, Modifier.weight(1f).fillMaxHeight(), { onElegir(i) }) {
                            miniatura(i, it)
                        }
                    }
                }
                // En fila no cabe el detalle de cada una: se explica la elegida debajo.
                opciones.firstOrNull { it.elegida }?.let {
                    Text(
                        stringResource(it.detalle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            Column(Modifier.fillMaxWidth().selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                opciones.forEachIndexed { i, opcion ->
                    TarjetaOpcion(opcion, enFila = false, Modifier, { onElegir(i) }) { miniatura(i, it) }
                }
            }
        }
    }
}

@Composable
private fun TarjetaOpcion(
    opcion: OpcionTarjeta,
    enFila: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    miniatura: @Composable (Modifier) -> Unit,
) {
    val forma = RoundedCornerShape(12.dp)
    val elegida = opcion.elegida
    Surface(
        modifier
            .testTag(opcion.testTag)
            .clip(forma)
            .selectable(selected = elegida, role = Role.RadioButton, onClick = onClick),
        shape = forma,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border =
            BorderStroke(
                if (elegida) 2.dp else 1.dp,
                if (elegida) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
            ),
    ) {
        if (enFila) {
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                miniatura(Modifier.fillMaxWidth())
                Text(
                    stringResource(opcion.titulo),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (elegida) FontWeight.Bold else FontWeight.Normal,
                )
            }
        } else {
            Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                miniatura(Modifier.width(MINIATURA_EN_LISTA))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        stringResource(opcion.titulo),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (elegida) FontWeight.Bold else FontWeight.Normal,
                    )
                    Text(
                        stringResource(opcion.detalle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Esquema de la pantalla de cocina con la paleta del cronómetro en el tema en uso, en proporción de móvil. */
@Composable
private fun Miniatura(
    disposicion: DisposicionCrono,
    modifier: Modifier,
) {
    val p = CronoPalette
    Canvas(
        modifier
            .aspectRatio(PROPORCION)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, p.linea, RoundedCornerShape(8.dp))
            .clearAndSetSemantics {},
    ) {
        drawRect(p.fondo)
        val m = size.width * 0.06f
        // Cabecera: servir y llegada.
        caja(Offset(m, m), Size(size.width - 2 * m, size.height * 0.06f), p.carta2)
        val arriba = m * 2 + size.height * 0.06f
        when (disposicion) {
            DisposicionCrono.APILADA -> apilada(p, arriba, m)
            DisposicionCrono.FOCO -> foco(p, arriba, m)
            DisposicionCrono.CLASICA -> clasica(p, arriba, m)
        }
    }
}

private fun DrawScope.caja(
    origen: Offset,
    tamano: Size,
    color: Color,
) = drawRoundRect(color, origen, tamano, CornerRadius(tamano.minDimension * 0.18f))

/** Tarjeta de carril: franja de color, título, reloj y barra. */
private fun DrawScope.tarjeta(
    p: ColoresCrono,
    origen: Offset,
    tamano: Size,
    color: Color,
    reloj: Float,
) {
    caja(origen, tamano, p.carta)
    val pad = tamano.width * 0.08f
    drawRect(color, origen + Offset(pad, pad), Size(tamano.width * 0.04f, tamano.height * 0.16f))
    caja(origen + Offset(pad * 2, pad), Size(tamano.width * 0.5f, tamano.height * 0.1f), p.apagado)
    val altoReloj = tamano.height * reloj
    caja(
        origen + Offset(tamano.width * 0.2f, (tamano.height - altoReloj) / 2),
        Size(tamano.width * 0.6f, altoReloj),
        p.texto,
    )
    caja(
        origen + Offset(pad, tamano.height - pad * 1.6f),
        Size((tamano.width - 2 * pad) * 0.6f, pad * 0.6f),
        color,
    )
}

private fun DrawScope.apilada(
    p: ColoresCrono,
    arriba: Float,
    m: Float,
) {
    val alto = (size.height - arriba - m * 4) / 3
    COLORES_MUESTRA.forEachIndexed { i, c ->
        tarjeta(p, Offset(m, arriba + i * (alto + m)), Size(size.width - 2 * m, alto), c, reloj = 0.22f)
    }
}

private fun DrawScope.foco(
    p: ColoresCrono,
    arriba: Float,
    m: Float,
) {
    val fila = size.height * 0.09f
    val grande = size.height - arriba - m * 4 - fila * 2
    tarjeta(p, Offset(m, arriba), Size(size.width - 2 * m, grande), COLORES_MUESTRA[0], reloj = 0.16f)
    COLORES_MUESTRA.drop(1).forEachIndexed { i, c ->
        val y = arriba + grande + m + i * (fila + m)
        caja(Offset(m, y), Size(size.width - 2 * m, fila), p.carta)
        drawRect(c, Offset(m * 1.6f, y + fila * 0.2f), Size(size.width * 0.03f, fila * 0.6f))
    }
}

private fun DrawScope.clasica(
    p: ColoresCrono,
    arriba: Float,
    m: Float,
) {
    // Plan plegable.
    val plan = size.height * 0.05f
    caja(Offset(m, arriba), Size(size.width - 2 * m, plan), p.carta)
    // Pestañas de recipientes.
    val yPest = arriba + plan + m
    val altoPest = size.height * 0.09f
    val anchoPest = (size.width - 2 * m - 2 * m * 0.6f) / 3
    COLORES_MUESTRA.forEachIndexed { i, c ->
        val x = m + i * (anchoPest + m * 0.6f)
        caja(Offset(x, yPest), Size(anchoPest, altoPest), if (i == 0) c.copy(alpha = 0.35f) else p.carta)
    }
    // Panel del recipiente: título, reloj grande y líneas de texto (con scroll).
    val yPanel = yPest + altoPest + m
    val altoBotones = size.height * 0.08f
    val altoPanel = size.height - yPanel - altoBotones - m * 2
    caja(Offset(m, yPanel + m), Size(size.width * 0.55f, altoPanel * 0.06f), p.apagado)
    caja(
        Offset(size.width * 0.2f, yPanel + altoPanel * 0.2f),
        Size(size.width * 0.6f, altoPanel * 0.18f),
        p.texto,
    )
    caja(Offset(m, yPanel + altoPanel * 0.46f), Size(size.width - 2 * m, altoPanel * 0.04f), COLORES_MUESTRA[0])
    repeat(4) { i ->
        val y = yPanel + altoPanel * (0.58f + i * 0.1f)
        caja(
            Offset(m, y),
            Size(
                (size.width - 2 * m) * (
                    if (i % 2 ==
                        0
                    ) {
                        0.9f
                    } else {
                        0.7f
                    }
                ),
                altoPanel * 0.04f,
            ),
            p.linea,
        )
    }
    // Mandos abajo.
    caja(Offset(m, size.height - altoBotones - m), Size(size.width - 2 * m, altoBotones), p.principal)
}
