package app.trombito.souschef.ui.crono

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import app.trombito.souschef.R
import app.trombito.souschef.ui.crono.guia.GuiaIds
import app.trombito.souschef.ui.crono.guia.guia
import app.trombito.souschef.ui.shell.Icono
import app.trombito.souschef.ui.theme.CronoPalette

internal fun tieneMicrofono(contexto: Context): Boolean =
    ContextCompat.checkSelfPermission(contexto, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

/**
 * Pide RECORD_AUDIO la primera vez que la voz está activa y falta el permiso (con la cocina en marcha). Devuelve la
 * acción del botón del micrófono: si ya se pidió y el sistema no vuelve a preguntar, abre los ajustes de la app.
 */
@Composable
internal fun permisoDeMicrofono(
    voz: EstadoVoz,
    viewModel: CronoViewModel,
): () -> Unit {
    val contexto = LocalContext.current
    var yaPedido by rememberSaveable { mutableStateOf(false) }
    val lanzador =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
            viewModel.vozIniciar(concedido)
        }
    LaunchedEffect(voz) {
        if (voz == EstadoVoz.SIN_PERMISO && !yaPedido) {
            yaPedido = true
            lanzador.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    return {
        val actividad = contexto.actividad()
        val definitivo =
            yaPedido &&
                actividad != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(actividad, Manifest.permission.RECORD_AUDIO)
        if (definitivo) {
            val paquete = Uri.fromParts("package", contexto.packageName, null)
            contexto.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, paquete))
        } else {
            yaPedido = true
            lanzador.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}

/** Micrófono de la cabecera: escuchando, silenciado o sin permiso. Oculto si la voz no está en juego. */
@Composable
internal fun BotonMicrofono(
    ui: CronoUi,
    silenciar: () -> Unit,
    pedirPermiso: () -> Unit,
) {
    if (ui.voz == EstadoVoz.APAGADA && !ui.vozSilenciada) return
    val sinPermiso = ui.voz == EstadoVoz.SIN_PERMISO
    val escuchando = ui.voz == EstadoVoz.ESCUCHANDO
    val descripcion =
        when {
            sinPermiso -> R.string.crono_voz_sin_permiso
            escuchando -> R.string.crono_voz_silenciar
            else -> R.string.crono_voz_activar
        }
    IconButton(onClick = if (sinPermiso) pedirPermiso else silenciar, Modifier.size(48.dp).guia(GuiaIds.MICRO)) {
        Icon(
            if (escuchando) Icono.Microfono.vector else Icono.MicrofonoMudo.vector,
            stringResource(descripcion),
            tint =
                when {
                    sinPermiso -> CronoPalette.alerta
                    escuchando -> CronoPalette.acento
                    else -> CronoPalette.tenue
                },
        )
    }
}
