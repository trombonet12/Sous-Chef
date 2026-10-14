package app.souschef.data.alarma

import android.app.NotificationManager
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
 * Acciones de las notificaciones: "Parar" (y el descarte) de un aviso detiene el sonido y lo quita; el descarte de la
 * ongoing "En marcha" abandona esa cocinada.
 */
@AndroidEntryPoint
class PararReceiver : BroadcastReceiver() {
    @Inject
    lateinit var cadena: CadenaAlarmas

    @Inject
    lateinit var sonido: SonidoAlarma

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val slug = intent.getStringExtra(EXTRA_SLUG) ?: return
        when (intent.action) {
            ACCION_PARAR -> {
                sonido.parar()
                val id = intent.getIntExtra(EXTRA_ID, 0)
                if (id != 0) context.getSystemService(NotificationManager::class.java).cancel(slug, id)
            }

            ACCION_ABANDONAR -> {
                sonido.parar()
                val pendiente = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    @Suppress("TooGenericExceptionCaught")
                    try {
                        cadena.abandonar(slug)
                    } catch (e: Exception) {
                        Log.e("SousChef", "Fallo al abandonar la cocinada", e)
                    } finally {
                        pendiente.finish()
                    }
                }
            }
        }
    }

    companion object {
        const val ACCION_PARAR = "app.souschef.CRONO_PARAR"
        const val ACCION_ABANDONAR = "app.souschef.CRONO_ABANDONAR"
        const val EXTRA_SLUG = "slug"
        const val EXTRA_ID = "id"
    }
}
