package app.trombito.souschef.ui.ficha

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R
import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.receta.Receta
import app.trombito.souschef.receta.TipoCocina
import app.trombito.souschef.ui.comun.CeldaDato
import app.trombito.souschef.ui.comun.ColoresUltimaVez
import app.trombito.souschef.ui.comun.SeccionPapel
import app.trombito.souschef.ui.comun.UltimaVez
import app.trombito.souschef.ui.theme.FichaPalette
import java.time.LocalDateTime

/** Resumen: lo que se lee de un vistazo. Lo largo queda en las demás pestañas o plegado. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PestanaResumen(
    receta: Receta,
    estado: FichaUiState,
    onTecnica: (String) -> Unit,
    onAnotar: (LocalDateTime, Int, String) -> Unit = { _, _, _ -> },
) {
    TuHistorial(estado.historial)
    UltimaVez(estado.notas, ColoresUltimaVez.Papel)
    if (receta.descripcion.isNotBlank()) {
        Text(
            receta.descripcion,
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
            color = FichaPalette.tinta2,
        )
    }
    FlowRow(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        CeldaDato(estado.raciones.toString(), pluralStringResource(R.plurals.ficha_unidad_racion, estado.raciones))
        receta.datos.filter { it.isNotEmpty() }.forEach { CeldaDato(it[0], it.getOrElse(1) { "" }) }
    }
    EnlacesTecnicas(estado.tecnicas, onTecnica)
    EtiquetasEquipoFicha(receta, estado.equipo)
    ResumenTiempo(receta)
    NotaCocina(estado.cocina)
    AnotarSinCronometro(estado, onAnotar)
}

/** Botón secundario «Anotar que la he cocinado»: abre el diálogo con fecha, raciones y nota. */
@Composable
private fun AnotarSinCronometro(
    estado: FichaUiState,
    onAnotar: (LocalDateTime, Int, String) -> Unit,
) {
    var abierto by rememberSaveable { mutableStateOf(false) }
    OutlinedButton(
        onClick = { abierto = true },
        modifier = Modifier.heightIn(min = 48.dp).testTag("anotar-cocinada"),
    ) {
        Text(stringResource(R.string.ficha_anotar_boton))
    }
    if (abierto) {
        DialogoAnotar(
            racionesPermitidas = estado.racionesPermitidas,
            racionesIniciales = estado.raciones,
            onAnotar = { momento, raciones, nota ->
                abierto = false
                onAnotar(momento, raciones, nota)
            },
            onCancelar = { abierto = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EnlacesTecnicas(
    tecnicas: List<TecnicaEnlace>,
    onTecnica: (String) -> Unit,
) {
    if (tecnicas.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tecnicas.forEach { t ->
            AssistChip(
                onClick = { onTecnica(t.slug) },
                label = { Text(stringResource(R.string.ficha_tecnica, t.titulo)) },
                colors = coloresAsistentePapel(),
                border = bordeAsistentePapel(),
            )
        }
    }
}

/** Diagrama por recipiente, botón al diagrama completo y la lectura del reparto, plegada. */
@Composable
private fun ResumenTiempo(receta: Receta) {
    if (receta.totalEfectivo <= 0 || receta.pasos.isEmpty()) return
    var completo by rememberSaveable { mutableStateOf(false) }
    SeccionPapel(stringResource(R.string.ficha_sec_tiempo)) {
        DiagramaCompacto(receta)
        OutlinedButton(onClick = { completo = true }, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.ficha_diagrama_ver))
        }
        receta.lectura?.takeIf { it.isNotBlank() }?.let {
            PlegableFicha(stringResource(R.string.ficha_reparte), Modifier.padding(top = 4.dp)) {
                Text(it, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
            }
        }
    }
    if (completo) DialogoDiagramaFicha(receta) { completo = false }
}

/** En gas o inducción el calor responde al instante; las explicaciones de inercia de la vitro no se traducen. */
@Composable
private fun NotaCocina(cocina: Cocina) {
    val nombre =
        when (cocina.tipo) {
            TipoCocina.GAS -> R.string.ficha_cocina_gas
            TipoCocina.INDUCCION -> R.string.ficha_cocina_induccion
            TipoCocina.VITRO -> return
        }
    NotaFicha(stringResource(R.string.ficha_nota_cocina, stringResource(nombre)))
}
