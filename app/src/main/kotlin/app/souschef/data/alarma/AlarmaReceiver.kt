package app.souschef.data.alarma

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import app.souschef.domain.crono.CadenaAlarmas
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Despierta la cadena de alarmas: al sonar la alarma emite lo vencido y arma la siguiente; tras un reinicio, una
 * actualización de la app o un cambio de hora/zona/permiso, reconstruye la cadena desde Room.
 */
@AndroidEntryPoint
class AlarmaReceiver : BroadcastReceiver() {
    @Inject
    lateinit var cadena: CadenaAlarmas

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action !in ACCIONES) return
        val pendiente = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                cadena.procesar()
            } catch (e: Exception) {
                Log.e("SousChef", "Fallo al procesar las alarmas del cronómetro", e)
            } finally {
                pendiente.finish()
            }
        }
    }

    companion object {
        const val ACCION_ALARMA = "app.souschef.CRONO_ALARMA"

        private val ACCIONES =
            setOf(
                ACCION_ALARMA,
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
                AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            )
    }
}
