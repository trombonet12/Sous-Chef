package app.trombito.souschef.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import app.trombito.souschef.R
import app.trombito.souschef.ui.ajustes.AjustesAcercaScreen
import app.trombito.souschef.ui.ajustes.AjustesAparienciaScreen
import app.trombito.souschef.ui.ajustes.AjustesCocinaScreen
import app.trombito.souschef.ui.ajustes.AjustesCronoScreen
import app.trombito.souschef.ui.ajustes.AjustesHistorialScreen
import app.trombito.souschef.ui.ajustes.AjustesHorarioScreen
import app.trombito.souschef.ui.ajustes.AjustesScreen
import app.trombito.souschef.ui.bienvenida.DestinoInicioViewModel
import app.trombito.souschef.ui.bienvenida.bienvenida
import app.trombito.souschef.ui.buzon.BuzonScreen
import app.trombito.souschef.ui.crono.CronoScreen
import app.trombito.souschef.ui.ficha.FichaScreen
import app.trombito.souschef.ui.historial.HistorialScreen
import app.trombito.souschef.ui.novedades.PuertaNovedades
import app.trombito.souschef.ui.novedades.TodasNovedadesScreen
import app.trombito.souschef.ui.recetas.RecetasScreen
import app.trombito.souschef.ui.shell.Pestana
import app.trombito.souschef.ui.tecnicas.TecnicaScreen
import app.trombito.souschef.ui.tecnicas.TecnicasScreen

private val ARG_SLUG = navArgument(Rutas.ARG_SLUG) { type = NavType.StringType }
private val ARG_FAMILIA = navArgument(Rutas.ARG_FAMILIA) { type = NavType.StringType }
private val ARG_CATEGORIA = navArgument(Rutas.ARG_CATEGORIA) { type = NavType.StringType }

/** Grafo de destinos. */
@Composable
fun SousChefNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val abrirFicha: (String) -> Unit = { navController.navigate(Rutas.ficha(it)) }
    val abrirTecnica: (String) -> Unit = { navController.navigate(Rutas.tecnica(it)) }
    // El destino de inicio se decide una sola vez, con los ajustes ya cargados (intro si aún no se vio).
    val inicio by hiltViewModel<DestinoInicioViewModel>().destino.collectAsStateWithLifecycle()
    val destinoInicio = inicio ?: return
    NavHost(navController, startDestination = destinoInicio, modifier = modifier) {
        listas(navController, abrirFicha, abrirTecnica)
        composable(Rutas.FICHA, arguments = listOf(ARG_SLUG)) { entrada ->
            val slug = entrada.arguments?.getString(Rutas.ARG_SLUG).orEmpty()
            FichaScreen(
                onAtras = { navController.popBackStack() },
                onTecnica = { navController.navigate(Rutas.tecnica(it)) },
                onCocinar = { raciones, servir -> navController.navigate(Rutas.crono(slug, raciones, servir)) },
            )
        }
        ajustes(navController)
        bienvenida(navController)
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
            EntradaCrono(slug, navController) {
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
}

/** El menú de Ajustes y sus pantallas. */
private fun NavGraphBuilder.ajustes(navController: NavHostController) {
    val atras: () -> Unit = { navController.popBackStack() }
    composable(Rutas.AJUSTES) {
        AjustesScreen(
            onAtras = atras,
            onAbrir = { navController.navigate(it) },
            onBuzon = { navController.navigate(Rutas.BUZON) },
        )
    }
    composable(Rutas.BUZON) { BuzonScreen(onAtras = atras) }
    composable(Rutas.AJUSTES_COCINA) { AjustesCocinaScreen(onAtras = atras) }
    composable(Rutas.AJUSTES_APARIENCIA) { AjustesAparienciaScreen(onAtras = atras) }
    composable(Rutas.AJUSTES_HORARIO) { AjustesHorarioScreen(onAtras = atras) }
    composable(Rutas.AJUSTES_CRONO) { AjustesCronoScreen(onAtras = atras) }
    composable(Rutas.AJUSTES_HISTORIAL) { AjustesHistorialScreen(onAtras = atras) }
    composable(Rutas.AJUSTES_ACERCA) {
        AjustesAcercaScreen(
            onAtras = atras,
            onIntro = { navController.navigate(Rutas.BIENVENIDA_REPETIR) },
            onNovedades = { navController.navigate(Rutas.AJUSTES_NOVEDADES) },
        )
    }
    composable(Rutas.AJUSTES_NOVEDADES) {
        TodasNovedadesScreen(onAtras = atras, onReceta = { navController.navigate(Rutas.ficha(it)) })
    }
}

/** Las tres pestañas de lista y las listas de una familia o categoría. */
private fun NavGraphBuilder.listas(
    navController: NavHostController,
    abrirFicha: (String) -> Unit,
    abrirTecnica: (String) -> Unit,
) {
    composable(Pestana.Recetas.ruta) {
        RecetasScreen(onAbrir = abrirFicha, onCategoria = { navController.navigate(Rutas.recetasFamilia(it)) })
        // Las novedades tras actualizar solo salen aquí, en el destino Recetas.
        PuertaNovedades(onReceta = abrirFicha)
    }
    composable(Rutas.RECETAS_FAMILIA, arguments = listOf(ARG_FAMILIA)) {
        RecetasScreen(onAbrir = abrirFicha, onAtras = { navController.popBackStack() })
    }
    composable(Pestana.Tecnicas.ruta) {
        TecnicasScreen(
            onAtras = null,
            onAbrir = abrirTecnica,
            onCategoria = { navController.navigate(Rutas.tecnicasCategoria(it)) },
        )
    }
    composable(Rutas.TECNICAS_CATEGORIA, arguments = listOf(ARG_CATEGORIA)) {
        TecnicasScreen(onAtras = { navController.popBackStack() }, onAbrir = abrirTecnica)
    }
    composable(Pestana.Historial.ruta) { HistorialScreen(onRepetir = abrirFicha) }
}
