package app.souschef.ui.recetas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.souschef.domain.AjustesRepo
import app.souschef.domain.HistorialRepo
import app.souschef.domain.OrdenRecetas
import app.souschef.domain.PreferenciasRepo
import app.souschef.domain.RecetasRepo
import app.souschef.domain.Reloj
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import javax.inject.Inject

/**
 * Lista de recetas. El texto y los filtros viven aquí y se pierden al cerrar la app; el orden y el diseño de las
 * tarjetas se guardan en Ajustes.
 */
@HiltViewModel
class RecetasViewModel
    @Inject
    constructor(
        recetas: RecetasRepo,
        private val preferencias: PreferenciasRepo,
        historial: HistorialRepo,
        private val ajustes: AjustesRepo,
        private val reloj: Reloj,
    ) : ViewModel() {
        private val filtro = MutableStateFlow(FiltroRecetas())

        val estado: StateFlow<RecetasUiState> =
            combine(
                recetas.observarTodas(),
                preferencias.observarTodas(),
                historial.observarUltimaFechaPorSlug(),
                filtro,
                ajustes.observar(),
            ) { r, p, h, f, a ->
                construirEstadoRecetas(r, p, h, f.copy(orden = a.orden), hoy()).copy(disposicion = a.lista)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecetasUiState())

        fun buscar(texto: String) {
            filtro.update { it.copy(consulta = texto) }
        }

        fun alternarFiltro(opcion: OpcionFiltro) {
            filtro.update { f -> f.copy(activos = if (opcion in f.activos) f.activos - opcion else f.activos + opcion) }
        }

        fun limpiarFiltros() {
            filtro.update { it.copy(activos = emptySet()) }
        }

        fun elegirOrden(orden: OrdenRecetas) {
            viewModelScope.launch { ajustes.fijarOrden(orden) }
        }

        fun alternarFavorita(
            slug: String,
            favorita: Boolean,
        ) {
            viewModelScope.launch { preferencias.fijarFavorito(slug, favorita) }
        }

        private fun hoy() = Instant.ofEpochMilli(reloj.ahoraMs()).atZone(ZoneOffset.UTC).toLocalDate()
    }
