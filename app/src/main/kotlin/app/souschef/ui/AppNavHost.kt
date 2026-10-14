package app.souschef.ui

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.core.util.Consumer
import androidx.navigation.compose.rememberNavController
import app.souschef.ui.shell.SousChefShell

/** Raíz de navegación. Con la app abierta (singleTop), los deep links de las notificaciones llegan por onNewIntent. */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val actividad = LocalActivity.current as? ComponentActivity
    DisposableEffect(actividad, navController) {
        val oyente = Consumer<Intent> { navController.handleDeepLink(it) }
        actividad?.addOnNewIntentListener(oyente)
        onDispose { actividad?.removeOnNewIntentListener(oyente) }
    }
    SousChefShell(navController = navController)
}
