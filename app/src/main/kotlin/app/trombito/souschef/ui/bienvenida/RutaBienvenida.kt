package app.trombito.souschef.ui.bienvenida

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import app.trombito.souschef.ui.Rutas
import app.trombito.souschef.ui.shell.Pestana

/**
 * Las dos entradas de la intro: la del primer arranque, que al acabar limpia la pila y va a Recetas, y la que se abre
 * desde Ajustes, que al acabar vuelve atrás.
 */
fun NavGraphBuilder.bienvenida(navController: NavHostController) {
    composable(Rutas.BIENVENIDA) {
        BienvenidaScreen(
            onTerminar = {
                navController.navigate(Pestana.Recetas.ruta) {
                    popUpTo(Rutas.BIENVENIDA) { inclusive = true }
                    launchSingleTop = true
                }
            },
        )
    }
    composable(Rutas.BIENVENIDA_REPETIR) {
        BienvenidaScreen(onTerminar = { navController.popBackStack() })
    }
}
