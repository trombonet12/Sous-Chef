package app.souschef.ui.ficha

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.souschef.R
import app.souschef.receta.Receta
import app.souschef.ui.comun.BarraTitulo
import app.souschef.ui.comun.EstadoVacio
import app.souschef.ui.comun.PasosMasMenos
import app.souschef.ui.comun.Tabular
import app.souschef.ui.shell.Icono
import app.souschef.ui.theme.FichaPalette

@Composable
fun FichaScreen(
    onAtras: () -> Unit,
    onTecnica: (String) -> Unit,
    onCocinar: (raciones: Int, servir: String) -> Unit,
    viewModel: FichaViewModel = hiltViewModel(),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val receta = estado.receta
    // Con la cocinada viva, salir de la ficha es abandonarla (con confirmación); ir a una técnica no.
    var confirmarSalida by remember { mutableStateOf(false) }
    val salir = { if (estado.cocinando != null) confirmarSalida = true else onAtras() }
    BackHandler(enabled = estado.cocinando != null) { confirmarSalida = true }
    if (confirmarSalida) {
        DialogoAbandonarDesdeFicha(
            onSi = {
                confirmarSalida = false
                viewModel.abandonarCocinada()
                onAtras()
            },
            onNo = { confirmarSalida = false },
        )
    }
    when {
        estado.cargando -> {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        }

        receta == null -> {
            Column {
                BarraTitulo("", onAtras)
                EstadoVacio(stringResource(R.string.ficha_no_encontrada), null)
            }
        }

        else -> {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                BarraFicha(estado, viewModel, salir, onCocinar)
                estado.cocinando?.let { BandaCocinando(it) }
                EnlacesTecnicas(estado.tecnicas, onTecnica)
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = FichaPalette.papel,
                    contentColor = FichaPalette.tinta,
                ) {
                    HojaPapel(receta, estado, viewModel)
                }
            }
        }
    }
}

/** Aviso de que esta receta tiene la cocina abierta: el botón de arriba vuelve a ella. */
@Composable
private fun BandaCocinando(carriles: Int) {
    Text(
        pluralStringResource(R.plurals.ficha_cocinando, carriles, carriles),
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.small)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        style = MaterialTheme.typography.labelLarge,
    )
}

@Composable
private fun DialogoAbandonarDesdeFicha(
    onSi: () -> Unit,
    onNo: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onNo,
        title = { Text(stringResource(R.string.crono_abandonar_titulo)) },
        text = { Text(stringResource(R.string.ficha_salir_abandona)) },
        confirmButton = { TextButton(onClick = onSi) { Text(stringResource(R.string.crono_abandonar_si)) } },
        dismissButton = { TextButton(onClick = onNo) { Text(stringResource(R.string.crono_abandonar_no)) } },
    )
}

@Composable
private fun BarraFicha(
    estado: FichaUiState,
    viewModel: FichaViewModel,
    onAtras: () -> Unit,
    onCocinar: (Int, String) -> Unit,
) {
    Column {
        val contexto = LocalContext.current
        val nombrePdf = stringResource(R.string.ficha_pdf_nombre, estado.receta?.titulo.orEmpty())
        BarraTitulo(estado.receta?.titulo.orEmpty(), onAtras) {
            val receta = estado.receta
            if (receta != null) {
                IconButton(
                    onClick = {
                        imprimirFicha(
                            contexto,
                            nombrePdf,
                            fichaHtml(receta, estado.raciones, TextosFicha.desde(contexto)),
                        )
                    },
                ) {
                    Icon(Icono.Imprimir.vector, contentDescription = stringResource(R.string.ficha_imprimir))
                }
            }
        }
        FilaServir(estado, viewModel, onCocinar)
    }
}

/** Hora de servir con su −/+, la hora de empezar y el botón del cronómetro (o «Volver a la cocina»). */
@Composable
private fun FilaServir(
    estado: FichaUiState,
    viewModel: FichaViewModel,
    onCocinar: (Int, String) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(stringResource(R.string.ficha_servir_a), style = MaterialTheme.typography.labelMedium)
            PasosMasMenos(
                valor = Servir.formato(estado.servirMin),
                descMenos = stringResource(R.string.ficha_servir_menos),
                descMas = stringResource(R.string.ficha_servir_mas),
                onMenos = { viewModel.ajustarServir(-Servir.PASO) },
                onMas = { viewModel.ajustarServir(Servir.PASO) },
            )
            estado.receta?.let { r ->
                Text(
                    stringResource(R.string.ficha_empezar_a, Servir.formato(Servir.inicio(estado.servirMin, r))),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Button(
            onClick = { onCocinar(estado.raciones, Servir.formato(estado.servirMin)) },
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Icon(Icono.Crono.vector, contentDescription = null)
            Text(
                stringResource(
                    if (estado.cocinando !=
                        null
                    ) {
                        R.string.ficha_volver_cocina
                    } else {
                        R.string.ficha_cocinar
                    },
                ),
                Modifier.padding(start = 8.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EnlacesTecnicas(
    tecnicas: List<TecnicaEnlace>,
    onTecnica: (String) -> Unit,
) {
    if (tecnicas.isNotEmpty()) {
        FlowRow(
            Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tecnicas.forEach { t ->
                AssistChip(
                    onClick = { onTecnica(t.slug) },
                    label = { Text(stringResource(R.string.ficha_tecnica, t.titulo)) },
                )
            }
        }
    }
}

/** Hoja de papel: siempre clara, también con el tema oscuro del sistema. */
@Composable
private fun HojaPapel(
    receta: Receta,
    estado: FichaUiState,
    viewModel: FichaViewModel,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        PortadaFicha(receta, estado.raciones)
        IngredientesFicha(receta, estado.raciones, viewModel::cambiarRaciones)
        TiempoFicha(receta)
        MepFicha(receta)
        PasosFicha(receta)
        TecnicaFicha(receta)
        receta.pie?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodySmall.merge(Tabular),
                color = FichaPalette.meta,
            )
        }
    }
}
