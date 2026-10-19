package app.souschef.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import app.souschef.R
import app.souschef.ui.ajustes.AjustesAparienciaScreen
import app.souschef.ui.ajustes.AjustesCronoScreen
import app.souschef.ui.ajustes.AjustesHistorialScreen
import app.souschef.ui.ajustes.AjustesHorarioScreen
import app.souschef.ui.ajustes.AjustesScreen
import app.souschef.ui.crono.CronoScreen
import app.souschef.ui.ficha.FichaScreen
import app.souschef.ui.historial.HistorialScreen
import app.souschef.ui.recetas.RecetasScreen
import app.souschef.ui.shell.Pestana
import app.souschef.ui.tecnicas.TecnicaScreen
import app.souschef.ui.tecnicas.TecnicasScreen

private val ARG_SLUG = navArgument(Rutas.ARG_SLUG) { type = NavType.StringType }

/** Grafo de destinos. */
@Composable
fun SousChefNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val abrirFicha: (String) -> Unit = { navController.navigate(Rutas.ficha(it)) }
    NavHost(navController, startDestination = Pestana.Recetas.ruta, modifier = modifier) {
        composable(Pestana.Recetas.ruta) { RecetasScreen(onAbrir = abrirFicha) }
        composable(Pestana.Tecnicas.ruta) {
            TecnicasScreen(onAtras = null, onAbrir = { navController.navigate(Rutas.tecnica(it)) })
        }
        composable(Pestana.Historial.ruta) { HistorialScreen(onRepetir = abrirFicha) }
        composable(Rutas.FICHA, arguments = listOf(ARG_SLUG)) { entrada ->
            val slug = entrada.arguments?.getString(Rutas.ARG_SLUG).orEmpty()
            FichaScreen(
                onAtras = { navController.popBackStack() },
                onTecnica = { navController.navigate(Rutas.tecnica(it)) },
                onCocinar = { raciones, servir -> navController.navigate(Rutas.crono(slug, raciones, servir)) },
            )
        }
        composable(Rutas.AJUSTES) {
            AjustesScreen(onAtras = { navController.popBackStack() }, onAbrir = { navController.navigate(it) })
        }
        composable(Rutas.AJUSTES_APARIENCIA) { AjustesAparienciaScreen(onAtras = { navController.popBackStack() }) }
        composable(Rutas.AJUSTES_HORARIO) { AjustesHorarioScreen(onAtras = { navController.popBackStack() }) }
        composable(Rutas.AJUSTES_CRONO) { AjustesCronoScreen(onAtras = { navController.popBackStack() }) }
        composable(Rutas.AJUSTES_HISTORIAL) { AjustesHistorialScreen(onAtras = { navController.popBackStack() }) }
        composable(Rutas.TECNICA, arguments = listOf(ARG_SLUG)) {
            TecnicaScreen(onAtras = { navController.popBackStack() }, onReceta = abrirFicha)
        }
        composable(
            Rutas.CRONO,
            arguments =
                listOf(
                    ARG_SLUG,
                    navArgument(Rutas.ARG_RACIONES) {
                        type = NavType.IntType
                        defaultValue = 0
                    },
                    navArgument(Rutas.ARG_SERVIR) {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            deepLinks = listOf(navDeepLink { uriPattern = "souschef://crono/{${Rutas.ARG_SLUG}}" }),
        ) { entrada ->
            val slug = entrada.arguments?.getString(Rutas.ARG_SLUG).orEmpty()
            CronoScreen(
                // Se vuelve a la ficha de la receta; si se llegó por la notificación y no está debajo, se abre.
                onSalir = {
                    if (navController.previousBackStackEntry?.destination?.route == Rutas.FICHA) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Rutas.ficha(slug)) { popUpTo(Rutas.CRONO) { inclusive = true } }
                    }
                },
                onTecnica = { navController.navigate(Rutas.tecnica(it)) },
            )
        }
    }
}
