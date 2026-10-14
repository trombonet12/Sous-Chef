package app.souschef.ui.ficha

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.souschef.R
import app.souschef.receta.BloqueMep
import app.souschef.receta.Escalado
import app.souschef.receta.Paso
import app.souschef.receta.Receta
import app.souschef.ui.comun.CeldaDato
import app.souschef.ui.comun.PasosMasMenos
import app.souschef.ui.comun.SeccionPapel
import app.souschef.ui.comun.Tabular
import app.souschef.ui.comun.textoRico
import app.souschef.ui.theme.FichaPalette

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PortadaFicha(
    receta: Receta,
    raciones: Int,
) {
    Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(receta.titulo, style = MaterialTheme.typography.headlineLarge)
        if (receta.descripcion.isNotBlank()) {
            Text(
                receta.descripcion,
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic,
                color = FichaPalette.tinta2,
            )
        }
        FlowRow(Modifier.padding(top = 8.dp)) {
            CeldaDato(raciones.toString(), pluralStringResource(R.plurals.ficha_unidad_racion, raciones))
            receta.datos.filter { it.isNotEmpty() }.forEach { CeldaDato(it[0], it.getOrElse(1) { "" }) }
        }
    }
}

@Composable
fun IngredientesFicha(
    receta: Receta,
    raciones: Int,
    onRaciones: (Int) -> Unit,
) {
    if (receta.grupos.isEmpty()) return
    SeccionPapel(stringResource(R.string.ficha_sec_ingredientes)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(
                stringResource(R.string.ficha_raciones_etiqueta).uppercase(),
                Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = FichaPalette.meta,
            )
            PasosMasMenos(
                valor = stringResource(R.string.ficha_raciones_valor, raciones),
                descMenos = stringResource(R.string.ficha_raciones_menos),
                descMas = stringResource(R.string.ficha_raciones_mas),
                onMenos = { onRaciones(raciones - 1) },
                onMas = { onRaciones(raciones + 1) },
            )
        }
        val f = Escalado.factor(raciones, receta.raciones)
        receta.grupos.forEach { g ->
            Text(g.n, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 6.dp))
            g.items.forEach { item ->
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        Escalado.cantidad(item, f),
                        Modifier.width(96.dp).padding(end = 12.dp),
                        style = MaterialTheme.typography.bodyMedium.merge(Tabular),
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End,
                    )
                    Text(item.n, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        receta.notaEscala?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = FichaPalette.meta,
            )
        }
    }
}

@Composable
fun MepFicha(receta: Receta) {
    if (receta.mep.isEmpty()) return
    SeccionPapel(stringResource(R.string.ficha_sec_mep)) {
        receta.mep.forEach { BloqueMepVista(it) }
    }
}

@Composable
private fun BloqueMepVista(bloque: BloqueMep) {
    Column(Modifier.padding(bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(bloque.t, style = MaterialTheme.typography.titleSmall)
        bloque.porq?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = FichaPalette.tinta2)
        }
        bloque.l.forEachIndexed { i, linea ->
            val marca = if (bloque.orden) "${i + 1}. " else "• "
            Row {
                Text(marca, Modifier.width(28.dp), style = MaterialTheme.typography.bodyMedium.merge(Tabular))
                Text(textoRico(linea), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun PasosFicha(receta: Receta) {
    if (receta.pasos.isEmpty()) return
    SeccionPapel(stringResource(R.string.ficha_sec_pasos)) {
        receta.pasos.forEachIndexed { i, paso -> PasoVista(i + 1, paso, receta) }
    }
}

@Composable
private fun PasoVista(
    numero: Int,
    paso: Paso,
    receta: Receta,
) {
    Row(Modifier.padding(bottom = 10.dp)) {
        Text(
            "$numero.",
            Modifier.width(32.dp),
            style = MaterialTheme.typography.titleSmall.merge(Tabular),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(paso.t, style = MaterialTheme.typography.titleSmall)
            Text(
                lineaMetaPaso(paso, receta),
                style = MaterialTheme.typography.labelMedium,
                color = FichaPalette.meta,
            )
            (paso.p ?: paso.nota)?.takeIf { it.isNotBlank() }?.let {
                Text(textoRico(it), style = MaterialTheme.typography.bodyMedium)
            }
            paso.hitosValidos.forEach { h ->
                Text(
                    "${formatoHito(h.en)} ${h.t}",
                    style = MaterialTheme.typography.bodySmall.merge(Tabular),
                    color = FichaPalette.tinta2,
                )
            }
        }
    }
}

/** "SARTÉN · 12 MIN · FUEGO 8 · SIN TAPA". */
@Composable
fun lineaMetaPaso(
    paso: Paso,
    receta: Receta,
): String {
    val partes =
        buildList {
            add((receta.carril(paso.carril)?.nombre ?: paso.carril).uppercase())
            add(stringResource(R.string.ficha_minutos_meta, Escalado.minutos(paso.min)).uppercase())
            add(
                paso.f?.let { stringResource(R.string.ficha_fuego_meta, it) }
                    ?: stringResource(R.string.ficha_sin_fuego),
            )
            paso.tapa?.takeIf { it.isNotBlank() }?.let { add(it.uppercase()) }
        }
    return partes.joinToString(" · ")
}

@Composable
fun TecnicaFicha(receta: Receta) {
    if (receta.cortes.isEmpty() && receta.porque.isEmpty()) return
    SeccionPapel(stringResource(R.string.ficha_sec_tecnica)) {
        receta.cortes.forEach { CorteVista(it) }
        receta.porque.filter { it.isNotEmpty() }.forEach { par ->
            Text(par[0], style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
            par.getOrNull(1)?.let { Text(textoRico(it), style = MaterialTheme.typography.bodyMedium) }
        }
    }
}
