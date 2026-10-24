package app.trombito.souschef.ui.novedades

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Se compone dentro del destino Recetas. Al llegar la primera vez en el proceso pregunta si toca enseñar novedades
 * (`entradaDirecta` sale del intent con el que se abrió la actividad) y, si toca, pinta el diálogo. El ViewModel es el
 * de la actividad, no el de la entrada de navegación, para que cambiar de pestaña no repita la comprobación.
 */
@Composable
fun PuertaNovedades(onReceta: (String) -> Unit) {
    val actividad = checkNotNull(LocalActivity.current as? ComponentActivity) { "Sin actividad" }
    val viewModel: NovedadesViewModel = hiltViewModel(actividad)
    val directa = esEntradaDirecta(actividad.intent)
    LaunchedEffect(viewModel) { viewModel.alLlegarARecetas(directa) }
    val pendientes by viewModel.pendientes.collectAsStateWithLifecycle()
    pendientes?.let { lista ->
        DialogoNovedades(
            novedades = lista,
            onMostrada = viewModel::alMostrarse,
            onCerrar = viewModel::cerrar,
            onReceta = {
                viewModel.cerrar()
                onReceta(it)
            },
        )
    }
}
