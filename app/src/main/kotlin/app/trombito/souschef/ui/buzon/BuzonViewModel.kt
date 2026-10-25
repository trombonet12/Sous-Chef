package app.trombito.souschef.ui.buzon

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trombito.souschef.domain.Ajustes
import app.trombito.souschef.domain.AjustesRepo
import app.trombito.souschef.domain.HistorialRepo
import app.trombito.souschef.domain.PreferenciasRepo
import app.trombito.souschef.domain.Reloj
import app.trombito.souschef.domain.ResumenUso
import app.trombito.souschef.domain.VersionApp
import app.trombito.souschef.domain.resumenDeUso
import app.trombito.souschef.ui.Rutas
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/**
 * Estado del formulario del Buzón (vive mientras la pantalla está en la pila), los ajustes para el pie del correo y las
 * cifras del resumen de uso opcional. Si la ruta trae `plato` (desde la búsqueda vacía de Recetas), el formulario
 * arranca en «Pedir una receta» con ese plato escrito.
 */
@HiltViewModel
@Suppress("LongParameterList") // un repositorio por fuente del resumen de uso
class BuzonViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        ajustes: AjustesRepo,
        versionApp: VersionApp,
        historial: HistorialRepo,
        preferencias: PreferenciasRepo,
        private val reloj: Reloj,
    ) : ViewModel() {
        /** Versión instalada, para el pie del correo. */
        val version: String = versionApp.nombre

        private val _formulario =
            MutableStateFlow(
                FormularioBuzon(plato = savedStateHandle.get<String>(Rutas.ARG_PLATO).orEmpty().take(Rutas.PLATO_MAX)),
            )
        val formulario: StateFlow<FormularioBuzon> = _formulario.asStateFlow()

        val ajustes: StateFlow<Ajustes> =
            ajustes.observar().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Ajustes())

        /** Las cifras que se adjuntan si se marca la casilla; `null` hasta leer el historial. */
        val resumen: StateFlow<ResumenUso?> =
            combine(
                historial.observarTodo(),
                preferencias.observarQuiereHacerla(),
                ajustes.observar(),
            ) { entradas, quiere, a ->
                val hoy = Instant.ofEpochMilli(reloj.ahoraMs()).atZone(ZoneId.systemDefault()).toLocalDate()
                resumenDeUso(entradas, quiere.size, a, version, hoy)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        fun editar(cambio: (FormularioBuzon) -> FormularioBuzon) {
            _formulario.update(cambio)
        }
    }
