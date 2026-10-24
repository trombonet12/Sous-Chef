package app.trombito.souschef.ui.bienvenida

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trombito.souschef.di.ApplicationScope
import app.trombito.souschef.domain.AjustesRepo
import app.trombito.souschef.domain.RecetasRepo
import app.trombito.souschef.domain.VersionApp
import app.trombito.souschef.domain.voz.Escucha
import app.trombito.souschef.domain.voz.Habla
import app.trombito.souschef.receta.voz.InterpreteVoz
import app.trombito.souschef.receta.voz.OrdenVoz
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Cómo va la prueba de voz de la intro. */
sealed interface PruebaVoz {
    data object Inactiva : PruebaVoz

    /** El chef dice su frase. */
    data object Hablando : PruebaVoz

    data object Escuchando : PruebaVoz

    /** No hay reconocedor de voz en el móvil. */
    data object SinReconocedor : PruebaVoz

    data object SinPermiso : PruebaVoz

    /** Se acabó el tiempo sin oír nada. */
    data object NadaOido : PruebaVoz

    /** Lo reconocido ([frase]) y la orden que entendió el intérprete (`null`: no era una orden). */
    data class Oido(
        val frase: String,
        val orden: OrdenVoz?,
    ) : PruebaVoz
}

/**
 * Datos de ejemplo de la intro (recetas reales del repositorio) y la prueba de voz: el chef habla, se escucha una
 * sola vez y se enseña lo que entendió [InterpreteVoz]. No guarda audio ni transcripciones.
 */
@HiltViewModel
class BienvenidaViewModel
    @Inject
    constructor(
        private val recetas: RecetasRepo,
        private val escucha: Escucha,
        private val habla: Habla,
        private val ajustes: AjustesRepo,
        private val version: VersionApp,
        @ApplicationScope private val ambito: CoroutineScope,
    ) : ViewModel() {
        private val _demo = MutableStateFlow(DatosDemo())
        val demo: StateFlow<DatosDemo> = _demo

        private val _voz = MutableStateFlow<PruebaVoz>(PruebaVoz.Inactiva)
        val voz: StateFlow<PruebaVoz> = _voz

        private var espera: Job? = null

        init {
            viewModelScope.launch {
                _demo.value = DatosDemo(recetas.obtener(SLUG_COCINA), recetas.obtener(SLUG_RACIONES))
            }
        }

        /** Dice [frase] con la voz del chef y, al acabar, escucha una vez. Necesita el micrófono ya concedido. */
        fun probarVoz(frase: String) {
            detener()
            if (!escucha.disponible) {
                _voz.value = PruebaVoz.SinReconocedor
                return
            }
            _voz.value = PruebaVoz.Hablando
            habla.decir(frase) { if (_voz.value == PruebaVoz.Hablando) escuchar() }
        }

        /**
         * Marca la intro como vista en el ámbito de aplicación: no se pierde al desapilar la pantalla. También marca
         * la versión actual como vista: en una instalación nueva no hay «novedades» que enseñar.
         */
        fun marcarIntroVista() {
            ambito.launch { ajustes.terminarIntro(version.nombre) }
        }

        /** El usuario no dio el micrófono. */
        fun sinPermiso() {
            detener()
            _voz.value = PruebaVoz.SinPermiso
        }

        /** Corta la voz y el micrófono y deja la prueba como al principio. */
        fun pararVoz() {
            if (_voz.value == PruebaVoz.Inactiva) return
            detener()
            _voz.value = PruebaVoz.Inactiva
        }

        private fun detener() {
            espera?.cancel()
            // Antes de callar: el fin del habla no debe pasar a escuchar.
            _voz.value = PruebaVoz.Inactiva
            escucha.parar()
            habla.callar()
        }

        private fun escuchar() {
            _voz.value = PruebaVoz.Escuchando
            escucha.empezar(::alOir) { porPermiso ->
                if (_voz.value == PruebaVoz.Escuchando) {
                    _voz.value = if (porPermiso) PruebaVoz.SinPermiso else PruebaVoz.NadaOido
                }
            }
            espera =
                viewModelScope.launch {
                    delay(ESPERA_MS)
                    if (_voz.value == PruebaVoz.Escuchando) {
                        escucha.parar()
                        _voz.value = PruebaVoz.NadaOido
                    }
                }
        }

        private fun alOir(alternativas: List<String>) {
            if (_voz.value != PruebaVoz.Escuchando) return
            espera?.cancel()
            escucha.parar()
            val frase = alternativas.firstOrNull { it.isNotBlank() }
            _voz.value =
                if (frase == null) {
                    PruebaVoz.NadaOido
                } else {
                    PruebaVoz.Oido(frase, InterpreteVoz.interpretar(alternativas, _demo.value.carrilesVoz))
                }
        }

        override fun onCleared() {
            espera?.cancel()
            escucha.parar()
            habla.liberar()
        }

        private companion object {
            const val ESPERA_MS = 10_000L
        }
    }
