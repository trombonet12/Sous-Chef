package app.trombito.souschef.ui.crono

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.ui.theme.CronoPalette

/**
 * Ventana de deshacer: la cocinada aún no está en el historial. [segundos] es la cuenta atrás (null si todavía no hay
 * tic); «Guardar y salir ya» la confirma sin esperar y «Deshacer» vuelve al cronómetro.
 */
@Composable
fun CierreGuardado(
    segundos: Int?,
    onDeshacer: () -> Unit,
    onGuardarYa: () -> Unit,
) {
    val descripcion = stringResource(R.string.crono_a11y_deshacer_cierre)
    val descripcionGuardar = stringResource(R.string.crono_a11y_guardar_ya)
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (segundos != null) {
                stringResource(R.string.crono_se_guarda_en, segundos)
            } else {
                stringResource(R.string.crono_se_guarda_pronto)
            },
            Modifier.testTag("cierre-cuenta-atras"),
        )
        Button(
            onClick = onGuardarYa,
            modifier =
                Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("guardar-cierre")
                    .semantics { contentDescription = descripcionGuardar },
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = CronoPalette.principal,
                    contentColor = CronoPalette.onPrincipal,
                ),
        ) {
            Text(stringResource(R.string.crono_guardar_ya))
        }
        OutlinedButton(
            onClick = onDeshacer,
            modifier =
                Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("deshacer-cierre")
                    .semantics { contentDescription = descripcion },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CronoPalette.texto),
        ) {
            Text(stringResource(R.string.crono_deshacer))
        }
    }
}

/** Segundos que quedan de la ventana de deshacer (de 1 a 12, redondeo `ceil`); null si aún no hay referencia. */
internal fun segundosParaGuardar(ui: CronoUi): Int? =
    ui.cierreHastaMs?.takeIf { ui.ahoraMs > 0L }?.let { hasta ->
        val restante = (hasta - ui.ahoraMs + MS_POR_SEGUNDO - 1) / MS_POR_SEGUNDO
        restante.toInt().coerceIn(1, (VENTANA_DESHACER_MS / MS_POR_SEGUNDO).toInt())
    }

private const val MS_POR_SEGUNDO = 1_000L
