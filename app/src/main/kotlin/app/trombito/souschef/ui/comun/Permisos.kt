package app.trombito.souschef.ui.comun

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Permisos que dejan al cronómetro mudo o tarde si faltan (los mismos avisos que la portada del cronómetro). */
enum class PermisoApp { NOTIFICACIONES, ALARMA_EXACTA, PANTALLA_COMPLETA }

/** Estado de cada permiso; en versiones de Android que no lo piden cuenta como concedido. */
data class EstadoPermisos(
    val notificaciones: Boolean = true,
    val alarmaExacta: Boolean = true,
    val pantallaCompleta: Boolean = true,
) {
    fun concedido(permiso: PermisoApp): Boolean =
        when (permiso) {
            PermisoApp.NOTIFICACIONES -> notificaciones
            PermisoApp.ALARMA_EXACTA -> alarmaExacta
            PermisoApp.PANTALLA_COMPLETA -> pantallaCompleta
        }
}

/** Lee el estado actual de los tres permisos. */
fun leerPermisos(contexto: Context): EstadoPermisos {
    val avisos = contexto.getSystemService(NotificationManager::class.java)
    val alarmas = contexto.getSystemService(AlarmManager::class.java)
    return EstadoPermisos(
        notificaciones = avisos.areNotificationsEnabled(),
        alarmaExacta = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmas.canScheduleExactAlarms(),
        pantallaCompleta =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || avisos.canUseFullScreenIntent(),
    )
}

fun tieneMicrofono(contexto: Context): Boolean =
    ContextCompat.checkSelfPermission(contexto, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

/** Pantalla de ajustes del sistema donde se concede [permiso]. */
fun intentAjustesDe(
    contexto: Context,
    permiso: PermisoApp,
): Intent {
    val paquete = "package:${contexto.packageName}".toUri()
    return when (permiso) {
        PermisoApp.NOTIFICACIONES -> {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName)
        }

        PermisoApp.ALARMA_EXACTA -> {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, paquete)
        }

        PermisoApp.PANTALLA_COMPLETA -> {
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, paquete)
        }
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

private fun Context.buscarActividad(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.buscarActividad()
        else -> null
    }

/**
 * Estado vivo de los tres permisos y la acción de pedirlos. Se vuelve a comprobar al volver a la pantalla
 * (`ON_RESUME`) y al contestar el diálogo del sistema.
 */
@Stable
class PermisosApp internal constructor(
    private val contexto: Context,
) {
    var estado: EstadoPermisos by mutableStateOf(leerPermisos(contexto))
        private set

    /** Abre el diálogo del sistema de notificaciones; lo pone [rememberPermisosApp]. */
    internal var lanzarNotificaciones: () -> Unit = {}

    private var notificacionesPedidas = false

    fun revisar() {
        estado = leerPermisos(contexto)
    }

    /**
     * Notificaciones: el diálogo del sistema la primera vez (Android 13+); si ya se negó, o en versiones anteriores,
     * los ajustes de la app. Alarmas exactas y pantalla completa: siempre sus ajustes.
     */
    fun permitir(permiso: PermisoApp) {
        val dialogo =
            permiso == PermisoApp.NOTIFICACIONES &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !notificacionesPedidas
        if (dialogo) {
            notificacionesPedidas = true
            lanzarNotificaciones()
        } else {
            runCatching { contexto.startActivity(intentAjustesDe(contexto, permiso)) }
        }
    }
}

@Composable
fun rememberPermisosApp(): PermisosApp {
    val contexto = LocalContext.current
    val propietario = LocalLifecycleOwner.current
    val actual = remember(contexto) { PermisosApp(contexto) }
    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { actual.revisar() }
    SideEffect { actual.lanzarNotificaciones = { lanzador.launch(Manifest.permission.POST_NOTIFICATIONS) } }
    DisposableEffect(propietario, actual) {
        val observador = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) actual.revisar() }
        propietario.lifecycle.addObserver(observador)
        onDispose { propietario.lifecycle.removeObserver(observador) }
    }
    return actual
}

/**
 * Acción que pide RECORD_AUDIO; [alResultado] recibe si se concedió. Si el sistema ya no vuelve a preguntar (se negó
 * con «no volver a preguntar») abre los ajustes de la app.
 */
@Composable
fun rememberPedirMicrofono(alResultado: (Boolean) -> Unit): () -> Unit {
    val contexto = LocalContext.current
    var yaPedido by rememberSaveable { mutableStateOf(false) }
    val resultado by rememberUpdatedState(alResultado)
    val lanzador =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { resultado(it) }
    return {
        val actividad = contexto.buscarActividad()
        val definitivo =
            yaPedido &&
                actividad != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(actividad, Manifest.permission.RECORD_AUDIO)
        if (definitivo) {
            val paquete = Uri.fromParts("package", contexto.packageName, null)
            runCatching { contexto.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, paquete)) }
        } else {
            yaPedido = true
            lanzador.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}
