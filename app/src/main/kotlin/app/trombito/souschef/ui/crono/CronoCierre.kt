package app.trombito.souschef.ui.crono

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.domain.MAX_NOTA
import app.trombito.souschef.domain.previstoMinDe
import app.trombito.souschef.receta.PasoMedido
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.crono.EstadoCrono
import app.trombito.souschef.receta.crono.ResumenCrono
import app.trombito.souschef.receta.crono.TemporizadoresLibres
import app.trombito.souschef.receta.desvios
import app.trombito.souschef.receta.textoInforme
import app.trombito.souschef.ui.theme.CronoPalette
import java.time.LocalDate

/** Cierre: resumen de la cocinada, valoración y nota opcionales y paso al historial. */
@Suppress("LongParameterList") // la receta, su estado y resumen, y lo opcional del cierre
@Composable
fun CronoCierre(
    receta: Receta,
    estado: EstadoCrono,
    resumen: ResumenCrono,
    notaAnterior: String? = null,
    valoracion: ValoracionCierreUi? = null,
    onCerrar: (String) -> Unit,
) {
    val nota = rememberSaveable { mutableStateOf("") }
    // El botón queda fijo abajo, fuera del scroll: con 4 carriles y letra grande no se pierde de vista.
    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding(),
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ContenidoCierre(receta, estado, resumen)
            if (valoracion != null) ValoracionCierre(valoracion)
            CampoNota(nota, notaAnterior)
        }
        Button(
            onClick = { onCerrar(nota.value) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .heightIn(min = 56.dp)
                    .testTag("ya-esta"),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = CronoPalette.principal,
                    contentColor = CronoPalette.onPrincipal,
                ),
        ) {
            Text(stringResource(R.string.crono_ya_esta), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ContenidoCierre(
    receta: Receta,
    estado: EstadoCrono,
    resumen: ResumenCrono,
) {
    Text(stringResource(R.string.crono_todo_fuera), style = MaterialTheme.typography.headlineMedium)
    Text(stringResource(R.string.crono_listo_emplata, receta.titulo), color = CronoPalette.apagado)
    resumen.carriles.forEach {
        val tramo = stringResource(R.string.crono_resumen_carril, it.carril, it.pasos, it.minutos)
        val fuego = if (it.deFuego) stringResource(R.string.crono_de_fuego) else ""
        Text(tramo + fuego)
    }
    resumen.minutosReales?.let {
        Text(
            textoRealPrevisto(it, previstoMinDe(receta)),
            fontWeight = FontWeight.Bold,
            color = CronoPalette.ok,
        )
    }
    Text(
        stringResource(
            R.string.crono_servido_previsto,
            formatoHora(resumen.servidoMs),
            formatoHora(estado.servirMs),
        ),
        color = CronoPalette.apagado,
    )
    val libres = TemporizadoresLibres.pendientes(estado)
    if (libres.isNotEmpty()) {
        Text(
            stringResource(R.string.crono_cierre_temporizadores, libres.joinToString(", ") { "${it.minutos} min" }),
            Modifier.testTag("cierre-temporizadores"),
            color = CronoPalette.apagado,
        )
    }
    if (resumen.pasosReales.isNotEmpty()) BotonInforme(receta, estado.raciones, resumen)
}

@Composable
private fun CampoNota(
    nota: MutableState<String>,
    notaAnterior: String?,
) {
    val teclado = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = nota.value,
        onValueChange = { nota.value = it.take(MAX_NOTA) },
        modifier = Modifier.fillMaxWidth().testTag("nota-cierre"),
        label = { Text(stringResource(R.string.crono_proxima_vez)) },
        minLines = 1,
        maxLines = 3,
        keyboardOptions =
            KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { teclado?.hide() }),
        supportingText = {
            Column {
                if (notaAnterior != null) {
                    Text(
                        stringResource(R.string.crono_nota_anterior, notaAnterior),
                        Modifier.testTag("nota-anterior"),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text("${nota.value.length}/$MAX_NOTA")
            }
        },
    )
}

/** «Real N min · previsto M (+D)», o «(−D)» si sale antes; el previsto es el total de la receta (`ceil`). */
@Composable
private fun textoRealPrevisto(
    reales: Int,
    previsto: Int,
): String {
    val delta = reales - previsto
    return when {
        delta > 0 -> stringResource(R.string.crono_real_previsto_mas, reales, previsto, delta)
        delta < 0 -> stringResource(R.string.crono_real_previsto_menos, reales, previsto, -delta)
        else -> stringResource(R.string.crono_real_previsto_igual, reales, previsto)
    }
}

@Composable
private fun BotonInforme(
    receta: Receta,
    raciones: Int,
    resumen: ResumenCrono,
) {
    val contexto = LocalContext.current
    OutlinedButton(
        onClick = { compartirInforme(contexto, receta, raciones, resumen) },
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("compartir-informe"),
    ) { Text(stringResource(R.string.crono_compartir_informe)) }
}

private fun compartirInforme(
    contexto: Context,
    receta: Receta,
    raciones: Int,
    resumen: ResumenCrono,
) {
    val reales = resumen.pasosReales.map { PasoMedido(it.id, it.segundos) }
    val texto =
        textoInforme(receta, LocalDate.now().toString(), raciones, resumen.minutosReales, desvios(receta, reales))
    val envio =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, texto)
        }
    val selector = Intent.createChooser(envio, contexto.getString(R.string.crono_compartir_informe_titulo))
    if (contexto !is Activity) selector.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    contexto.startActivity(selector)
}
