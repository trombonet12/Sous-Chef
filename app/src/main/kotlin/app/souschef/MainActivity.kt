package app.souschef

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.souschef.domain.Ajustes
import app.souschef.domain.AjustesRepo
import app.souschef.ui.AppNavHost
import app.souschef.ui.theme.SousChefTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var ajustesRepo: AjustesRepo

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

    private companion object {
        const val TRANSPARENTE = android.graphics.Color.TRANSPARENT
    }
}
