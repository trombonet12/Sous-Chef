package app.trombito.souschef.ui.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trombito.souschef.domain.Reloj
import app.trombito.souschef.domain.copia.AvisoCopiaAuto
import app.trombito.souschef.domain.copia.CopiaAutoRepo
import app.trombito.souschef.domain.copia.CopiaAutomatica
import app.trombito.souschef.domain.copia.EstadoCopiaAuto
import app.trombito.souschef.domain.copia.FalloCopiaAuto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * La copia automática de la carpeta (ADR-0015) para la interfaz: el estado de Ajustes → Historial, el aviso de que no
 * funciona (Historial y menú de Ajustes) y las dos acciones: elegir carpeta (también «otra») y desactivar.
 */
@HiltViewModel
class CopiaAutoViewModel
    @Inject
    constructor(
        private val copia: CopiaAutomatica,
        repo: CopiaAutoRepo,
        private val reloj: Reloj,
    ) : ViewModel() {
        val estado: StateFlow<EstadoCopiaAuto> =
            repo.observar().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoCopiaAuto())

        /** El fallo que hay que enseñar (según [AvisoCopiaAuto.debeAvisar]); null si no hay nada que avisar. */
        val aviso: StateFlow<FalloCopiaAuto?> =
            repo
                .observar()
                .map { e -> e.fallo.takeIf { AvisoCopiaAuto.debeAvisar(e, reloj.ahoraMs()) } }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        fun ahoraMs(): Long = reloj.ahoraMs()

        /** [uri]: el árbol que devolvió el selector del sistema. */
        fun elegirCarpeta(uri: String) {
            viewModelScope.launch { copia.elegirCarpeta(uri) }
        }

        fun desactivar() {
            viewModelScope.launch { copia.desactivar() }
        }
    }
