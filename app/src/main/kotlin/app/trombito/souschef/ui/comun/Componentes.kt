package app.trombito.souschef.ui.comun

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import app.trombito.souschef.receta.Carril
import app.trombito.souschef.ui.shell.Icono

/** Color de un carril (`#RRGGBB`); gris si el dato no se entiende. */
fun colorCarril(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color(0xFFB0A899))

private const val SATURACION_NEUTRO = 0.2f
private const val HEX_BASE = 16
private const val BYTE = 8
private val HEX_RGB = Regex("#([0-9A-Fa-f]{6})")
private val NEUTROS_PLAN = intArrayOf(0x7FB7D6, 0xB79BE0, 0x6FCBB5, 0xE58FB0)

/**
 * `0xRRGGBB` del carril [id] en cualquier diagrama de tiempos (lista, ficha, cocina y papel): el color del JSON si
 * tiene color propio; los neutros (grises o beige, saturación < 0,2) reciben uno de la paleta según su posición en
 * [carriles] (los carriles normalizados del motor), igual que en la cocina. Es Kotlin puro: lo usa también el HTML.
 */
fun rgbCarrilPlan(
    carriles: List<Carril>,
    id: String,
): Int {
    val i = carriles.indexOfFirst { it.id == id }
    val rgb =
        HEX_RGB
            .matchEntire(carriles.getOrNull(i)?.color.orEmpty())
            ?.groupValues
            ?.get(1)
            ?.toInt(HEX_BASE)
    return if (rgb != null && saturacion(rgb) >= SATURACION_NEUTRO) {
        rgb
    } else {
        NEUTROS_PLAN[i.coerceAtLeast(0) % NEUTROS_PLAN.size]
    }
}

private fun saturacion(rgb: Int): Float {
    val canales = listOf(rgb shr (2 * BYTE) and 0xFF, rgb shr BYTE and 0xFF, rgb and 0xFF)
    val max = canales.max()
    return if (max == 0) 0f else (max - canales.min()) / max.toFloat()
}

/** El [rgbCarrilPlan] como color de Compose. */
fun colorPlan(
    carriles: List<Carril>,
    id: String,
): Color = Color(0xFF000000.toInt() or rgbCarrilPlan(carriles, id))

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
