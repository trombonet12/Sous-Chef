package app.souschef.ui.comun

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.souschef.ui.shell.Icono
import app.souschef.ui.theme.FichaPalette
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.svg.SvgDecoder

/** Números tabulares para relojes, cantidades y contadores. */
val Tabular = TextStyle(fontFeatureSettings = "tnum")

/** Sección de una hoja de papel: título en versalitas, filete y contenido. */
@Composable
fun SeccionPapel(
    titulo: String,
    modifier: Modifier = Modifier,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column {
            Text(
                titulo.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = FichaPalette.meta,
                letterSpacing = 1.2.sp,
            )
            HorizontalDivider(Modifier.padding(top = 4.dp), color = FichaPalette.linea)
        }
        contenido()
    }
}

/** Celda de la fila de datos: valor grande y rótulo pequeño. */
@Composable
fun CeldaDato(
    grande: String,
    pequeno: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(end = 24.dp)) {
        Text(grande, style = MaterialTheme.typography.titleMedium.merge(Tabular))
        Text(pequeno, style = MaterialTheme.typography.labelMedium, color = FichaPalette.meta)
    }
}

/** Botones menos/más con un valor en medio; cada botón mide al menos 48 dp. */
@Composable
fun PasosMasMenos(
    valor: String,
    descMenos: String,
    descMas: String,
    onMenos: () -> Unit,
    onMas: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onMenos) { Icon(Icono.Menos.vector, contentDescription = descMenos) }
        Text(
            valor,
            modifier = Modifier.widthIn(min = 56.dp),
            style = MaterialTheme.typography.titleMedium.merge(Tabular),
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = onMas) { Icon(Icono.Anadir.vector, contentDescription = descMas) }
    }
}

/** Línea con `<b>` convertida en texto con negritas. */
fun textoRico(texto: String): AnnotatedString =
    buildAnnotatedString {
        conNegritas(texto).forEach { t ->
            if (t.negrita) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(t.texto) }
            } else {
                append(t.texto)
            }
        }
    }

/** Dibuja el SVG (texto) de un corte con coil-svg. */
@Composable
fun CorteSvg(
    svg: String,
    descripcion: String,
    modifier: Modifier = Modifier,
) {
    val contexto = LocalContext.current
    val peticion =
        remember(svg) {
            ImageRequest
                .Builder(contexto)
                .data(svg.toByteArray())
                .memoryCacheKey(svg.hashCode().toString())
                .decoderFactory(SvgDecoder.Factory())
                .build()
        }
    AsyncImage(model = peticion, contentDescription = descripcion, modifier = modifier)
}
