package app.trombito.souschef.ui.ajustes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.domain.copia.EstadoCopiaAuto
import app.trombito.souschef.domain.copia.FalloCopiaAuto
import app.trombito.souschef.domain.copia.UltimaCopia

/**
 * Copia automática en una carpeta (ADR-0015): elegir, cambiar o desactivar, y «Última copia» (la automática o la
 * manual, la más reciente). Sin ViewModel: el estado llega hecho. El texto avisa de que la carpeta puede sincronizarse.
 */
@Composable
internal fun SeccionCopiaAuto(
    estado: EstadoCopiaAuto,
    ahoraMs: Long,
    onElegir: (String) -> Unit,
    onDesactivar: () -> Unit,
) {
    val elegir =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let { onElegir(it.toString()) }
        }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.copia_auto_titulo), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.copia_auto_detalle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(if (estado.activa) R.string.copia_auto_activa else R.string.copia_auto_desactivada),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag("copia-auto-estado"),
        )
        Text(
            textoUltimaCopia(UltimaCopia.dias(estado.ultimaCopiaMs, ahoraMs)),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag("copia-auto-ultima"),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { elegir.launch(null) },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("copia-auto-elegir"),
            ) {
                Text(stringResource(if (estado.activa) R.string.copia_auto_cambiar else R.string.copia_auto_elegir))
            }
            if (estado.activa) {
                OutlinedButton(
                    onClick = onDesactivar,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("copia-auto-desactivar"),
                ) { Text(stringResource(R.string.copia_auto_desactivar)) }
            }
        }
    }
}

/** «Última copia: nunca / hoy / ayer / hace N días». */
@Composable
private fun textoUltimaCopia(dias: Long?): String =
    when {
        dias == null -> stringResource(R.string.copia_auto_ultima_nunca)
        dias == 0L -> stringResource(R.string.copia_auto_ultima_hoy)
        dias == 1L -> stringResource(R.string.copia_auto_ultima_ayer)
        else -> pluralStringResource(R.plurals.copia_auto_ultima_dias, dias.toInt(), dias.toInt())
    }

/**
 * Aviso visible de que la copia automática no funciona, con la acción «Elegir otra carpeta» (abre el selector del
 * sistema). Reutiliza el criterio de los otros avisos de copia: un texto claro y sin tecnicismos.
 */
@Composable
fun AvisoCopiaFallida(
    fallo: FalloCopiaAuto,
    onElegir: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val elegir =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let { onElegir(it.toString()) }
        }
    Surface(
        modifier = modifier.fillMaxWidth().testTag("copia-auto-aviso"),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(
                    when (fallo) {
                        FalloCopiaAuto.CARPETA_NO_VALE -> R.string.copia_auto_aviso_carpeta
                        FalloCopiaAuto.NO_SE_PUDO -> R.string.copia_auto_aviso_escritura
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(
                onClick = { elegir.launch(null) },
                modifier = Modifier.heightIn(min = 48.dp).testTag("copia-auto-aviso-accion"),
            ) { Text(stringResource(R.string.copia_auto_aviso_accion)) }
        }
    }
}
