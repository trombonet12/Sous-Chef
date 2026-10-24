package app.trombito.souschef.ui.ajustes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.domain.copia.ResultadoCopia
import java.time.LocalDate

/** Tipos que se aceptan al cargar: hay gestores de archivos que no marcan el .json como JSON. */
private val TIPOS_CARGA = arrayOf("application/json", "text/plain", "application/octet-stream")

/**
 * Copia del historial en un archivo del móvil (fuera de la app): sobrevive a «borrar datos» y a reinstalar. Guardar
 * crea el archivo donde elijas; cargar añade lo que falte, sin pisar ni borrar nada.
 */
@Composable
internal fun SeccionCopia(
    resultado: ResultadoCopia?,
    onGuardar: (String) -> Unit,
    onCargar: (String) -> Unit,
) {
    val guardar =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            uri?.let { onGuardar(it.toString()) }
        }
    val cargar =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { onCargar(it.toString()) }
        }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.ajustes_historial_detalle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { guardar.launch("sous-chef-historial-${LocalDate.now()}.json") },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.ajustes_copia_guardar)) }
            OutlinedButton(
                onClick = { cargar.launch(TIPOS_CARGA) },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.ajustes_copia_cargar)) }
        }
        resultado?.let { Text(textoResultado(it), style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun textoResultado(r: ResultadoCopia): String =
    when (r) {
        is ResultadoCopia.Guardada -> {
            pluralStringResource(R.plurals.ajustes_copia_guardada, r.cocinadas, r.cocinadas)
        }

        is ResultadoCopia.Cargada -> {
            val cargada =
                pluralStringResource(R.plurals.ajustes_copia_cargada, r.cocinadasNuevas, r.cocinadasNuevas)
            if (r.descartadas > 0) {
                "$cargada ${stringResource(R.string.ajustes_copia_descartadas, r.descartadas)}"
            } else {
                cargada
            }
        }

        ResultadoCopia.Grande -> {
            stringResource(R.string.ajustes_copia_grande)
        }

        ResultadoCopia.NoValida -> {
            stringResource(R.string.ajustes_copia_no_valida)
        }

        ResultadoCopia.Fallo -> {
            stringResource(R.string.ajustes_copia_fallo)
        }
    }
