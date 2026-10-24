package app.trombito.souschef.ui.crono

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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

/** «Cocinada guardada» durante la ventana de deshacer: el botón hace lo mismo que la acción del snackbar. */
@Composable
fun CierreGuardado(onDeshacer: () -> Unit) {
    val descripcion = stringResource(R.string.crono_a11y_deshacer_cierre)
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.crono_guardado))
        Button(
            onClick = onDeshacer,
            modifier =
                Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("deshacer-cierre")
                    .semantics { contentDescription = descripcion },
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = CronoPalette.principal,
                    contentColor = CronoPalette.onPrincipal,
                ),
        ) {
            Text(stringResource(R.string.crono_deshacer))
        }
    }
}
