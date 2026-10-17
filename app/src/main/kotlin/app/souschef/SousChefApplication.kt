package app.souschef

import android.app.Application
import android.util.Log
import app.souschef.data.actualizacion.ComprobadorActualizaciones
import app.souschef.data.actualizacion.TrabajoActualizaciones
import app.souschef.data.seed.Seeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SousChefApplication : Application() {
    @Inject
    lateinit var seeder: Seeder

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            // Los Flow de Room emiten solos cuando termina la siembra.
            @Suppress("TooGenericExceptionCaught")
            try {
                seeder.sembrar()
            } catch (e: Exception) {
                Log.e("SousChef", "Fallo al sembrar datos locales", e)
            }
        }
        // Aviso de versión nueva (ADR-0009): al abrir, como mucho cada 6 h, y un trabajo diario con red.
        TrabajoActualizaciones.programar(this)
        scope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                ComprobadorActualizaciones(this@SousChefApplication).comprobarSiToca()
            } catch (e: Exception) {
                Log.w("SousChef", "No se pudo comprobar si hay versión nueva", e)
            }
        }
    }
}
