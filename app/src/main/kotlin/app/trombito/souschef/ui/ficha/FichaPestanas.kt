package app.trombito.souschef.ui.ficha

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.ui.theme.Espacio
import app.trombito.souschef.ui.theme.FichaPalette

/** Fila de pestañas con scroll horizontal: a 360 dp no caben cinco con su texto. */
@Composable
fun BarraPestanas(
    elegida: PestanaFicha,
    onElegir: (PestanaFicha) -> Unit,
) {
    PrimaryScrollableTabRow(selectedTabIndex = elegida.ordinal, edgePadding = Espacio.s) {
        PestanaFicha.entries.forEach { p ->
            Tab(
                selected = p == elegida,
                onClick = { onElegir(p) },
                modifier = Modifier.testTag("ficha-pestana-${p.name.lowercase()}"),
                text = { Text(stringResource(p.titulo), maxLines = 1) },
            )
        }
    }
}

/**
 * Contenido de la pestaña elegida. Cada pestaña conserva su scroll y lo que se haya desplegado en ella al ir y volver.
 */
@Composable
fun ContenidoPestana(
    elegida: PestanaFicha,
    receta: Receta,
    estado: FichaUiState,
    onTecnica: (String) -> Unit,
) {
    val guardado = rememberSaveableStateHolder()
    val scrolls = List(PestanaFicha.entries.size) { rememberScrollState() }
    val scroll = scrolls[elegida.ordinal]
    guardado.SaveableStateProvider(elegida.name) {
        if (elegida == PestanaFicha.PASOS && receta.pasos.isNotEmpty()) {
            PestanaPasos(receta, scroll)
        } else {
            CuerpoPestana(scroll, elegida) { CuerpoDe(elegida, receta, estado, onTecnica) }
        }
    }
}

@Composable
private fun CuerpoDe(
    pestana: PestanaFicha,
    receta: Receta,
    estado: FichaUiState,
    onTecnica: (String) -> Unit,
) {
    when (pestana) {
        PestanaFicha.RESUMEN -> PestanaResumen(receta, estado, onTecnica)
        PestanaFicha.INGREDIENTES -> SiHay(receta.grupos.isNotEmpty()) { PestanaIngredientes(receta, estado) }
        PestanaFicha.PREPARACION -> SiHay(receta.mep.isNotEmpty()) { PestanaPreparacion(receta) }
        PestanaFicha.PASOS -> SiHay(false) {}
        PestanaFicha.PORQUE -> SiHay(receta.tienePorque()) { PestanaPorque(receta) }
    }
}

@Composable
private fun SiHay(
    hay: Boolean,
    contenido: @Composable () -> Unit,
) {
    if (hay) {
        contenido()
    } else {
        Text(
            stringResource(R.string.ficha_tab_vacia),
            style = MaterialTheme.typography.bodyMedium,
            color = FichaPalette.meta,
        )
    }
}

/** Columna con scroll propio de la pestaña; la etiqueta de prueba es `ficha-<pestaña>`. */
@Composable
private fun CuerpoPestana(
    scroll: ScrollState,
    pestana: PestanaFicha,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .testTag("ficha-${pestana.name.lowercase()}")
            .verticalScroll(scroll)
            .padding(horizontal = Espacio.l, vertical = Espacio.m),
        verticalArrangement = Arrangement.spacedBy(Espacio.m),
        content = contenido,
    )
}
