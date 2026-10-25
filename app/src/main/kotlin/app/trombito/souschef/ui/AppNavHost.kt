package app.trombito.souschef.ui

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.util.Consumer
import androidx.navigation.compose.rememberNavController
import app.trombito.souschef.ui.shell.SousChefShell

/**
 * Raíz de navegación. Con la app abierta (singleTop), los deep links de las notificaciones y los accesos directos del
 * icono (extra [EXTRA_ATAJO]) llegan por onNewIntent. El atajo pendiente se guarda con el estado de la pantalla: tras
 * girarla o recrear la actividad no se repite.
 */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val actividad = LocalActivity.current as? ComponentActivity
    var atajo by rememberSaveable { mutableStateOf(atajoDe(actividad?.intent)?.clave) }
    DisposableEffect(actividad, navController) {
        val oyente =
            Consumer<Intent> { intent ->
                navController.handleDeepLink(intent)
                atajoDe(intent)?.let { atajo = it.clave }
            }
        actividad?.addOnNewIntentListener(oyente)
        onDispose { actividad?.removeOnNewIntentListener(oyente) }
    }
    AplicarAtajo(navController, atajo) { atajo = null }
    SousChefShell(navController = navController)
}
