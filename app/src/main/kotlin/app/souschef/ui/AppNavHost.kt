package app.souschef.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import app.souschef.ui.shell.SousChefShell

/** Raíz de navegación. Las rutas de cada pantalla se añaden en F5-F7. */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    SousChefShell(navController = navController)
}
