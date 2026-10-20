package app.trombito.souschef.data.alarma

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import app.trombito.souschef.domain.crono.CadenaAlarmas
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Acción "Hecho" / "Hecho y siguiente" de las notificaciones: Hecho del paso más urgente y arranque del siguiente. */
@AndroidEntryPoint
class SiguienteReceiver : BroadcastReceiver() {
    @Inject
    lateinit var cadena: CadenaAlarmas

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != ACCION_SIGUIENTE) return
        val slug = intent.getStringExtra(EXTRA_SLUG) ?: return
        val pendiente = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                cadena.siguiente(slug)
            } catch (e: Exception) {
                Log.e("SousChef", "Fallo en la acción Siguiente", e)
            } finally {
                pendiente.finish()
            }
        }
    }

    companion object {
        const val ACCION_SIGUIENTE = "app.trombito.souschef.CRONO_SIGUIENTE"
        const val EXTRA_SLUG = "slug"
    }
}
