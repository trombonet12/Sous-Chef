package app.trombito.souschef.data.actualizacion

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import kotlinx.coroutines.CancellationException

/** Decisión pura de ADR-0013: qué hacer con lo que dice Play. Sin Android, para poder testearla. */
object DecisionActualizacion {
    const val INSTALADOR_PLAY = "com.android.vending"

    /** Desde esta prioridad (0-5) la actualización es obligatoria; el CI sube cada versión con 5. */
    const val PRIORIDAD_OBLIGATORIA = 4

    /** Lanzar el flujo inmediato (o reanudarlo) solo con la app de Play, sin cocinada viva y con permiso de Play. */
    fun lanzar(
        instalador: String?,
        hayViva: Boolean,
        disponibilidad: Int,
        prioridad: Int,
        inmediatoPermitido: Boolean,
    ): Boolean {
        val puede = instalador == INSTALADOR_PLAY && !hayViva
        // Una actualización inmediata que quedó a medias se reanuda.
        val aMedias = disponibilidad == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
        val nueva =
            disponibilidad == UpdateAvailability.UPDATE_AVAILABLE &&
                prioridad >= PRIORIDAD_OBLIGATORIA &&
                inmediatoPermitido
        return puede && (aMedias || nueva)
    }
}

/**
 * Fuerza la actualización desde Google Play (ADR-0013) con In-App Updates en modo inmediato. Nunca interrumpe una
 * cocinada viva. No pide red ni permisos: habla con la app de Play del móvil.
 */
class ActualizacionPlay(
    private val manager: AppUpdateManager,
    private val instalador: () -> String?,
    private val hayViva: suspend () -> Boolean,
) {
    /** Comprueba y, si procede, lanza la pantalla de Play. Sin Play o con error, no hace nada. */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    suspend fun comprobar(lanzador: ActivityResultLauncher<IntentSenderRequest>) {
        val origen = instalador()
        if (origen != DecisionActualizacion.INSTALADOR_PLAY || hayViva()) return
        try {
            val info = manager.requestAppUpdateInfo()
            val lanzar =
                DecisionActualizacion.lanzar(
                    instalador = origen,
                    hayViva = false,
                    disponibilidad = info.updateAvailability(),
                    prioridad = info.updatePriority(),
                    inmediatoPermitido = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE),
                )
            // requestAppUpdateInfo tarda: una cocinada puede haber arrancado en ese hueco.
            if (lanzar && !hayViva()) {
                val opciones = AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                manager.startUpdateFlowForResult(info, lanzador, opciones)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Sin Play Store o sin respuesta: la app sigue como si nada.
        }
    }

    companion object {
        /** Quién instaló la app: `getInstallSourceInfo` desde la API 30, `getInstallerPackageName` antes. */
        @Suppress("DEPRECATION", "SwallowedException")
        fun instaladorDe(context: Context): String? {
            val pm = context.packageManager
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    pm.getInstallSourceInfo(context.packageName).installingPackageName
                } else {
                    pm.getInstallerPackageName(context.packageName)
                }
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
        }
    }
}
