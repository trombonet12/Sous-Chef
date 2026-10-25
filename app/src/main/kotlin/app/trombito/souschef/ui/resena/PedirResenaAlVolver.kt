package app.trombito.souschef.ui.resena

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trombito.souschef.domain.resena.GestorResena
import app.trombito.souschef.ui.Rutas
import app.trombito.souschef.ui.shell.Pestana
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Prefijo de las rutas del cronómetro (`crono/{slug}?...`). */
private const val RUTA_CRONO = "crono"

/** Es la salida del cronómetro hacia Recetas o la Ficha: el único momento en que se puede pedir la reseña (F14). */
internal fun salidaDelCrono(
    anterior: String?,
    actual: String?,
): Boolean = anterior?.startsWith(RUTA_CRONO) == true && (actual == Pestana.Recetas.ruta || actual == Rutas.FICHA)

@HiltViewModel
class ResenaViewModel
    @Inject
    constructor(
        private val gestor: GestorResena,
    ) : ViewModel() {
        fun alVolverDelCrono() {
            viewModelScope.launch { gestor.alVolverDelCrono() }
        }
    }

/**
 * Sin interfaz: al salir del cronómetro hacia Recetas o la Ficha (tras cerrar o con «Guardar y salir ya») avisa al
 * gestor, que decide si toca pedir la reseña. Nunca se dispara dentro del cronómetro.
 */
@Composable
fun PedirResenaAlVolver(
    ruta: String?,
    viewModel: ResenaViewModel = hiltViewModel(),
) {
    var anterior by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(ruta) {
        if (salidaDelCrono(anterior, ruta)) viewModel.alVolverDelCrono()
        anterior = ruta
    }
}
