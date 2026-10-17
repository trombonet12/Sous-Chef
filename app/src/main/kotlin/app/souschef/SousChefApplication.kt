package app.souschef

import android.app.Application
import android.util.Log
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
    }
}
