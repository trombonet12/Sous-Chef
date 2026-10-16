package app.souschef.data.alarma

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import app.souschef.domain.crono.CadenaAlarmas
import app.souschef.domain.crono.IdsNotificacion
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * "Parar" de un aviso: quitar la notificación de alarma corta su sonido, y repintar devuelve el carril a su estado
 * normal. La ongoing "En marcha" ya no se abandona al descartarla (solo desde el menú del cronómetro).
 */
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
        val id = intent.getIntExtra(EXTRA_ID, 0)
        if (id != 0) {
            context.getSystemService(NotificationManager::class.java).cancel(IdsNotificacion.tagCarril(slug), id)
        }
        val pendiente = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                cadena.repintar()
            } catch (e: Exception) {
                Log.e("SousChef", "Fallo al repintar tras Parar", e)
            } finally {
                pendiente.finish()
            }
        }
    }

    companion object {
        const val ACCION_PARAR = "app.souschef.CRONO_PARAR"
        const val EXTRA_SLUG = "slug"
        const val EXTRA_ID = "id"
    }
}
