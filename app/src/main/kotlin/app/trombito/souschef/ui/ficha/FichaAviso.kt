package app.trombito.souschef.ui.ficha

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.content.ContextCompat
import app.trombito.souschef.R
import app.trombito.souschef.domain.crono.ChipAviso

/**
 * El chip «Avísame a las HH:mm» pide el permiso de notificaciones al tocarlo (Android 13+) y arma el aviso se conceda o
 * no, como la portada del cronómetro: sin permiso la alarma suena igual, solo que sin aviso en la bandeja.
 */
@Composable
internal fun rememberAvisarParaEmpezar(armar: () -> Unit): () -> Unit {
    val contexto = LocalContext.current
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { armar() }
    return {
        val necesita =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        if (necesita) pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS) else armar()
    }
}

/** «Avísame a las HH:mm», o «Aviso a las HH:mm · Quitar» cuando ya está puesto (ADR-0016). */
@Composable
internal fun ChipAvisoEmpezar(
    aviso: ChipAviso,
    acciones: AccionesFicha,
) {
    val hora = Servir.formato(aviso.horaMin)
    val descripcion = stringResource(R.string.recordatorio_quitar_descripcion, hora)
    AssistChip(
        onClick = if (aviso.puesto) acciones.onQuitarAviso else acciones.onAvisar,
        label = {
            Text(
                stringResource(
                    if (aviso.puesto) R.string.recordatorio_puesto else R.string.recordatorio_avisame,
                    hora,
                ),
            )
        },
        modifier =
            Modifier.testTag("aviso-empezar").semantics {
                if (aviso.puesto) contentDescription = descripcion
            },
    )
}
