package app.trombito.souschef.ui.novedades

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.domain.LineaNovedad
import app.trombito.souschef.domain.Novedades
import app.trombito.souschef.ui.shell.Icono

private val MIN_TOQUE = 48.dp
private val LADO_VINETA = 6.dp
private const val GIRO_ABIERTA = 90f

/** Las líneas de una versión con viñeta. Las de destino `receta:<slug>` se pueden tocar y abren esa ficha. */
@Composable
internal fun LineasNovedades(
    novedades: Novedades,
    onReceta: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        novedades.lineas.forEach { LineaNovedadFila(it, onReceta) }
    }
}

@Composable
private fun LineaNovedadFila(
    linea: LineaNovedad,
    onReceta: (String) -> Unit,
) {
    val slug = slugDeDestino(linea.destino)
    val base = Modifier.fillMaxWidth()
    val fila =
        if (slug == null) {
            base.padding(vertical = 4.dp)
        } else {
            base
                .heightIn(min = MIN_TOQUE)
                .clickable(
                    onClickLabel = stringResource(R.string.novedades_abrir_receta),
                    role = Role.Button,
                    onClick = { onReceta(slug) },
                ).padding(vertical = 4.dp)
        }
    Row(fila, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .padding(top = 9.dp)
                .size(LADO_VINETA)
                .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape),
        )
        Text(
            linea.texto,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (slug == null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
            textDecoration = if (slug == null) null else TextDecoration.Underline,
        )
    }
}

/** Una versión anterior plegada bajo «También en X»; al tocarla se despliegan sus líneas. */
@Composable
internal fun PlegadaNovedades(
    novedades: Novedades,
    onReceta: (String) -> Unit,
) {
    var abierta by rememberSaveable(novedades.version) { mutableStateOf(false) }
    val estado =
        stringResource(if (abierta) R.string.novedades_desplegado else R.string.novedades_plegado)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = MIN_TOQUE)
            .clickable(role = Role.Button, onClick = { abierta = !abierta })
            .semantics { stateDescription = estado }
            .testTag("novedades-tambien-${novedades.version}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.novedades_tambien, novedades.version),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
        )
        Icon(
            Icono.Chevron.vector,
            contentDescription = null,
            modifier = Modifier.rotate(if (abierta) GIRO_ABIERTA else 0f),
        )
    }
    if (abierta) LineasNovedades(novedades, onReceta)
}
