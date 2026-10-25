package app.trombito.souschef.ui.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trombito.souschef.domain.Ajustes
import app.trombito.souschef.domain.AjustesRepo
import app.trombito.souschef.domain.DisposicionCrono
import app.trombito.souschef.domain.DisposicionLista
import app.trombito.souschef.domain.HoraTarjeta
import app.trombito.souschef.domain.TemaApp
import app.trombito.souschef.domain.VersionApp
import app.trombito.souschef.domain.copia.GestorCopia
import app.trombito.souschef.domain.copia.ResultadoCopia
import app.trombito.souschef.receta.Cocina
import app.trombito.souschef.ui.ficha.Servir
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("TooManyFunctions") // una intención por ajuste de la pantalla
@HiltViewModel
class AjustesViewModel
    @Inject
    constructor(
        private val ajustes: AjustesRepo,
        private val copias: GestorCopia,
        versionApp: VersionApp,
    ) : ViewModel() {
        val estado: StateFlow<Ajustes> =
            ajustes.observar().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Ajustes())

        /** Versión instalada, para el menú de Ajustes y Acerca de. */
        val version: String = versionApp.nombre

        private val _copia = MutableStateFlow<ResultadoCopia?>(null)

        /** Resultado de la última copia guardada o cargada (para el mensaje bajo los botones). */
        val copia: StateFlow<ResultadoCopia?> = _copia

        /** [destino]: URI elegida con el selector del sistema. */
        fun guardarCopia(destino: String) {
            viewModelScope.launch { _copia.value = copias.exportar(destino) }
        }

        fun cargarCopia(origen: String) {
            viewModelScope.launch { _copia.value = copias.importar(origen) }
        }

        fun elegirDisposicion(d: DisposicionCrono) {
            viewModelScope.launch { ajustes.fijarDisposicion(d) }
        }

        fun activarVoz(activa: Boolean) {
            viewModelScope.launch { ajustes.fijarVoz(activa) }
        }

        fun elegirLista(d: DisposicionLista) {
            viewModelScope.launch { ajustes.fijarLista(d) }
        }

        fun elegirTema(t: TemaApp) {
            viewModelScope.launch { ajustes.fijarTema(t) }
        }

        /** Mueve la hora de servir por defecto [delta] minutos (con vuelta a medianoche). */
        fun ajustarServir(delta: Int) {
            viewModelScope.launch { ajustes.fijarServir(Servir.ajustar(estado.value.servirMin, delta)) }
        }

        fun elegirHoraTarjeta(h: HoraTarjeta) {
            viewModelScope.launch { ajustes.fijarHoraTarjeta(h) }
        }

        fun elegirCocina(c: Cocina) {
            viewModelScope.launch { ajustes.fijarCocina(c) }
        }

        fun ajustarFuegos(n: Int) {
            viewModelScope.launch { ajustes.fijarFuegos(n) }
        }

        fun activarHorno(tiene: Boolean) {
            viewModelScope.launch { ajustes.fijarHorno(tiene) }
        }

        fun ajustarRacionesDefecto(n: Int) {
            viewModelScope.launch { ajustes.fijarRacionesDefecto(n) }
        }

        fun marcarIntroVista(vista: Boolean) {
            viewModelScope.launch { ajustes.marcarIntroVista(vista) }
        }

        fun marcarGuiaCronoVista(vista: Boolean) {
            viewModelScope.launch { ajustes.marcarGuiaCronoVista(vista) }
        }
    }
