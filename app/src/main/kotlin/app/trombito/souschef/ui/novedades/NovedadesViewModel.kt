package app.trombito.souschef.ui.novedades

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trombito.souschef.di.ApplicationScope
import app.trombito.souschef.domain.ComprobarNovedades
import app.trombito.souschef.domain.EstadoActualizacion
import app.trombito.souschef.domain.FuenteNovedades
import app.trombito.souschef.domain.Novedades
import app.trombito.souschef.domain.SlugReceta
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val ESQUEMA_APP = "souschef"
private const val PREFIJO_RECETA = "receta:"

/** True si se arrancó por un enlace de la app (`souschef://crono/...`: notificación o alarma), no por el lanzador. */
fun esEntradaDirecta(intent: Intent?): Boolean = intent?.data?.scheme == ESQUEMA_APP

/** Slug de una línea con destino `receta:<slug>`; `null` si no es de receta o el slug no es válido. */
fun slugDeDestino(destino: String?): String? =
    destino
        ?.takeIf { it.startsWith(PREFIJO_RECETA) }
        ?.removePrefix(PREFIJO_RECETA)
        ?.takeIf { SlugReceta.valido(it) }

/**
 * Novedades tras actualizar. Se crea con el ámbito de la actividad: la comprobación se hace **una vez por proceso**, la
 * primera vez que se llega a Recetas; si fue por un enlace directo, no se enseña hasta el siguiente arranque.
 */
@HiltViewModel
class NovedadesViewModel
    @Inject
    constructor(
        private val comprobar: ComprobarNovedades,
        private val actualizacion: EstadoActualizacion,
        @ApplicationScope private val ambito: CoroutineScope,
    ) : ViewModel() {
        private val _pendientes = MutableStateFlow<List<Novedades>?>(null)

        /** Lo que hay que enseñar (la más nueva primero), o `null` si nada. */
        val pendientes: StateFlow<List<Novedades>?> = _pendientes

        private var comprobado = false

        fun alLlegarARecetas(entradaDirecta: Boolean) {
            if (comprobado) return
            comprobado = true
            viewModelScope.launch {
                // El flujo inmediato de Play tapa la app: se espera a saber si lo hay (con entrada directa, no).
                val play = !entradaDirecta && actualizacion.inmediataEnCurso.filterNotNull().first()
                _pendientes.value = comprobar(entradaDirecta, play)?.takeIf { it.isNotEmpty() }
            }
        }

        /**
         * La UI ya las pinta: se marcan como vistas ahora, no al cerrar, para no repetirlas si muere el proceso. En el
         * ámbito de la aplicación: si la pantalla se destruye al instante, la marca no se pierde.
         */
        fun alMostrarse() {
            ambito.launch { comprobar.marcarVistas() }
        }

        fun cerrar() {
            _pendientes.value = null
        }
    }

/** Todas las versiones (Ajustes → Acerca de → Novedades), de la más nueva a la más vieja. `null` hasta leerlas. */
@HiltViewModel
class TodasNovedadesViewModel
    @Inject
    constructor(
        fuente: FuenteNovedades,
    ) : ViewModel() {
        private val _todas = MutableStateFlow<List<Novedades>?>(null)
        val todas: StateFlow<List<Novedades>?> = _todas

        init {
            viewModelScope.launch { _todas.value = fuente.todas() }
        }
    }
