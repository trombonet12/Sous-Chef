package app.trombito.souschef.ui.comun

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import app.trombito.souschef.ui.theme.Espacio

// Piezas y constantes que repiten las tarjetas de Recetas, Historial y Técnicas (ver MarcoTarjeta).

/** Separador de las líneas de metadatos de las tarjetas. */
internal const val SEPARADOR_META = " · "

/** Líneas máximas del título de las tarjetas compactas (Tinte y Franja). */
internal const val LINEAS_TITULO_TARJETA = 2

/** Líneas máximas de la descripción o el resumen en la disposición Clásica. */
internal const val LINEAS_RESUMEN_CLASICA = 3

/**
 * Columna del bloque lateral de la disposición Tinte, centrada y con aire arriba y abajo. Con [descripcion] el bloque
 * se lee entero de una vez en TalkBack (se descartan los textos de dentro).
 */
@Composable
internal fun BloqueLateral(
    modifier: Modifier = Modifier,
    descripcion: String? = null,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    val base = modifier.padding(vertical = Espacio.m)
    Column(
        if (descripcion != null) base.clearAndSetSemantics { contentDescription = descripcion } else base,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = contenido,
    )
}

/** Cifra grande del bloque lateral (minutos o día del mes). */
@Composable
internal fun CifraBloque(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
    )
}

/** Rótulo pequeño y gris bajo la cifra del bloque lateral («min», mes). */
@Composable
internal fun RotuloBloque(
    texto: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
) {
    Text(
        texto,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = textAlign,
    )
}

/** Dato mediano y gris del bloque lateral (hora, minutos reales). */
@Composable
internal fun DatoBloque(
    texto: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
) {
    Text(
        texto,
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = textAlign,
    )
}

/** Título de las tarjetas Tinte y Franja. */
@Composable
internal fun TituloCompacto(
    texto: String,
    modifier: Modifier = Modifier,
) {
    Text(
        texto,
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        maxLines = LINEAS_TITULO_TARJETA,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Título de la disposición Clásica. */
@Composable
internal fun TituloClasico(
    texto: String,
    modifier: Modifier = Modifier,
) {
    Text(texto, modifier = modifier, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
}

/** Descripción o resumen en gris; no pinta nada si está en blanco. */
@Composable
internal fun ResumenTarjeta(
    texto: String,
    maxLines: Int = 1,
) {
    if (texto.isNotBlank()) {
        Text(
            texto,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
