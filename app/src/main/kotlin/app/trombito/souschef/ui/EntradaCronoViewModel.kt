package app.trombito.souschef.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import app.trombito.souschef.domain.RecetasRepo
import app.trombito.souschef.domain.SlugReceta
import app.trombito.souschef.ui.shell.Pestana
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Comprueba, antes de crear nada, que el slug que llega al cronómetro es válido y de una receta que existe. */
@HiltViewModel
class EntradaCronoViewModel
    @Inject
    constructor(
        private val recetas: RecetasRepo,
    ) : ViewModel() {
        suspend fun admite(slug: String): Boolean = SlugReceta.valido(slug) && recetas.obtener(slug) != null
    }

/**
 * Puerta de la ruta del cronómetro (el deep link puede traer cualquier slug). [contenido] solo se compone con un slug
 * válido de una receta existente, así que ni el ViewModel del cronómetro ni su sesión se crean con uno inválido; si no,
 * se abre Recetas.
 */
@Composable
fun EntradaCrono(
    slug: String,
    navController: NavHostController,
    contenido: @Composable () -> Unit,
) {
    val comprobar = hiltViewModel<EntradaCronoViewModel>()
    val admitido by produceState<Boolean?>(initialValue = null, slug) { value = comprobar.admite(slug) }
    if (admitido == true) contenido()
    if (admitido == false) {
        LaunchedEffect(Unit) {
            navController.navigate(Pestana.Recetas.ruta) {
                popUpTo(Rutas.CRONO) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
}
