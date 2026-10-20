package app.trombito.souschef.ui.ficha

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ChipColors
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.ui.shell.Icono
import app.trombito.souschef.ui.theme.FichaPalette

// Piezas comunes de las pestañas de la ficha, siempre en papel (también con el tema oscuro del sistema).

/** Aviso discreto en papel: una línea pequeña y apagada. */
@Composable
fun NotaFicha(texto: String) {
    Text(
        texto,
        Modifier.padding(top = 6.dp),
        style = MaterialTheme.typography.bodySmall,
        color = FichaPalette.meta,
    )
}

/** Etiqueta pequeña de solo lectura sobre papel (minuto de entrada, fuego, tapa). */
@Composable
fun ChipPapel(
    texto: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = FichaPalette.papel2,
        contentColor = FichaPalette.tinta2,
        border = BorderStroke(1.dp, FichaPalette.linea),
    ) {
        Text(
            texto,
            Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

/** Colores de los chips que se tocan (filtros, notas) sobre papel. */
@Composable
fun coloresAsistentePapel(): ChipColors =
    AssistChipDefaults.assistChipColors(
        containerColor = FichaPalette.papel,
        labelColor = FichaPalette.tinta,
        leadingIconContentColor = FichaPalette.tinta2,
    )

@Composable
fun bordeAsistentePapel() = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = FichaPalette.tinta3)

@Composable
fun coloresFiltroPapel(): SelectableChipColors =
    FilterChipDefaults.filterChipColors(
        containerColor = FichaPalette.papel,
        labelColor = FichaPalette.tinta,
        selectedContainerColor = FichaPalette.papel2,
        selectedLabelColor = FichaPalette.tinta,
    )

@Composable
fun bordeFiltroPapel(seleccionado: Boolean) =
    FilterChipDefaults.filterChipBorder(
        enabled = true,
        selected = seleccionado,
        borderColor = FichaPalette.linea,
        selectedBorderColor = FichaPalette.tinta3,
    )

/**
 * Bloque plegable: el título (con un [extra] opcional a la derecha) siempre se ve y el contenido sale al tocar.
 * Lo plegado se recuerda al girar la pantalla y al cambiar de pestaña.
 */
@Composable
fun PlegableFicha(
    titulo: String,
    modifier: Modifier = Modifier,
    abiertoInicial: Boolean = false,
    extra: String? = null,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    var abierto by rememberSaveable { mutableStateOf(abiertoInicial) }
    val estado = stringResource(if (abierto) R.string.ficha_desplegado else R.string.ficha_plegado)
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable { abierto = !abierto }
                .semantics { stateDescription = estado },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(titulo, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            if (extra != null) {
                Text(extra, style = MaterialTheme.typography.labelMedium, color = FichaPalette.meta)
            }
            Icon(
                Icono.Chevron.vector,
                contentDescription = null,
                modifier = Modifier.padding(start = 4.dp).rotate(if (abierto) GIRO_ABIERTO else 0f),
            )
        }
        if (abierto) contenido()
    }
}

internal const val GIRO_ABIERTO = 90f
