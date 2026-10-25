package app.trombito.souschef.data.alarma

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import app.trombito.souschef.domain.crono.CadenaAlarmas
import app.trombito.souschef.domain.crono.GestorRecordatorio
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Despierta la cadena de alarmas: al sonar la alarma emite lo vencido y arma la siguiente; tras un reinicio, una
 * actualización de la app o un cambio de hora/zona/permiso, reconstruye la cadena desde Room. El recordatorio «Avísame
 * para empezar» (ADR-0016) tiene su propia acción: al sonar avisa; en los demás casos se relee y se vuelve a armar, y
 * si la hora pasó hace 10 min o menos avisa en el acto.
 */
@AndroidEntryPoint
class AlarmaReceiver : BroadcastReceiver() {
    @Inject
    lateinit var cadena: CadenaAlarmas

    @Inject
    lateinit var recordatorio: GestorRecordatorio

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val accion = intent.action
        if (accion !in ACCIONES) return
        val pendiente = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (accion != ACCION_RECORDATORIO) protegido("las alarmas del cronómetro") { cadena.procesar() }
                if (accion != ACCION_ALARMA) protegido("el recordatorio de empezar") { recordatorio.procesar() }
            } finally {
                pendiente.finish()
            }
        }
    }

    /** Un fallo de la cadena no debe impedir el recordatorio, ni al revés. */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun protegido(
        que: String,
        bloque: suspend () -> Unit,
    ) {
        try {
            bloque()
        } catch (e: Exception) {
            Log.e("SousChef", "Fallo al procesar $que", e)
        }
    }

    companion object {
        const val ACCION_ALARMA = "app.trombito.souschef.CRONO_ALARMA"
        const val ACCION_RECORDATORIO = "app.trombito.souschef.RECORDATORIO"

        private val ACCIONES =
            setOf(
                ACCION_ALARMA,
                ACCION_RECORDATORIO,
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
                AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            )
    }
}
