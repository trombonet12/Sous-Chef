package app.trombito.souschef.ui.novedades

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trombito.souschef.R
import app.trombito.souschef.domain.Novedades
import app.trombito.souschef.ui.comun.BarraTitulo
import app.trombito.souschef.ui.comun.EstadoVacio

/** Ajustes → Acerca de → Novedades: todas las versiones, de la más nueva a la más vieja, sin confeti. */
@Composable
fun TodasNovedadesScreen(
    onAtras: () -> Unit,
    onReceta: (String) -> Unit,
    viewModel: TodasNovedadesViewModel = hiltViewModel(),
) {
    val todas by viewModel.todas.collectAsStateWithLifecycle()
    TodasNovedades(todas, onAtras, onReceta)
}

/** Pantalla sin ViewModel: `null` = leyendo. */
@Composable
internal fun TodasNovedades(
    todas: List<Novedades>?,
    onAtras: () -> Unit,
    onReceta: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        BarraTitulo(stringResource(R.string.novedades_titulo), onAtras)
        when {
            todas == null -> {
                CircularProgressIndicator(Modifier.padding(32.dp))
            }

            todas.isEmpty() -> {
                EstadoVacio(
                    stringResource(R.string.novedades_vacio_titulo),
                    stringResource(R.string.novedades_vacio_detalle),
                )
            }

            else -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    todas.forEach { version ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                version.titulo,
                                modifier = Modifier.semantics { heading() },
                                style = MaterialTheme.typography.titleLarge,
                            )
                            LineasNovedades(version, onReceta)
                        }
                    }
                }
            }
        }
    }
}
