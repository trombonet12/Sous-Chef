package app.trombito.souschef.ui.ficha

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.receta.BloqueMep
import app.trombito.souschef.receta.LineaMep
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.ui.comun.Tabular
import app.trombito.souschef.ui.shell.Icono
import app.trombito.souschef.ui.theme.FichaPalette

/** Preparación: un bloque plegable por tanda de `mep`, abierto de inicio; el detalle de cada línea sale al tocar. */
@Composable
fun PestanaPreparacion(receta: Receta) {
    receta.mep.forEachIndexed { i, bloque -> key(i) { BloqueMepVista(bloque) } }
}

@Composable
private fun BloqueMepVista(bloque: BloqueMep) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = FichaPalette.papel2,
        contentColor = FichaPalette.tinta,
    ) {
        PlegableFicha(
            titulo = bloque.t,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            abiertoInicial = true,
            extra = pluralStringResource(R.plurals.ficha_prep_cosas, bloque.l.size, bloque.l.size),
        ) {
            bloque.l.forEachIndexed { i, linea ->
                key(i) { FilaMep(if (bloque.orden) "${i + 1}." else null, LineaMep.parse(linea)) }
            }
            bloque.porq?.takeIf { it.isNotBlank() }?.let {
                PlegableFicha(stringResource(R.string.ficha_prep_porque)) {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = FichaPalette.tinta2)
                }
            }
        }
    }
}

/** Cabecera en negrita y «min N» a la derecha; el detalle, al tocar la fila. */
@Composable
private fun FilaMep(
    numero: String?,
    linea: LineaMep,
) {
    var abierta by rememberSaveable { mutableStateOf(false) }
    val detalle = linea.detalle.takeIf { linea.cabecera != null && it.isNotBlank() }
    val alTocar = stringResource(R.string.ficha_prep_ver_detalle)
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .then(
                    if (detalle != null) {
                        Modifier.clickable(onClickLabel = alTocar) { abierta = !abierta }
                    } else {
                        Modifier
                    },
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (numero != null) {
                Text(numero, Modifier.width(28.dp), style = MaterialTheme.typography.bodyMedium.merge(Tabular))
            }
            Text(
                linea.cabecera ?: linea.detalle,
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (linea.cabecera != null) FontWeight.Bold else FontWeight.Normal,
            )
            linea.minuto?.let {
                ChipPapel(stringResource(R.string.ficha_prep_minuto, it), Modifier.padding(start = 8.dp))
            }
            if (detalle != null) {
                Icon(
                    Icono.Chevron.vector,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 4.dp).rotate(if (abierta) GIRO_ABIERTO else 0f),
                )
            }
        }
        if (abierta && detalle != null) {
            Text(
                detalle,
                Modifier.padding(start = if (numero != null) 28.dp else 0.dp, bottom = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = FichaPalette.tinta2,
            )
        }
    }
}
