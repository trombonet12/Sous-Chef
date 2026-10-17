package app.souschef.data.alarma

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

/** "Parar" de la alarma: quitarla corta su sonido; la notificación fija de la cocina sigue donde estaba. */
@AndroidEntryPoint
class PararReceiver : BroadcastReceiver() {
    @Inject
    lateinit var cadena: CadenaAlarmas

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != ACCION_PARAR) return
        val slug = intent.getStringExtra(EXTRA_SLUG) ?: return
        val pendiente = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                cadena.silenciar(slug)
            } catch (e: Exception) {
                Log.e("SousChef", "Fallo al parar la alarma", e)
            } finally {
                pendiente.finish()
            }
        }
    }

    companion object {
        const val ACCION_PARAR = "app.souschef.CRONO_PARAR"
        const val EXTRA_SLUG = "slug"
    }
}
