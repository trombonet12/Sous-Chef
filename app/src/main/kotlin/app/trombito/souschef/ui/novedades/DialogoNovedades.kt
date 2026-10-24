package app.trombito.souschef.ui.novedades

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.trombito.souschef.R
import app.trombito.souschef.domain.Novedades

private val ANCHO_MAX = 560.dp
private val ALTO_MAX = 640.dp
private val MIN_TOQUE = 48.dp

/**
 * Las novedades tras actualizar, encima de Recetas. Un diálogo flotante (no a sangre) para que «un toque fuera» cierre;
 * Atrás y «A cocinar» también. [onMostrada] se llama al aparecer (ahí se marcan como vistas), no al cerrar.
 */
@Composable
fun DialogoNovedades(
    novedades: List<Novedades>,
    onMostrada: () -> Unit,
    onCerrar: () -> Unit,
    onReceta: (String) -> Unit,
) {
    LaunchedEffect(Unit) { onMostrada() }
    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.padding(16.dp).widthIn(max = ANCHO_MAX).fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            ContenidoNovedades(novedades, confeti = true, onCerrar = onCerrar, onReceta = onReceta)
        }
    }
}

/**
 * Contenido del diálogo sin ventana (se prueba aparte). Primero la entrada más nueva con su título; las anteriores,
 * plegadas bajo «También en X». El confeti va detrás de todo y no recibe toques.
 */
@Composable
internal fun ContenidoNovedades(
    novedades: List<Novedades>,
    confeti: Boolean,
    onCerrar: () -> Unit,
    onReceta: (String) -> Unit,
) {
    val nueva = novedades.firstOrNull() ?: return
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { foco.requestFocus() } }
    Box(Modifier.semantics { paneTitle = nueva.titulo }) {
        if (confeti) Confeti(Modifier.matchParentSize().testTag("novedades-confeti"))
        Column(Modifier.padding(24.dp)) {
            Text(
                nueva.titulo,
                modifier =
                    Modifier
                        .focusRequester(foco)
                        .focusable()
                        .semantics { heading() }
                        .testTag("novedades-titulo"),
                style = MaterialTheme.typography.headlineSmall,
            )
            Column(
                Modifier.weight(1f, fill = false).heightIn(max = ALTO_MAX).verticalScroll(rememberScrollState()),
            ) {
                LineasNovedades(nueva, onReceta, Modifier.padding(top = 12.dp))
                novedades.drop(1).forEach { PlegadaNovedades(it, onReceta) }
            }
            Button(
                onClick = onCerrar,
                modifier =
                    Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .heightIn(min = MIN_TOQUE)
                        .testTag("novedades-cerrar"),
            ) {
                Text(stringResource(R.string.novedades_cerrar))
            }
        }
    }
}
