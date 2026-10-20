@file:Suppress("MatchingDeclarationName")

package app.trombito.souschef.ui.comun

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.trombito.souschef.ui.shell.Icono
import app.trombito.souschef.ui.theme.Espacio

/** Un cuadradito de la rejilla: [recuento] ya viene formateado ("12 recetas"). */
data class CategoriaVista(
    val clave: String,
    val nombre: String,
    val icono: Icono,
    val color: ColorFamilia,
    val recuento: String,
)

private val LADO_MINIMO = 96.dp
private val TAMANO_ICONO = 28.dp
private const val LINEAS_NOMBRE = 2

/**
 * Rejilla de categorías en filas de [columnas] cuadraditos (sin LazyGrid: va dentro de un item de una LazyColumn).
 * Cada cuadradito es al menos cuadrado y crece en alto si el texto (letra grande) lo necesita; las filas se igualan.
 */
@Composable
fun RejillaCategorias(
    categorias: List<CategoriaVista>,
    onAbrir: (String) -> Unit,
    modifier: Modifier = Modifier,
    columnas: Int = 3,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val lado = maxOf((maxWidth - Espacio.s * (columnas - 1)) / columnas, LADO_MINIMO)
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
            categorias.chunked(columnas).forEach { fila ->
                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(Espacio.s),
                ) {
                    fila.forEach { categoria ->
                        Cuadradito(categoria, onAbrir, Modifier.weight(1f).heightIn(min = lado).fillMaxHeight())
                    }
                    repeat(columnas - fila.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun Cuadradito(
    categoria: CategoriaVista,
    onAbrir: (String) -> Unit,
    modifier: Modifier,
) {
    val descripcion = "${categoria.nombre}, ${categoria.recuento}"
    Surface(
        onClick = { onAbrir(categoria.clave) },
        modifier =
            modifier
                .testTag("categoria-${categoria.clave}")
                .clearAndSetSemantics {
                    contentDescription = descripcion
                    role = Role.Button
                    onClick {
                        onAbrir(categoria.clave)
                        true
                    }
                },
        shape = CardDefaults.shape,
        color = categoria.color.tinte,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(Espacio.s),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                categoria.icono.vector,
                contentDescription = null,
                modifier = Modifier.size(TAMANO_ICONO),
                tint = categoria.color.acento,
            )
            Text(
                categoria.nombre,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = LINEAS_NOMBRE,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = Espacio.xs),
            )
            Text(
                categoria.recuento,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
