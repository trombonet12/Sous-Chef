package app.trombito.souschef.ui.comun

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.ui.shell.Icono

/** Color de un carril (`#RRGGBB`); gris si el dato no se entiende. */
fun colorCarril(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color(0xFFB0A899))

@Composable
fun CampoBusqueda(
    inicial: String,
    etiqueta: String,
    onCambio: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var texto by rememberSaveable { mutableStateOf(inicial) }
    OutlinedTextField(
        value = texto,
        onValueChange = {
            texto = it
            onCambio(it)
        },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(etiqueta) },
        leadingIcon = { Icon(Icono.Buscar.vector, contentDescription = null) },
        trailingIcon =
            if (texto.isEmpty()) {
                null
            } else {
                {
                    IconButton(
                        onClick = {
                            texto = ""
                            onCambio("")
                        },
                    ) {
                        Icon(Icono.Cerrar.vector, contentDescription = stringResource(R.string.limpiar_busqueda))
                    }
                }
            },
        shape = MaterialTheme.shapes.small,
    )
}

/** Chips "Todas" + uno por valor; tocar el activo lo desactiva. */
@Composable
fun ChipsFiltro(
    opciones: List<Pair<String, Int>>,
    activa: String?,
    total: Int,
    onElegir: (String?) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
        item {
            FilterChip(
                selected = activa == null,
                onClick = { onElegir(null) },
                label = { Text(stringResource(R.string.filtro_todas_n, total)) },
            )
        }
        items(opciones, key = { it.first }) { (valor, n) ->
            FilterChip(
                selected = activa == valor,
                onClick = { onElegir(if (activa == valor) null else valor) },
                label = { Text(stringResource(R.string.filtro_n, valor, n)) },
            )
        }
    }
}

enum class Tono { Neutro, Teja, Oliva, Ambar }

/** Etiqueta pequeña de solo lectura (no es interactiva). */
@Composable
fun Etiqueta(
    texto: String,
    modifier: Modifier = Modifier,
    tono: Tono = Tono.Neutro,
    negrita: Boolean = false,
) {
    val esquema = MaterialTheme.colorScheme
    val (fondo, tinta) =
        when (tono) {
            Tono.Neutro -> esquema.surfaceVariant to esquema.onSurfaceVariant
            Tono.Teja -> esquema.errorContainer to esquema.onErrorContainer
            Tono.Oliva -> esquema.tertiaryContainer to esquema.onTertiaryContainer
            Tono.Ambar -> esquema.primaryContainer to esquema.onPrimaryContainer
        }
    Surface(modifier = modifier, shape = MaterialTheme.shapes.extraSmall, color = fondo, contentColor = tinta) {
        Text(
            texto,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (negrita) FontWeight.ExtraBold else FontWeight.SemiBold,
        )
    }
}

@Composable
fun BarraTitulo(
    titulo: String,
    onAtras: () -> Unit,
    modifier: Modifier = Modifier,
    acciones: @Composable RowScope.() -> Unit = {},
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onAtras) {
            Icon(Icono.Atras.vector, contentDescription = stringResource(R.string.atras))
        }
        Text(
            titulo,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 4.dp).weight(1f),
        )
        acciones()
    }
}

@Composable
fun EstadoVacio(
    titulo: String,
    detalle: String?,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            if (detalle != null) {
                Text(
                    detalle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
