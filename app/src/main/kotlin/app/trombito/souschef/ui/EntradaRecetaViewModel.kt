package app.trombito.souschef.ui

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import app.trombito.souschef.domain.RecetasRepo
import app.trombito.souschef.domain.SlugReceta
import app.trombito.souschef.ui.shell.Pestana
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Comprueba, antes de crear nada, que el slug que llega a una ruta de receta es válido y de una receta que existe. */
@HiltViewModel
class EntradaRecetaViewModel
    @Inject
    constructor(
        private val recetas: RecetasRepo,
    ) : ViewModel() {
        suspend fun admite(slug: String): Boolean = SlugReceta.valido(slug) && recetas.obtener(slug) != null
    }

/**
 * True si el destino se abrió desde fuera (deep link o intent): Navigation deja el intent en los argumentos de la
 * entrada. La navegación interna no lo lleva, así que no pasa por la puerta.
 */
fun llegadaPorEnlace(argumentos: Bundle?): Boolean = argumentos?.containsKey(NavController.KEY_DEEP_LINK_INTENT) == true

/**
 * Puerta de las rutas con el slug de una receta (el deep link puede traer cualquier slug). Con [aplicar], [contenido]
 * solo se compone con un slug válido de una receta existente, así que ni el ViewModel del destino ni su sesión se
 * crean con uno inválido; si no, se abre Recetas. Sin [aplicar] (navegación interna) deja pasar sin esperar.
 */
@Composable
fun EntradaReceta(
    slug: String,
    navController: NavHostController,
    rutaDestino: String,
    aplicar: Boolean = true,
    contenido: @Composable () -> Unit,
) {
    if (!aplicar) {
        contenido()
        return
    }
    val comprobar = hiltViewModel<EntradaRecetaViewModel>()
    val admitido by produceState<Boolean?>(initialValue = null, slug) { value = comprobar.admite(slug) }
    if (admitido == true) contenido()
    if (admitido == false) {
        LaunchedEffect(Unit) {
            navController.navigate(Pestana.Recetas.ruta) {
                popUpTo(rutaDestino) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
}

/** Puerta del cronómetro: siempre se aplica, venga de donde venga. */
@Composable
fun EntradaCrono(
    slug: String,
    navController: NavHostController,
    contenido: @Composable () -> Unit,
) = EntradaReceta(slug, navController, Rutas.CRONO, contenido = contenido)
