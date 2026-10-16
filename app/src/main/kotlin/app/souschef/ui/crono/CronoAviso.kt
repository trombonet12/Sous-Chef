package app.souschef.ui.crono

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.souschef.R
import app.souschef.ui.theme.CronoPalette

/**
 * Avisos de ajustes que dejan al cronómetro mudo o tarde: notificaciones desactivadas, alarmas exactas sin permiso y
 * (Android 14+) pantalla completa retirada. Se vuelve a comprobar al volver de Ajustes.
 */
@Composable
fun AvisoPantallaCompleta() {
    val contexto = LocalContext.current
    val propietario = LocalLifecycleOwner.current
    var revision by remember { mutableIntStateOf(0) }
    DisposableEffect(propietario) {
        val observador = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) revision++ }
        propietario.lifecycle.addObserver(observador)
        onDispose { propietario.lifecycle.removeObserver(observador) }
    }
    val sinNotificaciones =
        remember(revision) { !contexto.getSystemService(NotificationManager::class.java).areNotificationsEnabled() }
    val sinExactas =
        remember(revision) {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !contexto.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        }
    val sinPantalla =
        remember(revision) {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                !contexto.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
        }
    if (!sinNotificaciones && !sinExactas && !sinPantalla) return
    val paquete = "package:${contexto.packageName}".toUri()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (sinNotificaciones) {
            Banner(R.string.crono_aviso_notificaciones, R.string.crono_pantalla_ajustes) {
                contexto.abrirAjustes(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName),
                )
            }
        }
        if (sinExactas) {
            Banner(R.string.crono_aviso_exactas, R.string.crono_pantalla_ajustes) {
                contexto.abrirAjustes(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, paquete))
            }
        }
        if (sinPantalla) {
            Banner(R.string.crono_pantalla_aviso, R.string.crono_pantalla_ajustes) {
                contexto.abrirAjustes(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, paquete))
            }
        }
    }
}

private fun Context.abrirAjustes(intent: Intent) = startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

@Composable
private fun Banner(
    texto: Int,
    boton: Int,
    alPulsar: () -> Unit,
) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CronoPalette.carta2,
    ) {
        Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(texto), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = alPulsar) { Text(stringResource(boton), color = CronoPalette.acento) }
        }
    }
}
