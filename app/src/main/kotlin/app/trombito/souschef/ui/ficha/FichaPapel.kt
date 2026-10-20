@file:Suppress("TooManyFunctions") // una función por sección de la ficha

package app.trombito.souschef.ui.ficha

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.receta.BloqueMep
import app.trombito.souschef.receta.Escalado
import app.trombito.souschef.receta.Ingrediente
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.ui.comun.CeldaDato
import app.trombito.souschef.ui.comun.LocalCocina
import app.trombito.souschef.ui.comun.PasosMasMenos
import app.trombito.souschef.ui.comun.SeccionPapel
import app.trombito.souschef.ui.comun.Tabular
import app.trombito.souschef.ui.comun.nivel
import app.trombito.souschef.ui.comun.textoRico
import app.trombito.souschef.ui.theme.FichaPalette

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PortadaFicha(
    receta: Receta,
    raciones: Int,
) {
    Column(Modifier.padding(top = 4.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(receta.titulo, style = MaterialTheme.typography.headlineSmall)
        if (receta.descripcion.isNotBlank()) {
            Text(
                receta.descripcion,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = FichaPalette.tinta2,
            )
        }
        FlowRow(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            CeldaDato(raciones.toString(), pluralStringResource(R.plurals.ficha_unidad_racion, raciones))
            receta.datos.filter { it.isNotEmpty() }.forEach { CeldaDato(it[0], it.getOrElse(1) { "" }) }
        }
    }
}

@Composable
fun IngredientesFicha(
    receta: Receta,
    estado: FichaUiState,
    onRaciones: (Int) -> Unit,
) {
    if (receta.grupos.isEmpty()) return
    val raciones = estado.raciones
    val permitidas = estado.racionesPermitidas
    SeccionPapel(stringResource(R.string.ficha_sec_ingredientes)) {
        if (permitidas.size > 1) {
            val i = permitidas.indexOf(raciones)
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
                    onMenos = { permitidas.getOrNull(i - 1)?.let(onRaciones) },
                    onMas = { permitidas.getOrNull(i + 1)?.let(onRaciones) },
                )
            }
        }
        val f = Escalado.factor(raciones, receta.raciones)
        var porZona by rememberSaveable(receta.slug) { mutableStateOf(false) }
        SelectorIngredientes(porZona) { porZona = it }
        if (porZona) {
            receta.grupos.forEach { g ->
                Text(g.n, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                g.items.forEach { FilaIngrediente(it, f) }
            }
        } else {
            Escalado.listaGeneral(receta).forEach { FilaIngrediente(it, f) }
        }
        receta.notaEscala?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = FichaPalette.meta,
            )
        }
        if (estado.fueraDeBase) NotaFicha(stringResource(R.string.ficha_aviso_fuera_de_base, receta.raciones))
        if (estado.ajustadoAlRango && permitidas.isNotEmpty()) {
            NotaFicha(stringResource(R.string.ficha_aviso_rango, permitidas.first(), permitidas.last()))
        }
    }
}

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

@Composable
private fun SelectorIngredientes(
    porZona: Boolean,
    onPorZona: (Boolean) -> Unit,
) {
    val colores =
        SegmentedButtonDefaults.colors(
            activeContainerColor = FichaPalette.papel2,
            activeContentColor = FichaPalette.tinta,
            activeBorderColor = FichaPalette.tinta3,
            inactiveContainerColor = FichaPalette.papel,
            inactiveContentColor = FichaPalette.tinta2,
            inactiveBorderColor = FichaPalette.tinta3,
        )
    val opciones = listOf(R.string.ficha_ing_todo_junto, R.string.ficha_ing_por_zona)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        opciones.forEachIndexed { i, texto ->
            SegmentedButton(
                selected = porZona == (i == 1),
                onClick = { onPorZona(i == 1) },
                shape = SegmentedButtonDefaults.itemShape(i, opciones.size),
                colors = colores,
            ) { Text(stringResource(texto)) }
        }
    }
}

@Composable
private fun FilaIngrediente(
    item: Ingrediente,
    f: Double,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            Escalado.cantidad(item, f),
            Modifier.width(84.dp).padding(end = 12.dp),
            style = MaterialTheme.typography.bodyMedium.merge(Tabular),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
        )
        Text(item.n, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
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
    val cocina = LocalCocina.current
    val partes =
        buildList {
            add((receta.carril(paso.carril)?.nombre ?: paso.carril).uppercase())
            add(stringResource(R.string.ficha_minutos_meta, Escalado.minutos(paso.min)).uppercase())
            add(
                paso.f?.let { stringResource(R.string.ficha_fuego_meta, cocina.nivel(it).uppercase()) }
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
