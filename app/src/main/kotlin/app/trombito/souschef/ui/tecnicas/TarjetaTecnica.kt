package app.trombito.souschef.ui.tecnicas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.domain.DisposicionLista
import app.trombito.souschef.receta.Tecnica
import app.trombito.souschef.ui.comun.BloqueLateral
import app.trombito.souschef.ui.comun.Etiqueta
import app.trombito.souschef.ui.comun.LINEAS_RESUMEN_CLASICA
import app.trombito.souschef.ui.comun.MarcoTarjeta
import app.trombito.souschef.ui.comun.ResumenTarjeta
import app.trombito.souschef.ui.comun.RotuloBloque
import app.trombito.souschef.ui.comun.SEPARADOR_META
import app.trombito.souschef.ui.comun.TituloClasico
import app.trombito.souschef.ui.comun.TituloCompacto
import app.trombito.souschef.ui.comun.colorTono
import app.trombito.souschef.ui.theme.Espacio

private val TAMANO_ICONO = 28.dp

/** Tarjeta de una técnica en la disposición elegida en Ajustes; el tono y el icono vienen de su categoría. */
@Composable
fun TarjetaTecnica(
    tecnica: Tecnica,
    disposicion: DisposicionLista,
    onAbrir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val categoria = CategoriaTecnica.de(tecnica.categoria)
    val color = colorTono(categoria.tono)
    when (disposicion) {
        DisposicionLista.TINTE -> {
            MarcoTarjeta(
                disposicion = disposicion,
                color = color,
                onClick = onAbrir,
                modifier = modifier,
                bloque = {
                    BloqueLateral(Modifier.padding(horizontal = Espacio.xs)) {
                        Icon(
                            categoria.icono.vector,
                            contentDescription = null,
                            modifier = Modifier.size(TAMANO_ICONO),
                            tint = color.acento,
                        )
                        RotuloBloque(
                            textoPasos(tecnica),
                            modifier = Modifier.padding(top = Espacio.xs),
                            textAlign = TextAlign.Center,
                        )
                    }
                },
            ) {
                CuerpoCompacto(tecnica, linea = tecnica.categoria.conInicialMayuscula())
            }
        }

        DisposicionLista.FRANJA -> {
            MarcoTarjeta(disposicion, color, onAbrir, modifier) {
                val linea =
                    listOf(tecnica.categoria.conInicialMayuscula(), textoPasos(tecnica))
                        .filter { it.isNotEmpty() }
                        .joinToString(SEPARADOR_META)
                CuerpoCompacto(tecnica, linea)
            }
        }

        DisposicionLista.CLASICA -> {
            MarcoTarjeta(disposicion, color, onAbrir, modifier) { CuerpoClasico(tecnica) }
        }
    }
}

@Composable
private fun textoPasos(tecnica: Tecnica): String {
    val n = tecnica.pasos.size
    return pluralStringResource(R.plurals.tecnica_pasos_n, n, n)
}

/** Título, resumen en una línea y una línea de categoría (con los pasos en la franja). */
@Composable
private fun CuerpoCompacto(
    tecnica: Tecnica,
    linea: String,
) {
    Column(Modifier.padding(Espacio.m)) {
        TituloCompacto(tecnica.titulo)
        ResumenTarjeta(tecnica.resumen)
        if (linea.isNotEmpty()) {
            Text(
                linea,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CuerpoClasico(tecnica: Tecnica) {
    Column(Modifier.padding(Espacio.l), verticalArrangement = Arrangement.spacedBy(Espacio.s)) {
        TituloClasico(tecnica.titulo)
        ResumenTarjeta(tecnica.resumen, maxLines = LINEAS_RESUMEN_CLASICA)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (tecnica.categoria.isNotBlank()) Etiqueta(tecnica.categoria)
            Etiqueta(textoPasos(tecnica))
        }
    }
}
