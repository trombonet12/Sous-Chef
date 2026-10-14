package app.souschef.ui.tecnicas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.souschef.R
import app.souschef.receta.Tecnica
import app.souschef.ui.comun.BarraTitulo
import app.souschef.ui.comun.CeldaDato
import app.souschef.ui.comun.EstadoVacio
import app.souschef.ui.comun.SeccionPapel
import app.souschef.ui.comun.Tabular
import app.souschef.ui.comun.textoRico
import app.souschef.ui.ficha.CorteVista
import app.souschef.ui.theme.FichaPalette

@Composable
fun TecnicaScreen(
    onAtras: () -> Unit,
    onReceta: (String) -> Unit,
    viewModel: TecnicaViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val tecnica = estado.tecnica
    when {
        estado.cargando -> {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        }

        tecnica == null -> {
            Column {
                BarraTitulo("", onAtras)
                EstadoVacio(stringResource(R.string.tecnica_no_encontrada), null)
            }
        }

        else -> {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                BarraTitulo(tecnica.titulo, onAtras)
                EnlacesRecetas(estado, onReceta)
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = FichaPalette.papel,
                    contentColor = FichaPalette.tinta,
                ) {
                    HojaTecnica(tecnica)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EnlacesRecetas(
    estado: TecnicaUiState,
    onReceta: (String) -> Unit,
) {
    if (estado.recetas.isNotEmpty()) {
        FlowRow(
            Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            estado.recetas.forEach { r ->
                AssistChip(
                    onClick = { onReceta(r.slug) },
                    label = { Text(stringResource(R.string.tecnica_receta, r.titulo)) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HojaTecnica(tecnica: Tecnica) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(tecnica.titulo, style = MaterialTheme.typography.headlineSmall)
        if (tecnica.resumen.isNotBlank()) {
            Text(
                tecnica.resumen,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = FichaPalette.tinta2,
            )
        }
        FlowRow(Modifier.padding(top = 8.dp)) {
            val n = tecnica.pasos.size
            if (tecnica.categoria.isNotBlank()) CeldaDato(tecnica.categoria, "")
            CeldaDato(n.toString(), pluralStringResource(R.plurals.tecnica_unidad_paso, n))
        }
        PasosTecnica(tecnica)
        if (tecnica.cortes.isNotEmpty()) {
            SeccionPapel(stringResource(R.string.tecnica_sec_corte)) { tecnica.cortes.forEach { CorteVista(it) } }
        }
        if (tecnica.consejos.isNotEmpty()) {
            SeccionPapel(stringResource(R.string.tecnica_sec_consejos)) {
                tecnica.consejos.forEach {
                    Text(stringResource(R.string.tecnica_consejo, it), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        tecnica.pie?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = FichaPalette.meta,
            )
        }
    }
}

@Composable
private fun PasosTecnica(tecnica: Tecnica) {
    if (tecnica.pasos.isEmpty()) return
    SeccionPapel(stringResource(R.string.tecnica_sec_como)) {
        tecnica.pasos.forEachIndexed { i, paso ->
            Row(Modifier.padding(bottom = 8.dp)) {
                Text("${i + 1}.", Modifier.width(32.dp), style = MaterialTheme.typography.titleSmall.merge(Tabular))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(paso.t, style = MaterialTheme.typography.titleSmall)
                    if (paso.p.isNotBlank()) Text(textoRico(paso.p), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
