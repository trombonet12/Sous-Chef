package app.souschef.ui.crono

import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
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
 * Android 14+ puede retirar el permiso de pantalla completa: sin él los avisos no encienden la pantalla.
 * Se vuelve a comprobar al volver de Ajustes.
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
    val falta =
        remember(revision) {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                !contexto.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
        }
    if (!falta) return
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CronoPalette.carta2,
    ) {
        Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.crono_pantalla_aviso),
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(
                onClick = {
                    contexto.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                            "package:${contexto.packageName}".toUri(),
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
            ) { Text(stringResource(R.string.crono_pantalla_ajustes), color = CronoPalette.acento) }
        }
    }
}
