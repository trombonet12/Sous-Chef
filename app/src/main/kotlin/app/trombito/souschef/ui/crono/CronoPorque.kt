package app.trombito.souschef.ui.crono

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.trombito.souschef.receta.Carril
import app.trombito.souschef.receta.Paso
import app.trombito.souschef.ui.comun.ContextoPaso
import app.trombito.souschef.ui.comun.DetallePaso
import app.trombito.souschef.ui.comun.contextoPaso
import app.trombito.souschef.ui.comun.paletaDetalleCrono
import app.trombito.souschef.ui.theme.CronoPalette
import java.time.Instant
import java.time.ZoneId

private const val MS_MEDIO_MINUTO = 30_000L
private const val MIN_HORA = 60

/**
 * El detalle del paso (el mismo componente que la ficha, ADR-0017): primero lo ejecutivo y después el porqué, con los
 * enlaces a las técnicas de la receta. [contexto] da «paso i de n», «tras N» y las horas; sin él, solo lo que sale del
 * propio paso (ver [contextoPasoCrono]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongParameterList")
@Composable
fun PorqueSheet(
    paso: Paso,
    carril: Carril,
    color: Color,
    tecnicas: Map<String, String>,
    onTecnica: (String) -> Unit,
    onCerrar: () -> Unit,
    contexto: ContextoPaso = ContextoPaso(),
) {
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CronoPalette.carta,
        contentColor = CronoPalette.texto,
    ) {
        DetallePaso(
            paso = paso,
            carrilNombre = carril.nombre,
            tipoCarril = carril.tipoCarril,
            contexto = contexto,
            paleta = paletaDetalleCrono(color),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            tecnicas = tecnicas,
            onTecnica = {
                onCerrar()
                onTecnica(it)
            },
        )
    }
}

/** El [ContextoPaso] de [paso] con las horas del plan de la cocina (`servir − total + off`, como el motor). */
fun contextoPasoCrono(
    datos: DatosCrono,
    paso: Paso,
): ContextoPaso {
    val totalMs = Math.round(datos.receta.totalEfectivo * 2) * MS_MEDIO_MINUTO
    val inicio = Instant.ofEpochMilli(datos.estado.servirMs - totalMs).atZone(ZoneId.systemDefault())
    return contextoPaso(datos.recetaUi, paso, inicio.hour * MIN_HORA + inicio.minute)
}
