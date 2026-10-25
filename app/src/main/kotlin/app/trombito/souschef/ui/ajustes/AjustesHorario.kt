package app.trombito.souschef.ui.ajustes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trombito.souschef.R
import app.trombito.souschef.domain.Ajustes
import app.trombito.souschef.domain.HoraTarjeta
import app.trombito.souschef.ui.comun.PasosMasMenos
import app.trombito.souschef.ui.ficha.Servir
import java.time.LocalTime

/** Duración de ejemplo de los detalles de «Hora en las tarjetas». */
private const val DURACION_EJEMPLO_MIN = 47

@Composable
fun AjustesHorarioScreen(
    onAtras: () -> Unit,
    viewModel: AjustesViewModel = hiltViewModel(),
) {
    val ajustes by viewModel.estado.collectAsStateWithLifecycle()
    val ahora = remember { LocalTime.now().let { it.hour * 60 + it.minute } }
    AjustesHorario(
        ajustes,
        ahora,
        onAtras,
        viewModel::ajustarServir,
        viewModel::elegirHoraTarjeta,
        viewModel::ajustarCenar,
    )
}

/** [ahoraMin]: hora actual en minutos del día, para el ejemplo de «Si empiezas ya». */
@Suppress("LongParameterList") // un callback por ajuste de la pantalla
@Composable
internal fun AjustesHorario(
    ajustes: Ajustes,
    ahoraMin: Int,
    onAtras: () -> Unit,
    onServir: (Int) -> Unit,
    onHoraTarjeta: (HoraTarjeta) -> Unit,
    onCenar: (Int) -> Unit = {},
) {
    PantallaAjustes(stringResource(R.string.ajustes_horario), onAtras) {
        HoraPorDefecto(
            titulo = R.string.ajustes_servir_titulo,
            nota = R.string.ajustes_servir_nota,
            minutos = ajustes.servirMin,
            descripciones = R.string.ficha_servir_menos to R.string.ficha_servir_mas,
            onMover = onServir,
        )
        Column(Modifier.padding(top = 16.dp)) {
            HoraPorDefecto(
                titulo = R.string.ajustes_cenar_titulo,
                nota = R.string.ajustes_cenar_nota,
                minutos = ajustes.cenarMin,
                descripciones = R.string.cenar_menos to R.string.cenar_mas,
                onMover = onCenar,
            )
        }
        Text(
            stringResource(R.string.ajustes_tarjetas_titulo),
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Column(Modifier.fillMaxWidth().selectableGroup()) {
            val empieza = Servir.formato(Servir.ajustar(ajustes.servirMin, -DURACION_EJEMPLO_MIN))
            val acaba = Servir.formato(Servir.ajustar(ahoraMin, DURACION_EJEMPLO_MIN))
            OpcionHora(
                HoraTarjeta.EMPEZAR,
                ajustes.horaTarjeta,
                R.string.ajustes_tarjeta_empezar,
                stringResource(
                    R.string.ajustes_tarjeta_empezar_detalle,
                    empieza,
                    Servir.formato(ajustes.servirMin),
                ),
                onHoraTarjeta,
            )
            OpcionHora(
                HoraTarjeta.ACABAR,
                ajustes.horaTarjeta,
                R.string.ajustes_tarjeta_acabar,
                stringResource(R.string.ajustes_tarjeta_acabar_detalle, acaba),
                onHoraTarjeta,
            )
        }
    }
}

/** Una hora por defecto (comer o cenar): título, − y +, y una nota; [descripciones] = (antes, más tarde). */
@Composable
private fun HoraPorDefecto(
    titulo: Int,
    nota: Int,
    minutos: Int,
    descripciones: Pair<Int, Int>,
    onMover: (Int) -> Unit,
) {
    Text(
        stringResource(titulo),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
    PasosMasMenos(
        valor = Servir.formato(minutos),
        descMenos = stringResource(descripciones.first),
        descMas = stringResource(descripciones.second),
        onMenos = { onMover(-Servir.PASO) },
        onMas = { onMover(Servir.PASO) },
    )
    Text(
        stringResource(nota),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun OpcionHora(
    opcion: HoraTarjeta,
    elegida: HoraTarjeta,
    titulo: Int,
    detalle: String,
    onElegir: (HoraTarjeta) -> Unit,
) {
    val marcada = opcion == elegida
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag("hora-${opcion.name}")
            .selectable(selected = marcada, role = Role.RadioButton, onClick = { onElegir(opcion) })
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = marcada, onClick = null)
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(titulo),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (marcada) FontWeight.Bold else FontWeight.Normal,
            )
            Text(
                detalle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
