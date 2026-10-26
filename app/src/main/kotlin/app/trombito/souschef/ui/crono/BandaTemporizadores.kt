package app.trombito.souschef.ui.crono

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.receta.crono.EstadoCrono
import app.trombito.souschef.receta.crono.TemporizadorLibre
import app.trombito.souschef.receta.crono.TemporizadoresLibres
import app.trombito.souschef.ui.comun.Tabular
import app.trombito.souschef.ui.shell.Icono
import app.trombito.souschef.ui.theme.CronoPalette

private val MINUTOS_RAPIDOS = listOf(1, 3, 5, 10, 15)
private const val DIGITOS_MAX = 3
private val BOTON_QUITAR = 48.dp

/**
 * Banda de los temporizadores libres (ADR-0016), común a las tres disposiciones: una fila compacta por temporizador con
 * su cuenta atrás y un botón para quitarlo. Sin libres no ocupa nada.
 */
@Composable
internal fun BandaTemporizadores(
    estado: EstadoCrono,
    ahoraMs: Long,
    onQuitar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (estado.libres.isEmpty()) return
    Column(
        modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).testTag("banda-temporizadores"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        estado.libres.forEach { FilaTemporizador(it, ahoraMs, onQuitar) }
    }
}

@Composable
private fun FilaTemporizador(
    libre: TemporizadorLibre,
    ahoraMs: Long,
    onQuitar: (String) -> Unit,
) {
    val quedan = libre.quedanS(ahoraMs)
    val listo = libre.avisado || quedan <= 0
    Row(
        Modifier
            .testTag("temporizador-${libre.id}")
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (listo) CronoPalette.parpadeo else CronoPalette.carta)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icono.Reloj.vector, null, Modifier.size(20.dp), tint = CronoPalette.apagado)
        Text(
            stringResource(R.string.crono_temporizador_fila, libre.minutos),
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            if (listo) stringResource(R.string.crono_temporizador_listo) else formatoReloj(quedan),
            color = if (listo) CronoPalette.alerta else CronoPalette.texto,
            style = MaterialTheme.typography.titleMedium.merge(Tabular),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        IconButton(onClick = { onQuitar(libre.id) }, Modifier.size(BOTON_QUITAR)) {
            Icon(Icono.Cerrar.vector, stringResource(R.string.crono_temporizador_quitar, libre.minutos))
        }
    }
}

/** Ya hay tantos temporizadores sin avisar como caben a la vez. */
internal fun EstadoCrono.libresLlenos(): Boolean =
    TemporizadoresLibres.pendientes(this).size >= TemporizadoresLibres.MAX_A_LA_VEZ

/**
 * Diálogo de «Temporizador»: minutos rápidos (1, 3, 5, 10 y 15) y un campo de 1 a 180. Con tres en marcha solo avisa.
 */
@Composable
internal fun DialogoTemporizador(
    lleno: Boolean,
    onPoner: (Int) -> Unit,
    onCerrar: () -> Unit,
) {
    var texto by remember { mutableStateOf("") }
    val minutos = minutosValidos(texto)
    val poner = { m: Int ->
        onPoner(m)
        onCerrar()
    }
    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = CronoPalette.carta2,
        titleContentColor = CronoPalette.texto,
        textContentColor = CronoPalette.texto,
        title = { Text(stringResource(R.string.crono_temporizador_titulo)) },
        text = {
            if (lleno) {
                Text(stringResource(R.string.crono_temporizador_lleno), Modifier.testTag("temporizador-lleno"))
            } else {
                OpcionesTemporizador(texto, { texto = it.filter(Char::isDigit).take(DIGITOS_MAX) }, poner)
            }
        },
        confirmButton = {
            if (!lleno) {
                TextButton(
                    onClick = { minutos?.let(poner) },
                    enabled = minutos != null,
                    modifier = Modifier.testTag("temporizador-poner"),
                ) {
                    Text(stringResource(R.string.crono_temporizador_poner))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) {
                Text(stringResource(R.string.crono_temporizador_cancelar), color = CronoPalette.texto)
            }
        },
    )
}

/** Los minutos del campo si están entre 1 y 180; null si no. */
internal fun minutosValidos(texto: String): Int? =
    texto.toIntOrNull()?.takeIf { it in TemporizadoresLibres.MIN_MINUTOS..TemporizadoresLibres.MAX_MINUTOS }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OpcionesTemporizador(
    texto: String,
    onTexto: (String) -> Unit,
    onPoner: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MINUTOS_RAPIDOS.forEach { m ->
                AssistChip(
                    onClick = { onPoner(m) },
                    label = { Text(stringResource(R.string.crono_temporizador_rapido, m)) },
                    modifier = Modifier.testTag("temporizador-rapido-$m"),
                )
            }
        }
        OutlinedTextField(
            value = texto,
            onValueChange = onTexto,
            label = { Text(stringResource(R.string.crono_temporizador_campo)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedTextColor = CronoPalette.texto,
                    unfocusedTextColor = CronoPalette.texto,
                ),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("temporizador-campo"),
        )
    }
}
