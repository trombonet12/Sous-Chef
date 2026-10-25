package app.trombito.souschef.ui.ficha

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.domain.MAX_NOTA
import app.trombito.souschef.domain.PASO_HORA_ANOTAR_MIN
import app.trombito.souschef.domain.ajustarMomento
import app.trombito.souschef.ui.comun.PasosMasMenos
import app.trombito.souschef.ui.comun.fechaCorta
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val FORMATO_FECHA = DateTimeFormatter.ISO_LOCAL_DATE
private val FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm")

/**
 * «Anotar que la he cocinado»: fecha y hora (por defecto, [ahora]; nunca del futuro), raciones (las de la ficha, dentro
 * de [racionesPermitidas]) y una nota opcional. No es un cronómetro: solo recoge los datos de [onAnotar].
 */
@Composable
fun DialogoAnotar(
    racionesPermitidas: List<Int>,
    racionesIniciales: Int,
    onAnotar: (momento: LocalDateTime, raciones: Int, nota: String) -> Unit,
    onCancelar: () -> Unit,
    ahora: LocalDateTime = remember { LocalDateTime.now().withNano(0) },
) {
    // LocalDateTime no es guardable tal cual: se guarda como texto ISO y se lee al pintar.
    var momentoIso by rememberSaveable { mutableStateOf(ahora.toString()) }
    var raciones by rememberSaveable { mutableIntStateOf(racionesIniciales) }
    var nota by rememberSaveable { mutableStateOf("") }
    val momento = LocalDateTime.parse(momentoIso)
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.ficha_anotar_titulo)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CamposMomento(momento, ahora) { momentoIso = it.toString() }
                if (racionesPermitidas.size > 1) {
                    CampoRaciones(racionesPermitidas, raciones) { raciones = it }
                }
                CampoNota(nota) { nota = it.take(MAX_NOTA) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAnotar(momento, raciones, nota) }, modifier = Modifier.testTag("anotar-guardar")) {
                Text(stringResource(R.string.ficha_anotar_guardar))
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text(stringResource(R.string.ficha_anotar_cancelar)) } },
    )
}

/** Fecha (de día en día) y hora (de [PASO_HORA_ANOTAR_MIN] en [PASO_HORA_ANOTAR_MIN]); nunca pasa de [ahora]. */
@Composable
private fun CamposMomento(
    momento: LocalDateTime,
    ahora: LocalDateTime,
    onCambia: (LocalDateTime) -> Unit,
) {
    Etiquetado(R.string.ficha_anotar_fecha) {
        PasosMasMenos(
            valor = fechaCorta(FORMATO_FECHA.format(momento)),
            descMenos = stringResource(R.string.ficha_anotar_dia_menos),
            descMas = stringResource(R.string.ficha_anotar_dia_mas),
            onMenos = { onCambia(ajustarMomento(momento, ahora, dias = -1)) },
            onMas = { onCambia(ajustarMomento(momento, ahora, dias = 1)) },
        )
    }
    Etiquetado(R.string.ficha_anotar_hora) {
        PasosMasMenos(
            valor = FORMATO_HORA.format(momento),
            descMenos = stringResource(R.string.ficha_anotar_hora_menos),
            descMas = stringResource(R.string.ficha_anotar_hora_mas),
            onMenos = { onCambia(ajustarMomento(momento, ahora, minutos = -PASO_HORA_ANOTAR_MIN)) },
            onMas = { onCambia(ajustarMomento(momento, ahora, minutos = PASO_HORA_ANOTAR_MIN)) },
        )
    }
}

@Composable
private fun CampoRaciones(
    permitidas: List<Int>,
    raciones: Int,
    onCambia: (Int) -> Unit,
) {
    val i = permitidas.indexOf(raciones)
    Etiquetado(R.string.ficha_raciones_etiqueta) {
        PasosMasMenos(
            valor = raciones.toString(),
            descMenos = stringResource(R.string.ficha_raciones_menos),
            descMas = stringResource(R.string.ficha_raciones_mas),
            onMenos = { permitidas.getOrNull(i - 1)?.let(onCambia) },
            onMas = { permitidas.getOrNull(i + 1)?.let(onCambia) },
        )
    }
}

@Composable
private fun CampoNota(
    nota: String,
    onCambia: (String) -> Unit,
) {
    val teclado = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = nota,
        onValueChange = onCambia,
        modifier = Modifier.fillMaxWidth().testTag("anotar-nota"),
        label = { Text(stringResource(R.string.ficha_anotar_nota)) },
        minLines = 1,
        maxLines = 3,
        keyboardOptions =
            KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { teclado?.hide() }),
        supportingText = { Text("${nota.length}/$MAX_NOTA") },
    )
}

@Composable
private fun Etiquetado(
    etiqueta: Int,
    contenido: @Composable () -> Unit,
) {
    Column {
        Text(stringResource(etiqueta), style = MaterialTheme.typography.labelMedium)
        contenido()
    }
}
