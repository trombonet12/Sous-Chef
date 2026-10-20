package app.trombito.souschef

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import app.trombito.souschef.data.actualizacion.ActualizacionPlay
import app.trombito.souschef.domain.Ajustes
import app.trombito.souschef.domain.AjustesRepo
import app.trombito.souschef.ui.AppNavHost
import app.trombito.souschef.ui.theme.SousChefTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var ajustesRepo: AjustesRepo

    @Inject lateinit var actualizacion: ActualizacionPlay

    /** Resultado de la pantalla de Play; si se cancela no se reintenta hasta el siguiente `onResume`. */
    private val pantallaActualizacion = registerForActivityResult(StartIntentSenderForResult()) {}

    private var comprobacion: Job? = null

    /** Hasta leer los ajustes (tema) se queda el splash, para no pintar un fotograma con el tema equivocado. */
    @Volatile private var listo = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { !listo }
        super.onCreate(savedInstanceState)
        setContent {
            val ajustes: Ajustes? by ajustesRepo.observar().collectAsStateWithLifecycle(initialValue = null)
            val actuales = ajustes ?: return@setContent
            listo = true
            val oscuro = actuales.tema.oscuro(isSystemInDarkTheme())
            DisposableEffect(oscuro) {
                val barras = SystemBarStyle.auto(TRANSPARENTE, TRANSPARENTE) { oscuro }
                enableEdgeToEdge(statusBarStyle = barras, navigationBarStyle = barras)
                onDispose {}
            }
            SousChefTheme(darkTheme = oscuro) {
                Surface { AppNavHost() }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Actualización obligatoria desde Play (ADR-0013); nunca con una cocinada viva.
        if (comprobacion?.isActive == true) return
        comprobacion = lifecycleScope.launch { actualizacion.comprobar(pantallaActualizacion) }
    }

    private companion object {
        const val TRANSPARENTE = android.graphics.Color.TRANSPARENT
    }
}
