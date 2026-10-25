package app.trombito.souschef.ui.crono.voz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.trombito.souschef.R

private val GRUPOS_ORDENES =
    listOf(
        R.string.intro_voz_g_avanzar to R.string.intro_voz_g_avanzar_texto,
        R.string.intro_voz_g_tiempo to R.string.intro_voz_g_tiempo_texto,
        R.string.intro_voz_g_preguntar to R.string.intro_voz_g_preguntar_texto,
        R.string.intro_voz_g_alarma to R.string.intro_voz_g_alarma_texto,
        R.string.intro_voz_g_carril to R.string.intro_voz_g_carril_texto,
    )

/** Las órdenes que entiende el intérprete, agrupadas y con un ejemplo dicho de cada una. */
@Composable
fun GruposOrdenesVoz(
    colorGrupo: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GRUPOS_ORDENES.forEach { (titulo, texto) ->
            Column {
                Text(stringResource(titulo), style = MaterialTheme.typography.labelLarge, color = colorGrupo)
                Text(stringResource(texto), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Hoja modal «Qué puedes decirle al Chef»: la misma lista de la intro, a mano en Ajustes y en la cocina. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaOrdenesVoz(
    onCerrar: () -> Unit,
    colores: ColoresOrdenesVoz = ColoresOrdenesVoz.material(),
) {
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colores.fondo,
        contentColor = colores.texto,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("hoja-ordenes-voz"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(R.string.voz_ordenes_titulo),
                Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
            )
            Text(stringResource(R.string.intro_voz_cabecera), style = MaterialTheme.typography.bodyMedium)
            GruposOrdenesVoz(colores.grupo)
        }
    }
}
