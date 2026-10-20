package app.trombito.souschef.ui.crono.portada

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.receta.TipoCocina
import app.trombito.souschef.ui.comun.LocalCocina
import app.trombito.souschef.ui.comun.Tabular
import app.trombito.souschef.ui.comun.textoRico
import app.trombito.souschef.ui.crono.BloqueCasillas
import app.trombito.souschef.ui.crono.Casilla
import app.trombito.souschef.ui.shell.Icono
import app.trombito.souschef.ui.theme.CronoPalette

/** Lo que la checklist le pide a quien la use: marcar una casilla o varias a la vez. */
internal class AccionesChecklist(
    val marcar: (String, Boolean) -> Unit,
    val marcarVarias: (List<String>, Boolean) -> Unit,
)

/** Título («Antes de encender nada») y descripción de la receta que encabezan la checklist. */
internal data class EncabezadoChecklist(
    val titulo: String,
    val descripcion: String,
)

/**
 * Checklist de antes de encender: progreso global, descripción plegable, nota de cocina y un bloque plegable por
 * grupo de casillas. Solo estado y callbacks; los ids de las casillas vienen ya calculados.
 *
 * @param conDetalle true con `mep` (cabecera + ⓘ con el detalle); false con los grupos de ingredientes, que se
 * pintan enteros en una línea.
 */
@Composable
internal fun ChecklistAntes(
    encabezado: EncabezadoChecklist,
    bloques: List<BloqueCasillas>,
    marcados: Set<String>,
    conDetalle: Boolean,
    acciones: AccionesChecklist,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CabeceraChecklist(encabezado, progresoTotal(bloques, marcados))
        bloques.forEachIndexed { i, b ->
            BloqueChecklist(i, b, marcados, conDetalle, acciones)
        }
    }
}

@Composable
private fun BloqueChecklist(
    indice: Int,
    bloque: BloqueCasillas,
    marcados: Set<String>,
    conDetalle: Boolean,
    acciones: AccionesChecklist,
) {
    val avance = progreso(bloque.casillas, marcados)
    var abierto by rememberSaveable(indice) { mutableStateOf(!avance.completo) }
    // Al completarse se pliega solo; si algo se desmarca vuelve a abrirse. Reabrirlo a mano no lo repliega.
    LaunchedEffect(avance.completo) { abierto = !avance.completo }
    Column(Modifier.testTag("bloque-$indice")) {
        if (bloque.titulo != null) {
            CabeceraBloque(
                bloque.titulo,
                avance,
                abierto,
                onAlternar = { abierto = !abierto },
                onMarcarTodo = { acciones.marcarVarias(bloque.casillas.map { it.id }, !avance.completo) },
            )
        }
        if (abierto || bloque.titulo == null) {
            bloque.casillas.forEach { c ->
                FilaCasilla(c, c.id in marcados, conDetalle) { acciones.marcar(c.id, it) }
            }
        }
    }
}

@Composable
private fun CabeceraBloque(
    titulo: String,
    avance: Progreso,
    abierto: Boolean,
    onAlternar: () -> Unit,
    onMarcarTodo: () -> Unit,
) {
    val estado =
        stringResource(if (abierto) R.string.crono_checklist_desplegado else R.string.crono_checklist_plegado)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button, onClick = onAlternar)
                .semantics { stateDescription = estado },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Chevron(abierto)
            Text(
                titulo,
                Modifier.padding(start = 8.dp).weight(1f, fill = false),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (avance.completo) {
                    stringResource(R.string.crono_checklist_hecho)
                } else {
                    stringResource(R.string.crono_checklist_contador, avance.hechas, avance.total)
                },
                Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.labelLarge.merge(Tabular),
                color = if (avance.completo) CronoPalette.ok else CronoPalette.apagado,
            )
        }
        BotonMarcarTodo(titulo, avance.completo, onMarcarTodo)
    }
}

@Composable
private fun BotonMarcarTodo(
    titulo: String,
    completo: Boolean,
    onClick: () -> Unit,
) {
    val accion =
        stringResource(
            if (completo) R.string.crono_checklist_desmarcar_todo_en else R.string.crono_checklist_marcar_todo_en,
            titulo,
        )
    val texto =
        stringResource(if (completo) R.string.crono_checklist_desmarcar_todo else R.string.crono_checklist_marcar_todo)
    TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = accion }) {
        Text(texto, color = CronoPalette.acento)
    }
}

@Composable
private fun FilaCasilla(
    casilla: Casilla,
    marcada: Boolean,
    conDetalle: Boolean,
    onCambio: (Boolean) -> Unit,
) {
    val vista = remember(casilla.texto, conDetalle) { if (conDetalle) vistaCasilla(casilla.texto) else null }
    var detalleVisible by remember { mutableStateOf(false) }
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CasillaMarcable(Modifier.weight(1f), casilla, vista, marcada, onCambio)
            if (vista?.detalle != null) {
                IconButton(onClick = { detalleVisible = !detalleVisible }, Modifier.size(48.dp)) {
                    Icon(
                        Icono.Info.vector,
                        stringResource(R.string.crono_checklist_detalle_de, vista.titulo),
                        tint = CronoPalette.apagado,
                    )
                }
            }
        }
        if (detalleVisible && vista?.detalle != null) {
            Text(
                textoRico(vista.detalle),
                Modifier.padding(start = 48.dp, end = 12.dp, bottom = 8.dp),
                color = CronoPalette.apagado,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun CasillaMarcable(
    modifier: Modifier,
    casilla: Casilla,
    vista: VistaCasilla?,
    marcada: Boolean,
    onCambio: (Boolean) -> Unit,
) {
    val color = if (marcada) CronoPalette.ok else CronoPalette.texto
    val tachado = if (marcada) TextDecoration.LineThrough else null
    Row(
        modifier
            .heightIn(min = 48.dp)
            .toggleable(value = marcada, role = Role.Checkbox, onValueChange = onCambio),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = marcada,
            onCheckedChange = null,
            colors =
                CheckboxDefaults.colors(
                    checkedColor = CronoPalette.ok,
                    checkmarkColor = CronoPalette.onPrincipal,
                    uncheckedColor = CronoPalette.tenue,
                ),
        )
        if (vista == null) {
            Text(
                textoRico(casilla.texto),
                Modifier.padding(start = 12.dp).weight(1f),
                color = color,
                textDecoration = tachado,
            )
        } else {
            Text(
                vista.titulo,
                Modifier.padding(start = 12.dp).weight(1f),
                color = color,
                fontWeight = FontWeight.Bold,
                textDecoration = tachado,
            )
            if (vista.minuto != null) ChipMinuto(vista.minuto)
        }
    }
}

@Composable
private fun ChipMinuto(minuto: Int) {
    Spacer(Modifier.width(8.dp))
    Text(
        stringResource(R.string.crono_checklist_min, minuto),
        Modifier
            .background(CronoPalette.carta2, RoundedCornerShape(percent = 50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium.merge(Tabular),
        color = CronoPalette.apagado,
    )
}
