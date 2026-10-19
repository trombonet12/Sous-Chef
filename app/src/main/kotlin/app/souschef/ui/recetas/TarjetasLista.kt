@file:Suppress("TooManyFunctions") // tres tarjetas y sus piezas comunes

package app.souschef.ui.recetas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.souschef.R
import app.souschef.domain.DisposicionLista
import app.souschef.receta.Escalado
import app.souschef.receta.Tags
import app.souschef.ui.ficha.Servir
import app.souschef.ui.shell.Icono
import app.souschef.ui.theme.Espacio

private val ANCHO_BLOQUE_TIEMPO = 64.dp
private val GROSOR_SEPARADOR = 1.dp
private val ANCHO_FRANJA = 5.dp
private val SIN_ELEVACION = 0.dp
private const val SEPARADOR = " · "
private const val SIN_PROTEINA = "ninguna"
private const val LINEAS_TITULO = 2
private const val LINEAS_PIE = 2
private const val LINEAS_DESCRIPCION_CLASICA = 3

/** Tarjeta de la lista de recetas en la disposición elegida en Ajustes. */
@Composable
fun TarjetaLista(
    tarjeta: TarjetaReceta,
    disposicion: DisposicionLista,
    onFavorita: () -> Unit,
    onAbrir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (disposicion) {
        DisposicionLista.TINTE -> TarjetaTinte(tarjeta, onFavorita, onAbrir, modifier)
        DisposicionLista.FRANJA -> TarjetaFranja(tarjeta, onFavorita, onAbrir, modifier)
        DisposicionLista.CLASICA -> TarjetaClasica(tarjeta, onFavorita, onAbrir, modifier)
    }
}

// ---- TINTE: fondo del color de la base, tiempo y hora de empezar a la izquierda ----

@Composable
private fun TarjetaTinte(
    tarjeta: TarjetaReceta,
    onFavorita: () -> Unit,
    onAbrir: () -> Unit,
    modifier: Modifier,
) {
    val color = colorBase(tarjeta.familia)
    Card(
        onClick = onAbrir,
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = color.tinte,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = SIN_ELEVACION),
    ) {
        Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
            BloqueTiempo(tarjeta, Modifier.width(ANCHO_BLOQUE_TIEMPO))
            Box(
                Modifier
                    .fillMaxHeight()
                    .padding(vertical = Espacio.s)
                    .width(GROSOR_SEPARADOR)
                    .background(color.acento),
            )
            CuerpoTinte(tarjeta, onFavorita, Modifier.weight(1f))
        }
    }
}

@Composable
private fun BloqueTiempo(
    tarjeta: TarjetaReceta,
    modifier: Modifier = Modifier,
) {
    val minutos = Escalado.minutos(tarjeta.receta.totalEfectivo)
    val hora = Servir.formato(tarjeta.inicio)
    val descripcion =
        stringResource(R.string.tarjeta_minutos, minutos) + ", " + stringResource(R.string.lista_empieza, hora)
    Column(
        modifier.padding(vertical = Espacio.m).clearAndSetSemantics { contentDescription = descripcion },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            minutos,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.lista_min),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(hora, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CuerpoTinte(
    tarjeta: TarjetaReceta,
    onFavorita: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val baseProteina = baseYProteina(tarjeta.receta.tags)
    Column(modifier.padding(start = Espacio.m, bottom = Espacio.s)) {
        TituloConEstrella(tarjeta, onFavorita, MaterialTheme.typography.titleMedium)
        Column(Modifier.padding(end = Espacio.m)) {
            Descripcion(tarjeta.receta.descripcion)
            if (baseProteina.isNotEmpty()) {
                Text(
                    baseProteina,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                lineaDetalle(tarjeta, conBase = false),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = LINEAS_PIE,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ---- FRANJA: tarjeta clara con una franja de color a la izquierda ----

@Composable
private fun TarjetaFranja(
    tarjeta: TarjetaReceta,
    onFavorita: () -> Unit,
    onAbrir: () -> Unit,
    modifier: Modifier,
) {
    Card(
        onClick = onAbrir,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.fillMaxHeight().width(ANCHO_FRANJA).background(colorBase(tarjeta.familia).acento))
            CuerpoFranja(tarjeta, onFavorita, Modifier.weight(1f))
        }
    }
}

@Composable
private fun CuerpoFranja(
    tarjeta: TarjetaReceta,
    onFavorita: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tiempo =
        stringResource(R.string.tarjeta_minutos, Escalado.minutos(tarjeta.receta.totalEfectivo)) +
            SEPARADOR + stringResource(R.string.lista_empieza, Servir.formato(tarjeta.inicio))
    Column(modifier.padding(start = Espacio.m, bottom = Espacio.m)) {
        TituloConEstrella(tarjeta, onFavorita, MaterialTheme.typography.titleMedium)
        Column(Modifier.padding(end = Espacio.m)) {
            Descripcion(tarjeta.receta.descripcion)
            Text(tiempo, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(
                lineaDetalle(tarjeta, conBase = true),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = LINEAS_PIE,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ---- CLÁSICA: la tarjeta de siempre, con la huella de carriles ----

@Composable
private fun TarjetaClasica(
    tarjeta: TarjetaReceta,
    onFavorita: () -> Unit,
    onAbrir: () -> Unit,
    modifier: Modifier,
) {
    val receta = tarjeta.receta
    Card(onClick = onAbrir, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = Espacio.l, top = Espacio.s, end = Espacio.xs, bottom = Espacio.m)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    receta.titulo,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                BotonFavorita(tarjeta.favorita, receta.titulo, onFavorita)
            }
            Column(Modifier.padding(end = Espacio.m), verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
                if (receta.descripcion.isNotBlank()) {
                    Text(
                        receta.descripcion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = LINEAS_DESCRIPCION_CLASICA,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Huella(receta)
                PieTarjeta(tarjeta)
            }
        }
    }
}

/** Una sola línea de metadatos (tiempo, fuegos, última vez) de la tarjeta clásica. */
@Composable
private fun PieTarjeta(tarjeta: TarjetaReceta) {
    val inicio =
        listOfNotNull(
            stringResource(R.string.tarjeta_minutos, Escalado.minutos(tarjeta.receta.totalEfectivo)),
            textoFuegos(tarjeta),
        ).joinToString(SEPARADOR)
    val hace = tarjeta.hace.texto()
    val resalte = colorHace(tarjeta)
    val texto =
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(inicio) }
            append(SEPARADOR)
            withStyle(SpanStyle(color = resalte)) { append(hace) }
        }
    Text(
        texto,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = LINEAS_PIE,
        overflow = TextOverflow.Ellipsis,
    )
}

// ---- Piezas comunes ----

@Composable
private fun TituloConEstrella(
    tarjeta: TarjetaReceta,
    onFavorita: () -> Unit,
    estilo: TextStyle,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            tarjeta.receta.titulo,
            style = estilo,
            fontWeight = FontWeight.SemiBold,
            maxLines = LINEAS_TITULO,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        BotonFavorita(tarjeta.favorita, tarjeta.receta.titulo, onFavorita)
    }
}

/** Descripción de la receta en una sola línea y en gris. */
@Composable
private fun Descripcion(texto: String) {
    if (texto.isNotBlank()) {
        Text(
            texto,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BotonFavorita(
    favorita: Boolean,
    titulo: String,
    onClick: () -> Unit,
) {
    val descripcion =
        stringResource(if (favorita) R.string.favorita_quitar else R.string.favorita_marcar, titulo)
    IconButton(onClick = onClick) {
        Icon(
            if (favorita) Icono.EstrellaLlena.vector else Icono.Estrella.vector,
            contentDescription = descripcion,
            tint = if (favorita) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun textoFuegos(tarjeta: TarjetaReceta): String? =
    if (tarjeta.fuegos > 0) pluralStringResource(R.plurals.tarjeta_fuegos, tarjeta.fuegos, tarjeta.fuegos) else null

@Composable
private fun colorHace(tarjeta: TarjetaReceta): Color =
    if (tarjeta.hace.resaltado) MaterialTheme.colorScheme.error else Color.Unspecified

/** «[Base · proteína · ]N fuegos · horno · hace X», con «hace X» en rojo si lleva tiempo sin cocinarse. */
@Composable
private fun lineaDetalle(
    tarjeta: TarjetaReceta,
    conBase: Boolean,
): AnnotatedString {
    val partes =
        listOfNotNull(
            baseYProteina(tarjeta.receta.tags).takeIf { conBase && it.isNotEmpty() },
            textoFuegos(tarjeta),
            stringResource(R.string.lista_horno).takeIf { tarjeta.conHorno },
        )
    val hace = tarjeta.hace.texto()
    val resalte = colorHace(tarjeta)
    return buildAnnotatedString {
        partes.forEach {
            append(it)
            append(SEPARADOR)
        }
        withStyle(SpanStyle(color = resalte)) { append(hace) }
    }
}

/** «Pasta · pollo»: base y proteína con la primera letra en mayúscula; sin la proteína si es «ninguna». */
internal fun baseYProteina(tags: Tags): String {
    val base = tags.base.orEmpty().trim()
    val proteina =
        tags.proteina
            .orEmpty()
            .trim()
            .takeUnless { it.equals(SIN_PROTEINA, ignoreCase = true) }
            .orEmpty()
    return listOf(base, proteina)
        .filter { it.isNotEmpty() }
        .joinToString(SEPARADOR)
        .replaceFirstChar { it.uppercase() }
}
